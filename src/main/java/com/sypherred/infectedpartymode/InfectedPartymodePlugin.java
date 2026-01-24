package com.sypherred.infectedpartymode;

import com.google.inject.Provides;

import javax.inject.Inject;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;

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
import com.sypherred.infectedpartymode.model.GameSession;
import com.sypherred.infectedpartymode.model.InfectionMode;
import com.sypherred.infectedpartymode.model.InfectionState;
import com.sypherred.infectedpartymode.model.PlayerState;
import com.sypherred.infectedpartymode.overlay.ArenaRegionShadeOverlay;
import com.sypherred.infectedpartymode.overlay.GameInfoOverlay;
import com.sypherred.infectedpartymode.overlay.OutOfBoundsOverlay;
import com.sypherred.infectedpartymode.overlay.worldmap.RegionDebugWorldMapOverlay;
import com.sypherred.infectedpartymode.party.GameStartedFromParty;
import com.sypherred.infectedpartymode.party.HostAuthorityManager;
import com.sypherred.infectedpartymode.party.PartySyncManager;
import com.sypherred.infectedpartymode.rules.InfectionManager;
import com.sypherred.infectedpartymode.rules.OutOfBoundsManager;
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
		eventBus.register(this);
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
		eventBus.unregister(this);
		eventBus.unregister(outOfBoundsManager);
		forceStop();
	}

	/* =========================
	   Party Start Event
	   ========================= */

	@Subscribe
	public void onGameStartedFromParty(GameStartedFromParty e)
	{
		if (gameState == GameState.RUNNING)
		{
			return;
		}

		GameSession session = partySyncManager.getGameSession();
		gameState = GameState.RUNNING;

		int remaining = (int) (session.getRemainingMillis() / 1000L);
		gameTimer.start(Math.max(remaining, 0));

		infectedPanel.refreshControls();
		pluginMessage("Game started (party sync).");
	}

	/* =========================
	   Game Control
	   ========================= */

	public void startGame(int ignoredDurationSeconds)
	{
		if (!hostAuthorityManager.isHost())
		{
			pluginMessage("Only the host can start the game.");
			return;
		}

		Set<Integer> preview = areaManager.getPreviewRegions();
		if (preview.isEmpty())
		{
			buildPreviewFromConfig();
			preview = areaManager.getPreviewRegions();
		}

		areaManager.setActiveRegions(preview);
		areaManager.clearPreview();

		int durationSeconds = config.gameDurationMinutes() * 60;

		if (partyService.isInParty())
		{
			partySyncManager.sendArea();
			partySyncManager.initializePlayersFromParty();
			partySyncManager.sendGameStart(durationSeconds);
		}
		else
		{
			gameState = GameState.RUNNING;
			gameTimer.start(durationSeconds);
		}
	}

	public void stopGame()
	{
		if (!hostAuthorityManager.isHost())
		{
			pluginMessage("Only the host can stop the game.");
			return;
		}

		forceStop();
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
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (gameState == GameState.RUNNING)
		{
			infectedPanel.refreshControls();
		}
	}

	/* =========================
	   UI Accessors (REQUIRED)
	   ========================= */

	public boolean isGameRunning()
	{
		return gameState == GameState.RUNNING;
	}

	public ArenaMode getArenaMode()
	{
		return config.arenaMode();
	}

	public int getRemainingSeconds()
	{
		return gameTimer != null ? gameTimer.getRemainingSeconds() : 0;
	}

	public boolean isHost()
	{
		return hostAuthorityManager.isHost();
	}

	public int getActiveRegionCount()
	{
		return areaManager.getAllowedRegions().size();
	}

	/* =========================
	   Preview / Helpers
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
