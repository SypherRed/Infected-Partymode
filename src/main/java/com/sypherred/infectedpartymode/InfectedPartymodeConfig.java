package com.sypherred.infectedpartymode;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

import com.sypherred.infectedpartymode.area.ArenaMode;
import com.sypherred.infectedpartymode.area.PresetArena;
import com.sypherred.infectedpartymode.model.InfectionMode;

@ConfigGroup("infectedpartymode")
public interface InfectedPartymodeConfig extends Config
{
    /* =========================
       Arena
       ========================= */

    @ConfigSection(
            name = "Arena",
            description = "Arena configuration",
            position = 0
    )
    String arenaSection = "arena";

    @ConfigItem(
            keyName = "arenaMode",
            name = "Arena Mode",
            description = "How the arena is generated before the game starts",
            section = arenaSection,
            position = 0
    )
    default ArenaMode arenaMode()
    {
        return ArenaMode.NONE;
    }

    @ConfigItem(
            keyName = "regionCount",
            name = "Arena Size (Regions)",
            description = "Number of connected regions used for the arena",
            section = arenaSection,
            position = 1
    )
    @Range(min = 1, max = 10)
    default int regionCount()
    {
        return 1;
    }

    @ConfigItem(
            keyName = "presetArena",
            name = "Preset Arena",
            description = "Select a predefined arena (used in Preset mode)",
            section = arenaSection,
            position = 2
    )
    default PresetArena presetArena()
    {
        return PresetArena.NONE;
    }

    @ConfigItem(
            keyName = "manualRegions",
            name = "Manual Region IDs",
            description = "Comma-separated region IDs (used in Manual mode)",
            section = arenaSection,
            position = 3
    )
    default String manualRegions()
    {
        return "";
    }

    /* =========================
       Game
       ========================= */

    @ConfigSection(
            name = "Game",
            description = "Game rules and duration",
            position = 10
    )
    String gameSection = "game";

    @ConfigItem(
            keyName = "gameDurationMinutes",
            name = "Game Duration (Minutes)",
            description = "Total duration of the game in minutes",
            section = gameSection,
            position = 0
    )
    @Range(min = 1, max = 60)
    default int gameDurationMinutes()
    {
        return 10;
    }

    @ConfigItem(
            keyName = "infectionMode",
            name = "Infection Mode",
            description = "How initial infected players are selected",
            section = gameSection,
            position = 1
    )
    default InfectionMode infectionMode()
    {
        return InfectionMode.RANDOM;
    }

    @ConfigItem(
            keyName = "initialInfectedCount",
            name = "Initial Infected Players",
            description = "Number of players that start infected (Random mode only)",
            section = gameSection,
            position = 2
    )
    @Range(min = 1, max = 10)
    default int initialInfectedCount()
    {
        return 1;
    }

    /* =========================
       Debug
       ========================= */

    @ConfigSection(
            name = "Debug",
            description = "Debug and development tools",
            position = 90
    )
    String debugSection = "debug";

    @ConfigItem(
            keyName = "debugShowRegionIds",
            name = "Show Region IDs on Worldmap",
            description = "Draws region tiles with their region IDs on the world map",
            section = debugSection,
            position = 0
    )
    default boolean debugShowRegionIds()
    {
        return false;
    }
}
