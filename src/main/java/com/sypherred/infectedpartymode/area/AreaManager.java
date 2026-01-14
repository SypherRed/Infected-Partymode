package com.sypherred.infectedpartymode.area;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton // <<< DAS IST DER ENTSCHEIDENDE FIX
public class AreaManager
{
    private static final Logger log =
            LoggerFactory.getLogger(AreaManager.class);

    private final Client client;
    private ChunkArea activeArea;

    @Inject
    public AreaManager(Client client)
    {
        this.client = client;
    }

    public ChunkArea generatePlayerChunkArea()
    {
        Player local = client.getLocalPlayer();
        if (local == null)
        {
            log.warn("Cannot generate arena: local player is null");
            return null;
        }

        WorldPoint wp = local.getWorldLocation();

        int chunkX = wp.getX() >> 3;
        int chunkY = wp.getY() >> 3;

        activeArea = new ChunkArea(
                chunkX - 1,
                chunkY - 1,
                3,
                3
        );

        log.info(
                "Arena set to player chunk {}, {} (3x3)",
                chunkX,
                chunkY
        );

        return activeArea;
    }

    public void setActiveArea(ChunkArea area)
    {
        this.activeArea = area;
        log.info("Active arena set externally");
    }

    public ChunkArea getActiveArea()
    {
        return activeArea;
    }

    public void clearArea()
    {
        activeArea = null;
        log.info("Arena cleared");
    }

    public boolean isInsideArea(WorldPoint point)
    {
        if (activeArea == null || point == null)
        {
            return false;
        }

        int chunkX = point.getX() >> 3;
        int chunkY = point.getY() >> 3;

        return activeArea.containsChunk(chunkX, chunkY);
    }
}
