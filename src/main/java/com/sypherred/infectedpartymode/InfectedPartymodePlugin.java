package com.sypherred.infectedpartymode;

import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.party.PartyService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.ClientToolbar;


import com.sypherred.infectedpartymode.area.AreaManager;
import com.sypherred.infectedpartymode.game.GameState;
import com.sypherred.infectedpartymode.game.GameTimer;
//import com.sypherred.infectedpartymode.overlay.ArenaBorderSceneOverlay;
//import com.sypherred.infectedpartymode.overlay.ArenaDebugOverlay;
import com.sypherred.infectedpartymode.overlay.ArenaFillOverlay;
//import com.sypherred.infectedpartymode.overlay.GameInfoOverlay;
//import com.sypherred.infectedpartymode.overlay.SceneTileDebugOverlay;
import com.sypherred.infectedpartymode.party.PartySyncManager;
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

	//@Inject
	//private ArenaBorderSceneOverlay arenaBorderSceneOverlay;

	//@Inject
	//private ArenaDebugOverlay arenaDebugOverlay;

	@Inject
	private ArenaFillOverlay arenaFillOverlay;

	@Inject
	private AreaManager areaManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private Client client;

	@Inject
	private EventBus eventBus;

	//@Inject
	//private GameInfoOverlay gameInfoOverlay;

	@Inject
	private InfectedPartymodeConfig config;

	@Inject
	private InfectionManager infectionManager;

	@Inject
	private InfectedPanel infectedPanel;

	@Inject
	private OutOfBoundsManager outOfBoundsManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private PartyService partyService;

	@Inject
	private PartySyncManager partySyncManager;


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
		log.info("Infected Partymode starting");

		eventBus.register(outOfBoundsManager);
		gameTimer = new GameTimer(executor);

		overlayManager.add(arenaFillOverlay);
		//overlayManager.add(arenaDebugOverlay);
		// overlayManager.add(arenaBorderSceneOverlay);
		//overlayManager.add(gameInfoOverlay);

		BufferedImage icon = null;
		try
		{
			icon = ImageIO.read(
					getClass().getResourceAsStream("/infected_icon.png")
			);
		}
		catch (IOException | IllegalArgumentException e)
		{
			log.warn("Could not load plugin icon");
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

		if (outOfBoundsManager != null)
		{
			eventBus.unregister(outOfBoundsManager);
		}

		stopGame();

		overlayManager.remove(arenaFillOverlay);
		//overlayManager.remove(arenaDebugOverlay);
		// overlayManager.remove(arenaBorderSceneOverlay);
		//overlayManager.remove(gameInfoOverlay);

		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
			navButton = null;
		}
	}

	public void startGame(int durationSeconds)
	{
		if (gameState == GameState.RUNNING)
		{
			log.warn("Game already running");
			return;
		}

		if (client.getLocalPlayer() == null)
		{
			log.warn("Cannot start game: local player is null");
			return;
		}

		log.info("Starting game for {} seconds", durationSeconds);

		gameState = GameState.RUNNING;
		gameTimer.start(durationSeconds);

		areaManager.generatePlayerChunkArea();
	}

	public void stopGame()
	{
		if (gameState == GameState.IDLE)
		{
			return;
		}

		log.info("Stopping game");

		gameState = GameState.IDLE;

		if (gameTimer != null)
		{
			gameTimer.stop();
		}

		areaManager.clearArea();
	}

	@Subscribe
	public void onGameTick(net.runelite.api.events.GameTick tick)
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

	public boolean isGameRunning()
	{
		return gameState == GameState.RUNNING;
	}

	public int getRemainingSeconds()
	{
		return gameTimer != null ? gameTimer.getRemainingSeconds() : 0;
	}
}
