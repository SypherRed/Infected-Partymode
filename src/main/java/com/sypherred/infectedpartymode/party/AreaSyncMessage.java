package com.sypherred.infectedpartymode.party;

import net.runelite.client.party.messages.PartyMemberMessage;

/**
 * Syncs the active chunk arena to party members.
 */
public class AreaSyncMessage extends PartyMemberMessage
{
    private int baseChunkX;
    private int baseChunkY;
    private int widthChunks;
    private int heightChunks;

    // Required for deserialization
    public AreaSyncMessage()
    {
    }

    public AreaSyncMessage(int baseChunkX, int baseChunkY, int widthChunks, int heightChunks)
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
}
