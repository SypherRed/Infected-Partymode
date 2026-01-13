package com.sypherred.infectedpartymode.party;

import net.runelite.client.party.messages.PartyMemberMessage;

/**
 * Syncs game start time & duration.
 */
public class TimerSyncMessage extends PartyMemberMessage
{
    private long startTimeMillis;
    private int durationSeconds;

    public TimerSyncMessage()
    {
    }

    public TimerSyncMessage(long startTimeMillis, int durationSeconds)
    {
        this.startTimeMillis = startTimeMillis;
        this.durationSeconds = durationSeconds;
    }

    public long getStartTimeMillis()
    {
        return startTimeMillis;
    }

    public int getDurationSeconds()
    {
        return durationSeconds;
    }
}
