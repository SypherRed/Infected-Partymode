package com.sypherred.infectedpartymode;

import com.google.inject.Provides;
import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import com.sypherred.infectedpartymode.party.PartySyncManager;
import com.sypherred.infectedpartymode.area.AreaManager;
import com.sypherred.infectedpartymode.rules.OutOfBoundsManager;
import com.sypherred.infectedpartymode.overlay.ArenaBorderOverlay;
import com.sypherred.infectedpartymode.overlay.GameInfoOverlay;

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
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(arenaBorderOverlay);
		overlayManager.remove(gameInfoOverlay);
	}

}
