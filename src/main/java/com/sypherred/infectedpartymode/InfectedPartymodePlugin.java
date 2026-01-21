package com.sypherred.infectedpartymode;

import com.google.inject.Provides;

import javax.inject.Inject;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Player;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.party.PartyService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

import com.sypherred.infectedpartymode.area.AreaManager;
import com.sypherred.infectedpartymode.area.ArenaMode;
import com.sypherred.infectedpartymode.area.AreaRandomUtil;
import com.sypherred.infectedpartymode.area.ManualRegionParser;
import com.sypherred.infectedpartymode.game.GameState;
import com.sypherred.infectedpartymode.game.GameTimer;
import com.sypherred.infectedpartymode.model.InfectionMode;
import com.sypherred.infectedpartymode.model.InfectionState;
import com.sypherred.infectedpartymode.model.PlayerState;
import com.sypherred.infectedpartymode.overlay.ArenaRegionShadeOverlay;
import com.sypherred.infectedpartymode.overlay.GameInfoOverlay;
import com.sypherred.infectedpartymode.overlay.worldmap.RegionDebugWorldMapOverlay;
import com.sypherred.infectedpartymode.overlay.OutOfBoundsOverlay;
import com.sypherred.infectedpartymode.party.PartySyncManager;
import com.sypherred.infectedpartymode.party.HostAuthorityManager;
import com.sypherred.infectedpartymode.rules.OutOfBoundsManager;
import com.sypherred.infectedpartymode.rules.InfectionManager;
import com.sypherred.infectedpartymode.ui.InfectedPanel;

