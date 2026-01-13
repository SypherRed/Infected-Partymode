package com.sypherred.infectedpartymode;

import com.google.inject.Provides;
import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import com.sypherred.infectedpartymode.party.PartySyncManager;
import com.sypherred.infectedpartymode.area.AreaManager;


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
	private InfectedPartymodeConfig config;

	@Provides
	InfectedPartymodeConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(InfectedPartymodeConfig.class);
	}

	@Override
	protected void startUp()
	{
		// PartySyncManager wird automatisch registriert (EventBus)
	}

	@Override
	protected void shutDown()
	{
		// später: State reset
	}

}
