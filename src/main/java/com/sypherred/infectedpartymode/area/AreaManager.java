package com.sypherred.infectedpartymode.area;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * Region-based arena manager.
 *
 * A region = 64x64 tiles (8x8 chunks).
 * The arena always consists of a connected region cluster.
 */
@Singleton
public class AreaManager
{
    private static final Logger log =
            LoggerFactory.getLogger(AreaManager.class);

    private final Client client;

    /** Allowed region IDs for the current arena */
    private final Set<Integer> allowedRegions = new HashSet<>();

    @Inject
    public AreaManager(Client client)
    {
        this.client = client;
    }

    /* =========================
       Arena generation
       ========================= */

    /**
     * Generates a region-based arena starting at the player's current region.
     *
     * @param regionCount number of connected regions (>= 1)
     */
    public void generatePlayerRegionArea(int regionCount)
    {
        Player local = client.getLocalPlayer();
        if (local == null)
        {
            log.warn("Cannot generate arena: local player is null");
            return;
        }

        if (regionCount < 1)
        {
            regionCount = 1;
        }

        int startRegionId = local.getWorldLocation().getRegionID();

        allowedRegions.clear();
        allowedRegions.add(startRegionId);

        Queue<Integer> frontier = new LinkedList<>();
        frontier.add(startRegionId);

        while (!frontier.isEmpty() && allowedRegions.size() < regionCount)
        {
            int regionId = frontier.poll();

            int rx = regionId >> 8;
            int ry = regionId & 0xFF;

            int[][] neighbors = {
                    {rx + 1, ry},
                    {rx - 1, ry},
                    {rx, ry + 1},
                    {rx, ry - 1}
            };

            for (int[] n : neighbors)
            {
                int neighborId = (n[0] << 8) | n[1];

                if (allowedRegions.add(neighborId))
                {
                    frontier.add(neighborId);

                    if (allowedRegions.size() >= regionCount)
                    {
                        break;
                    }
                }
            }
        }

        log.info(
                "Arena generated: {} region(s), start region {}",
                allowedRegions.size(),
                startRegionId
        );
    }

    /* =========================
       Arena state
       ========================= */

    public void clearArea()
    {
        allowedRegions.clear();
        log.info("Arena cleared");
    }

    public boolean hasActiveArea()
    {
        return !allowedRegions.isEmpty();
    }

    /**
     * Checks if a world point lies inside the allowed region set.
     */
    public boolean isInsideArea(WorldPoint point)
    {
        if (point == null)
        {
            return false;
        }

        return allowedRegions.contains(point.getRegionID());
    }

    /**
     * Returns a copy of the allowed region IDs.
     */
    public Set<Integer> getAllowedRegions()
    {
        return Set.copyOf(allowedRegions);
    }
}
