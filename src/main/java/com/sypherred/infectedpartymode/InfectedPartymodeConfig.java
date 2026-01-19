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
            description = "How the arena regions are selected",
            position = 0
    )
    default ArenaMode arenaMode()
    {
        return ArenaMode.CURRENT_PLUS_N;
    }

    @ConfigItem(
            keyName = "regionCount",
            name = "Region count",
            description = "Number of connected regions starting from the current player region",
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
            description = "Fixed preset arena (used when Arena mode = PRESET)",
            position = 2
    )
    default PresetArena presetArena()
    {
        return PresetArena.LUMBRIDGE;
    }
}
