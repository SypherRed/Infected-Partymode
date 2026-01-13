package com.sypherred.infectedpartymode;

import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.party.PartyService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.ClientToolbar;

import com.sypherred.infectedpartymode.party.PartySyncManager;
import com.sypherred.infectedpartymode.area.AreaManager;
import com.sypherred.infectedpartymode.rules.OutOfBoundsManager;
import com.sypherred.infectedpartymode.overlay.ArenaBorderOverlay;
import com.sypherred.infectedpartymode.overlay.GameInfoOverlay;
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
	@Inject
	private Client client;

	@Inject
	private PartyService partyService;

	@Inject
	private PartySyncManager partySyncManager;

	@Inject
	private AreaManager areaManager;

	@Inject
	private OutOfBoundsManager outOfBoundsManager;

	@Inject
	private InfectedPartymodeConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ArenaBorderOverlay arenaBorderOverlay;

	@Inject
	private GameInfoOverlay gameInfoOverlay;

	@Inject
	private InfectionManager infectionManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private InfectedPanel infectedPanel;

	private NavigationButton navButton;

	@Provides
	InfectedPartymodeConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(InfectedPartymodeConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(arenaBorderOverlay);
		overlayManager.add(gameInfoOverlay);

		BufferedImage icon = null;

		try
		{
			icon = ImageIO.read(
					getClass().getResourceAsStream("/infected_icon.png")
			);
		}
		catch (IOException | IllegalArgumentException e)
		{
			// Icon couldn't be loaded - Plugin functional
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
		overlayManager.remove(arenaBorderOverlay);
		overlayManager.remove(gameInfoOverlay);

		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
			navButton = null;
		}
	}

}
