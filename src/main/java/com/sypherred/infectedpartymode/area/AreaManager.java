package com.sypherred.infectedpartymode.area;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

import javax.inject.Inject;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles creation and validation of the chunk-based arena.
 */
public class AreaManager
{
    private static final Logger log =
            LoggerFactory.getLogger(AreaManager.class);

    private static final int CHUNK_SIZE = 8;

    private final Client client;
    private final Random random = new Random();

    private ChunkArea activeArea;

    @Inject
    public AreaManager(Client client)
    {
        this.client = client;
    }

    /**
     * Generates a random chunk area around the local player.
     *
     * @param maxChunks max width/height in chunks (e.g. 3)
     */
    public ChunkArea generateRandomArea(int maxChunks)
    {
        Player local = client.getLocalPlayer();
        if (local == null)
        {
            log.warn("Cannot generate arena: local player is null");
            return null;
        }

        WorldPoint wp = local.getWorldLocation();

        int baseChunkX = wp.getX() >> 3;
        int baseChunkY = wp.getY() >> 3;

        int widthChunks = 1 + random.nextInt(maxChunks);
        int heightChunks = 1 + random.nextInt(maxChunks);

        activeArea = new ChunkArea(
                baseChunkX,
                baseChunkY,
                widthChunks,
                heightChunks
        );

        log.info(
                "Generated arena at chunk {}, {} ({}x{} chunks)",
                baseChunkX,
                baseChunkY,
                widthChunks,
                heightChunks
        );

        return activeArea;
    }

    public ChunkArea getActiveArea()
    {
        return activeArea;
    }

    public void setActiveArea(ChunkArea area)
    {
        this.activeArea = area;
    }

    public void clearArea()
    {
        activeArea = null;
        log.info("Arena cleared");
    }

    /**
     * Checks if a world point lies inside the active chunk area.
     */
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
