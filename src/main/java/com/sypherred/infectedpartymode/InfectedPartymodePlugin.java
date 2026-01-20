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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Player;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
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
import com.sypherred.infectedpartymode.overlay.ArenaRegionShadeOverlay;
import com.sypherred.infectedpartymode.overlay.GameInfoOverlay;
import com.sypherred.infectedpartymode.overlay.worldmap.RegionDebugWorldMapOverlay;
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
	private static final Logger log =
			LoggerFactory.getLogger(InfectedPartymodePlugin.class);

    /* =========================
       Injected core services
       ========================= */

	@Inject
	private Client client;

	@Inject
	private EventBus eventBus;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private PartyService partyService;

	@Inject
	private PartySyncManager partySyncManager;

	@Inject
	private HostAuthorityManager hostAuthorityManager;

	@Inject
	private InfectedPartymodeConfig config;

    /* =========================
       Game logic
       ========================= */

	@Inject
	private AreaManager areaManager;

	@Inject
	private OutOfBoundsManager outOfBoundsManager;

	@SuppressWarnings("unused")
	@Inject
	private InfectionManager infectionManager;

    /* =========================
       UI
       ========================= */

	@Inject
	private InfectedPanel infectedPanel;

    /* =========================
       Overlays
       ========================= */

	@Inject
	private ArenaRegionShadeOverlay arenaRegionShadeOverlay;

	@Inject
	private GameInfoOverlay gameInfoOverlay;

	@Inject
	private RegionDebugWorldMapOverlay regionDebugWorldMapOverlay;

    /* =========================
       State
       ========================= */

	private GameState gameState = GameState.IDLE;
	private GameTimer gameTimer;
	private NavigationButton navButton;

    /* =========================
       Config
       ========================= */

	@Provides
	InfectedPartymodeConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(InfectedPartymodeConfig.class);
	}

    /* =========================
       Lifecycle
       ========================= */

	@Override
	protected void startUp()
	{
		log.info("Infected Partymode starting");

		eventBus.register(outOfBoundsManager);

		gameTimer = new GameTimer(executor);

		overlayManager.add(arenaRegionShadeOverlay);
		overlayManager.add(gameInfoOverlay);
		overlayManager.add(regionDebugWorldMapOverlay);

		// Build initial preview (pre-game)
		updateArenaPreview();

		BufferedImage icon = null;
		try
		{
			icon = ImageIO.read(
					getClass().getResourceAsStream("/infected_icon.png")
			);
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
	}

	@Override
	protected void shutDown()
	{
		log.info("Infected Partymode shutting down");

		eventBus.unregister(outOfBoundsManager);

		stopGame();

		overlayManager.remove(arenaRegionShadeOverlay);
		overlayManager.remove(gameInfoOverlay);
		overlayManager.remove(regionDebugWorldMapOverlay);

		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
			navButton = null;
		}
	}

    /* =========================
       Game control (HOST-ONLY)
       ========================= */

	public void startGame(int durationSeconds)
	{
		if (gameState == GameState.RUNNING)
		{
			return;
		}

		if (client.getLocalPlayer() == null)
		{
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

		if (hostAuthorityManager.getHostMemberId() == null)
		{
			hostAuthorityManager.claimHost();
			if (partyService.isInParty() && partyService.getLocalMember() != null)
			{
				partySyncManager.sendHostClaim(
						partyService.getLocalMember().getMemberId()
				);
			}
		}

		// Commit preview -> active (or generate active directly if preview empty)
		areaManager.clearArea();

		Set<Integer> preview = areaManager.getPreviewRegions();
		if (!preview.isEmpty())
		{
			areaManager.setActiveRegions(preview);
		}
		else
		{
			// Fallback: generate active directly (should rarely happen)
			generateActiveArenaFromConfig();
		}

		gameState = GameState.RUNNING;
		gameTimer.start(durationSeconds);

		// Clear preview once the game starts (avoids mixing states)
		areaManager.clearPreview();

		if (partyService.isInParty())
		{
			partySyncManager.sendArea();
		}

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

		log.info("Stopping game");

		gameState = GameState.IDLE;

		if (gameTimer != null)
		{
			gameTimer.stop();
		}

		areaManager.clearArea();
		hostAuthorityManager.reset();

		// Restore preview after game end
		updateArenaPreview();

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

		generateRandomPreviewArena();
		pluginMessage("Random arena preview rerolled.");
	}

    /* =========================
       Preview (pre-game)
       ========================= */

	public void updateArenaPreview()
	{
		if (isGameRunning())
		{
			return;
		}

		areaManager.clearPreview();

		switch (config.arenaMode())
		{
			case PRESET:
				if (config.presetArena().isValid())
				{
					areaManager.setPreviewRegions(
							config.presetArena().getRegions()
					);
				}
				break;

			case RANDOM:
				generateRandomPreviewArena();
				break;

			case MANUAL:
				var manual = ManualRegionParser.parse(config.manualRegions());
				if (!manual.isEmpty())
				{
					areaManager.setPreviewRegions(manual);
				}
				break;

			case CURRENT_PLUS_N:
				Player local = client.getLocalPlayer();
				if (local != null)
				{
					int startRegionId = local.getWorldLocation().getRegionID();
					areaManager.setPreviewRegions(
							buildConnectedRegionCluster(startRegionId, config.regionCount())
					);
				}
				break;

			case NONE:
			default:
				break;
		}
	}

	private void generateRandomPreviewArena()
	{
		int startRegion = AreaRandomUtil.randomRegionAnywhere(client);
		areaManager.setPreviewRegions(
				buildConnectedRegionCluster(startRegion, config.regionCount())
		);
	}

	private void generateActiveArenaFromConfig()
	{
		switch (config.arenaMode())
		{
			case PRESET:
				if (config.presetArena().isValid())
				{
					areaManager.setActiveRegions(config.presetArena().getRegions());
				}
				break;

			case RANDOM:
				int startRegion = AreaRandomUtil.randomRegionAnywhere(client);
				areaManager.generatePlayerRegionAreaFromRegion(startRegion, config.regionCount());
				break;

			case MANUAL:
				var manual = ManualRegionParser.parse(config.manualRegions());
				if (!manual.isEmpty())
				{
					areaManager.setActiveRegions(manual);
				}
				break;

			case CURRENT_PLUS_N:
			default:
				areaManager.generatePlayerRegionArea(config.regionCount());
				break;
		}
	}

	/**
	 * Builds a connected region cluster (64x64 tiles per region) using the same logic as AreaManager,
	 * but returns a Set so we can use it for PREVIEW without touching allowedRegions.
	 */
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
       Ticks
       ========================= */

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (gameState != GameState.RUNNING)
		{
			return;
		}

		if (gameTimer.getRemainingSeconds() <= 0)
		{
			log.info("Game timer ended");
			stopGame();
		}
	}

    /* =========================
       Accessors (Overlay / UI)
       ========================= */

	public boolean isGameRunning()
	{
		return gameState == GameState.RUNNING;
	}

	public int getRemainingSeconds()
	{
		return gameTimer != null ? gameTimer.getRemainingSeconds() : 0;
	}

	public ArenaMode getArenaMode()
	{
		return config.arenaMode();
	}

	public int getActiveRegionCount()
	{
		return areaManager.getAllowedRegions().size();
	}

	public boolean isHost()
	{
		return hostAuthorityManager.isHost();
	}

    /* =========================
       Chat helper
       ========================= */

	private void pluginMessage(String message)
	{
		client.addChatMessage(
				ChatMessageType.GAMEMESSAGE,
				"",
				"[Infected] " + message,
				null
		);
	}
}
