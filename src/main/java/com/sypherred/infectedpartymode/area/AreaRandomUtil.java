package com.sypherred.infectedpartymode.area;

import net.runelite.api.Client;
import net.runelite.api.Player;

import java.util.Random;

public final class AreaRandomUtil
{
    private static final Random RNG = new Random();

    private AreaRandomUtil() {}

    public static int randomRegionAnywhere(Client client)
    {
        Player p = client.getLocalPlayer();
        if (p == null)
        {
            throw new IllegalStateException("Local player is null");
        }

        int base = p.getWorldLocation().getRegionID();
        int rx = base >> 8;
        int ry = base & 0xFF;

        int dx = RNG.nextInt(20) - 10;
        int dy = RNG.nextInt(20) - 10;

        return ((rx + dx) << 8) | (ry + dy);
    }
}
