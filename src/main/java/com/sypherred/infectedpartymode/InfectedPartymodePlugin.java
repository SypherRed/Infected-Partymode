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

    /* =========================
       Game Control
       ========================= */

	public void startGame()
	{
		if (gameState == GameState.RUNNING)
		{
			return;
		}

		if (!hostAuthorityManager.isHost())
		{
			pluginMessage("Only the host can start the game.");
			return;
		}

		if (config.arenaMode() == ArenaMode.NONE)
		{
			pluginMessage("Please select an arena mode first.");
			return;
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
			pluginMessage("No preview arena available.");
			return;
		}

		areaManager.setActiveRegions(preview);
		areaManager.clearPreview();

		int durationSeconds = config.gameDurationMinutes() * 60;
		gameState = GameState.RUNNING;
		gameTimer.start(durationSeconds);

		// =========================
		// Initial Infection (N random players)
		// =========================
		List<PlayerState> candidates =
				new ArrayList<>(partySyncManager.getPlayerStates().values());

		if (!candidates.isEmpty())
		{
			Collections.shuffle(candidates);

			int count = Math.min(
					config.initialInfectedCount(),
					candidates.size()
			);

			for (int i = 0; i < count; i++)
			{
				PlayerState ps = candidates.get(i);
				partySyncManager.sendInfectionState(
						ps.getPlayerName(),
						InfectionState.INFECTED
				);
			}

			pluginMessage(count + " player(s) have been infected.");
		}

		if (partyService.isInParty())
		{
			partySyncManager.sendArea();
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
       Helpers
       ========================= */

	private void buildPreviewFromConfig()
	{
		areaManager.clearArea();
		areaManager.clearPreview();

		switch (config.arenaMode())
		{
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
				Player local = client.getLocalPlayer();
				if (local != null)
				{
					areaManager.setPreviewRegions(
							buildConnectedRegionCluster(
									local.getWorldLocation().getRegionID(),
									config.regionCount()
							)
					);
				}
				break;
			case PRESET:
				if (config.presetArena().isValid())
				{
					areaManager.setPreviewRegions(config.presetArena().getRegions());
				}
				break;
			case NONE:
			default:
				break;
		}
	}

	private void generateRandomPreview()
	{
		int start = AreaRandomUtil.randomRegionAnywhere(client);
		areaManager.setPreviewRegions(
				buildConnectedRegionCluster(start, config.regionCount())
		);
	}

	private static Set<Integer> buildConnectedRegionCluster(int startRegionId, int regionCount)
	{
		Set<Integer> regions = new HashSet<>();
		Queue<Integer> frontier = new LinkedList<>();

		regions.add(startRegionId);
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
