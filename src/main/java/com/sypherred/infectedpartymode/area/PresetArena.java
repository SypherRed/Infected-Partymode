package com.sypherred.infectedpartymode.area;

import java.util.Set;

public enum PresetArena
{
    LUMBRIDGE(Set.of(
            12850 // Lumbridge region
    )),

    VARROCK(Set.of(
            12853 // Varrock west/east core region
    )),

    FALADOR(Set.of(
            11828 // Falador core region
    ));

    private final Set<Integer> regions;

    PresetArena(Set<Integer> regions)
    {
        this.regions = regions;
    }

    public Set<Integer> getRegions()
    {
        return regions;
    }
}
