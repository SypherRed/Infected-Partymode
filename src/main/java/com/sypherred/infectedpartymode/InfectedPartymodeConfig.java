package com.sypherred.infectedpartymode;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("infectedpartymode")
public interface InfectedPartymodeConfig extends Config
{
    @ConfigItem(
            keyName = "regionCount",
            name = "Region count",
            description = "Number of connected regions starting from the current player region",
            position = 0
    )
    @Range(
            min = 1,
            max = 10
    )
    default int regionCount()
    {
        return 1;
    }
}