@PluginDescriptor(
		name = "Infected Partymode",
		description = "Party-based Infected / ManHunt game mode",
		tags = {"infected", "party", "manhunt", "minigame"},
		enabledByDefault = false
)
public class InfectedPartymodePlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(InfectedPartymodePlugin.class);

	@Inject private Client client;
	@Inject private EventBus eventBus;
	@Inject private OverlayManager overlayManager;
	@Inject private ClientToolbar clientToolbar;
	@Inject private ScheduledExecutorService executor;
	@Inject private PartyService partyService;
	@Inject private PartySyncManager partySyncManager;
	@Inject private HostAuthorityManager hostAuthorityManager;
	@Inject private InfectedPartymodeConfig config;

	@Inject private AreaManager areaManager;
	@Inject private OutOfBoundsManager outOfBoundsManager;
	@SuppressWarnings("unused")
	@Inject private InfectionManager infectionManager;

	@Inject private InfectedPanel infectedPanel;

	@Inject private ArenaRegionShadeOverlay arenaRegionShadeOverlay;
	@Inject private GameInfoOverlay gameInfoOverlay;
	@Inject private RegionDebugWorldMapOverlay regionDebugWorldMapOverlay;
	@Inject private OutOfBoundsOverlay outOfBoundsOverlay;

	private GameState gameState = GameState.IDLE;
	private GameTimer gameTimer;
	private NavigationButton navButton;

	@Provides
	InfectedPartymodeConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(InfectedPartymodeConfig.class);
	}

	@Override
	protected void startUp()
	{
		eventBus.register(outOfBoundsManager);
		gameTimer = new GameTimer(executor);

		overlayManager.add(arenaRegionShadeOverlay);
		overlayManager.add(gameInfoOverlay);
		overlayManager.add(regionDebugWorldMapOverlay);
		overlayManager.add(outOfBoundsOverlay);

		buildPreviewFromConfig();

		BufferedImage icon = null;
		try
		{
			icon = ImageIO.read(getClass().getResourceAsStream("/infected_icon.png"));
		}
		catch (IOException | IllegalArgumentException e)
		{
			log.warn("Could not load plugin icon", e);
		}

		navButton = NavigationButton.builder()
				.tooltip("Infected Partymode")
				.icon(icon)
				.panel(infectedPanel)
				.build();

		clientToolbar.addNavigation(navButton);
		infectedPanel.refreshControls();
	}

	@Override
	protected void shutDown()
	{
		eventBus.unregister(outOfBoundsManager);

		forceStop();

		overlayManager.remove(arenaRegionShadeOverlay);
		overlayManager.remove(gameInfoOverlay);
		overlayManager.remove(regionDebugWorldMapOverlay);
		overlayManager.remove(outOfBoundsOverlay);

		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
			navButton = null;
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if (!"infectedpartymode".equals(e.getGroup()))
		{
			return;
		}

		if (gameState == GameState.IDLE)
		{
			buildPreviewFromConfig();
		}

		infectedPanel.refreshControls();
	}

    /* =========================
       Game Control
       ========================= */

	// Keeping signature for compatibility with your current UI button call.
	// Duration is now taken from config.
	public void startGame(int ignoredDurationSeconds)
	{
		if (gameState == GameState.RUNNING)
		{
			return;
		}

		Player local = client.getLocalPlayer();
		if (local == null)
		{
			pluginMessage("Local player not ready yet.");
			return;
		}

		if (config.arenaMode() == ArenaMode.NONE)
		{
			pluginMessage("Please select an arena mode first.");
			return;
		}

		if (!hostAuthorityManager.isHost())
		{
			pluginMessage("Only the host can start the game.");
			return;
		}

		// Claim host once (party-safe)
		if (hostAuthorityManager.getHostMemberId() == null)
		{
			hostAuthorityManager.claimHost();
			if (partyService.isInParty() && partyService.getLocalMember() != null)
			{
				partySyncManager.sendHostClaim(partyService.getLocalMember().getMemberId());
			}
		}

		// Commit preview -> active arena
		areaManager.clearArea();
		Set<Integer> preview = areaManager.getPreviewRegions();

		if (preview.isEmpty())
		{
			buildPreviewFromConfig();
			preview = areaManager.getPreviewRegions();
		}

		if (preview.isEmpty())
		{
			pluginMessage("No preview arena available. Check your settings.");
			return;
		}

		areaManager.setActiveRegions(preview);
		areaManager.clearPreview();

		int durationSeconds = config.gameDurationMinutes() * 60;
		gameState = GameState.RUNNING;
		gameTimer.start(durationSeconds);

		if (partyService.isInParty())
		{
			partySyncManager.sendArea();
		}

		// Baseline: ensure everyone we can currently see exists as HEALTHY (unless already tracked)
		for (Player p : client.getPlayers())
		{
			if (p != null && p.getName() != null)
			{
				partySyncManager.ensurePlayerHealthy(p.getName());
			}
		}

		// =========================
		// Initial Infection
		// =========================
		if (config.infectionMode() == InfectionMode.RANDOM)
		{
			List<PlayerState> candidates = new ArrayList<>(partySyncManager.getPlayerStates().values());

			if (!candidates.isEmpty())
			{
				Collections.shuffle(candidates);

				int count = Math.min(config.initialInfectedCount(), candidates.size());
				for (int i = 0; i < count; i++)
				{
					PlayerState ps = candidates.get(i);
					partySyncManager.sendInfectionState(ps.getPlayerName(), InfectionState.INFECTED);
				}

				pluginMessage(count + " player(s) have been infected.");
			}
			else
			{
				pluginMessage("No players available to infect yet.");
			}
		}
		else
		{
			pluginMessage("Manual infection mode active. No players infected yet.");
		}

		infectedPanel.refreshControls();
		pluginMessage("Game started.");
	}

	public void stopGame()
	{
		if (gameState == GameState.IDLE)
		{
			return;
		}

		if (!hostAuthorityManager.isHost())
		{
			pluginMessage("Only the host can stop the game.");
			return;
		}

		gameState = GameState.IDLE;
		gameTimer.stop();

		areaManager.clearArea();

		buildPreviewFromConfig();

		infectedPanel.refreshControls();
		pluginMessage("Game stopped.");
	}

	public void rerollRandomArena()
	{
		if (gameState != GameState.IDLE)
		{
			pluginMessage("Arena can only be rerolled before the game starts.");
			return;
		}

		if (!hostAuthorityManager.isHost())
		{
			pluginMessage("Only the host can reroll the arena.");
			return;
		}

		if (config.arenaMode() != ArenaMode.RANDOM)
		{
			pluginMessage("Reroll is only available in Random mode.");
			return;
		}

		generateRandomPreview();
		infectedPanel.refreshControls();
		pluginMessage("Arena rerolled.");
	}

    /* =========================
       Preview Logic (pre-game)
       ========================= */

	private void buildPreviewFromConfig()
	{
		areaManager.clearArea();
		areaManager.clearPreview();

		Player local = client.getLocalPlayer();
		if (local == null)
		{
			return;
		}

		switch (config.arenaMode())
		{
			case PRESET:
				if (config.presetArena().isValid())
				{
					areaManager.setPreviewRegions(config.presetArena().getRegions());
				}
				break;

			case RANDOM:
				generateRandomPreview();
				break;

			case MANUAL:
				Set<Integer> manual = ManualRegionParser.parse(config.manualRegions());
				if (!manual.isEmpty())
				{
					areaManager.setPreviewRegions(manual);
				}
				break;

			case CURRENT_PLUS_N:
				int start = local.getWorldLocation().getRegionID();
				areaManager.setPreviewRegions(buildConnectedRegionCluster(start, config.regionCount()));
				break;

			case NONE:
			default:
				break;
		}
	}

	private void generateRandomPreview()
	{
		Player local = client.getLocalPlayer();
		if (local == null)
		{
			return;
		}

		int start = AreaRandomUtil.randomRegionAnywhere(client);
		areaManager.setPreviewRegions(buildConnectedRegionCluster(start, config.regionCount()));
	}

	private static Set<Integer> buildConnectedRegionCluster(int startRegionId, int regionCount)
	{
		if (regionCount < 1)
		{
			regionCount = 1;
		}

		Set<Integer> regions = new HashSet<>();
		regions.add(startRegionId);

		Queue<Integer> frontier = new LinkedList<>();
		frontier.add(startRegionId);

		while (!frontier.isEmpty() && regions.size() < regionCount)
		{
			int regionId = frontier.poll();

			int rx = regionId >> 8;
			int ry = regionId & 0xFF;

			int[][] neighbors = {
					{rx + 1, ry},
					{rx - 1, ry},
					{rx, ry + 1},
					{rx, ry - 1}
			};

			for (int[] n : neighbors)
			{
				int neighborId = (n[0] << 8) | n[1];

				if (regions.add(neighborId))
				{
					frontier.add(neighborId);
					if (regions.size() >= regionCount)
					{
						break;
					}
				}
			}
		}

		return regions;
	}

    /* =========================
       Tick
       ========================= */

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (gameState == GameState.RUNNING)
		{
			infectedPanel.refreshControls();

			if (gameTimer.getRemainingSeconds() <= 0)
			{
				stopGame();
			}
		}
	}

    /* =========================
       Accessors
       ========================= */

	public boolean isGameRunning()
	{
		return gameState == GameState.RUNNING;
	}

	public ArenaMode getArenaMode()
	{
		return config.arenaMode();
	}

	public int getActiveRegionCount()
	{
		return areaManager.getAllowedRegions().size();
	}

	public int getRemainingSeconds()
	{
		return gameTimer != null ? gameTimer.getRemainingSeconds() : 0;
	}

	public boolean isHost()
	{
		return hostAuthorityManager.isHost();
	}

    /* =========================
       Chat
       ========================= */

	private void pluginMessage(String msg)
	{
		client.addChatMessage(
				ChatMessageType.GAMEMESSAGE,
				"",
				"[Infected] " + msg,
				null
		);
	}

	private void forceStop()
	{
		gameState = GameState.IDLE;

		if (gameTimer != null)
		{
			gameTimer.stop();
		}

		areaManager.clearArea();
		areaManager.clearPreview();
	}
}
