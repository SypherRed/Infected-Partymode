package com.sypherred.infectedpartymode.area;

import net.runelite.api.Client;
import net.runelite.api.Player;

import java.util.Random;

public final class AreaRandomUtil
{
    private static final Random RNG = new Random();

    private AreaRandomUtil() {}

    public static int randomRegionNearPlayer(Client client, int radius)
    {
        Player p = client.getLocalPlayer();
        if (p == null)
        {
            throw new IllegalStateException("Local player is null");
        }

        int base = p.getWorldLocation().getRegionID();
        int rx = base >> 8;
        int ry = base & 0xFF;

        int dx = RNG.nextInt(radius * 2 + 1) - radius;
        int dy = RNG.nextInt(radius * 2 + 1) - radius;

        return ((rx + dx) << 8) | (ry + dy);
    }
}
