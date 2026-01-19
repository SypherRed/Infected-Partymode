package com.sypherred.infectedpartymode.area;

import java.util.Set;

public enum PresetArena
{
    NONE(Set.of()),

    LUMBRIDGE(Set.of(
            12850
    )),

    VARROCK(Set.of(
            12853
    )),

    FALADOR(Set.of(
            11828
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

    public boolean isValid()
    {
        return this != NONE && !regions.isEmpty();
    }
}
