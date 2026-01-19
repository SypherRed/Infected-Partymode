package com.sypherred.infectedpartymode.area;

import java.util.HashSet;
import java.util.Set;

public final class ManualRegionParser
{
    private ManualRegionParser() {}

    public static Set<Integer> parse(String input)
    {
        Set<Integer> regions = new HashSet<>();
        if (input == null || input.isBlank())
        {
            return regions;
        }

        for (String part : input.split(","))
        {
            try
            {
                int id = Integer.parseInt(part.trim());
                if (id > 0)
                {
                    regions.add(id);
                }
            }
            catch (NumberFormatException ignored)
            {
            }
        }

        return regions;
    }
}
