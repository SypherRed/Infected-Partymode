package com.sypherred.infectedpartymode;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

import com.sypherred.infectedpartymode.area.ArenaMode;
import com.sypherred.infectedpartymode.area.PresetArena;

@ConfigGroup("infectedpartymode")
public interface InfectedPartymodeConfig extends Config
{
    @ConfigItem(
            keyName = "arenaMode",
            name = "Arena mode",
            description = "Select how the arena is generated",
            position = 0
    )
    default ArenaMode arenaMode()
    {
        return ArenaMode.NONE;
    }

    @ConfigItem(
            keyName = "regionCount",
            name = "Region count",
            description = "Number of connected regions",
            position = 1
    )
    @Range(min = 1, max = 10)
    default int regionCount()
    {
        return 1;
    }

    @ConfigItem(
            keyName = "presetArena",
            name = "Preset arena",
            description = "Used when Arena mode = PRESET",
            position = 2
    )
    default PresetArena presetArena()
    {
        return PresetArena.LUMBRIDGE;
    }
}
