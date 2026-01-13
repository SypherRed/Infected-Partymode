package com.sypherred.infectedpartymode;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class InfectedPartymodePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(InfectedPartymodePlugin.class);
		RuneLite.main(args);
	}
}