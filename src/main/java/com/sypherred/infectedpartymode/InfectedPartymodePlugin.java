package com.sypherred.infectedpartymode;

import com.google.inject.Provides;

import javax.inject.Inject;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
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
import com.sypherred.infectedpartymode.game.GameState;
import com.sypherred.infectedpartymode.game.GameTimer;
import com.sypherred.infectedpartymode.overlay.ArenaRegionShadeOverlay;
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

	@Inject
	private InfectionManager infectionManager;

    /* =========================
       UI
       ========================= */

	@Inject
	private InfectedPanel infectedPanel;

    /* =========================
       Overlay
       ========================= */

	@Inject
	private ArenaRegionShadeOverlay arenaRegionShadeOverlay;

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

		// Host enforcement
		if (!hostAuthorityManager.isHost())
		{
			client.addChatMessage(
					ChatMessageType.GAMEMESSAGE,
					"",
					"Only the host can start the game.",
					null
			);
			return;
		}

		// First host claims authority (party-safe)
		if (hostAuthorityManager.getHostMemberId() == null)
		{
			hostAuthorityManager.claimHost();
			if (partyService.isInParty())
			{
				partySyncManager.sendHostClaim(
						partyService.getLocalMember().getMemberId()
				);
			}
		}

		int regionCount = config.regionCount();
		log.info("Starting game for {} seconds with {} regions", durationSeconds, regionCount);

		gameState = GameState.RUNNING;
		gameTimer.start(durationSeconds);

		areaManager.generatePlayerRegionArea(regionCount);
	}

	public void stopGame()
	{
		if (gameState == GameState.IDLE)
		{
			return;
		}

		if (!hostAuthorityManager.isHost())
		{
			client.addChatMessage(
					ChatMessageType.GAMEMESSAGE,
					"",
					"Only the host can stop the game.",
					null
			);
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
       Accessors
       ========================= */

	public boolean isGameRunning()
	{
		return gameState == GameState.RUNNING;
	}

	public int getRemainingSeconds()
	{
		return gameTimer != null ? gameTimer.getRemainingSeconds() : 0;
	}
}
