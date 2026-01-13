package com.sypherred.infectedpartymode.area;

/**
 * Describes a rectangular area made of chunks.
 *
 * Chunk = 8x8 tiles
 */
public class ChunkArea
{
    private final int baseChunkX;
    private final int baseChunkY;
    private final int widthChunks;
    private final int heightChunks;

    public ChunkArea(int baseChunkX, int baseChunkY, int widthChunks, int heightChunks)
    {
        this.baseChunkX = baseChunkX;
        this.baseChunkY = baseChunkY;
        this.widthChunks = widthChunks;
        this.heightChunks = heightChunks;
    }

    public int getBaseChunkX()
    {
        return baseChunkX;
    }

    public int getBaseChunkY()
    {
        return baseChunkY;
    }

    public int getWidthChunks()
    {
        return widthChunks;
    }

    public int getHeightChunks()
    {
        return heightChunks;
    }

    /**
     * Checks whether a chunk coordinate lies inside this area.
     */
    public boolean containsChunk(int chunkX, int chunkY)
    {
        return chunkX >= baseChunkX
                && chunkX < baseChunkX + widthChunks
                && chunkY >= baseChunkY
                && chunkY < baseChunkY + heightChunks;
    }
}
