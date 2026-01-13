package com.sypherred.infectedpartymode.model;

public class GameSession
{
    private boolean running;
    private long startTimeMillis;
    private int durationSeconds;

    public void start(long startTimeMillis, int durationSeconds)
    {
        this.running = true;
        this.startTimeMillis = startTimeMillis;
        this.durationSeconds = durationSeconds;
    }

    public void stop()
    {
        this.running = false;
        this.startTimeMillis = 0;
        this.durationSeconds = 0;
    }

    public boolean isRunning()
    {
        return running;
    }

    public long getStartTimeMillis()
    {
        return startTimeMillis;
    }

    public int getDurationSeconds()
    {
        return durationSeconds;
    }

    public long getRemainingMillis()
    {
        if (!running)
        {
            return 0;
        }

        long elapsed = System.currentTimeMillis() - startTimeMillis;
        long remaining = durationSeconds * 1000L - elapsed;

        return Math.max(remaining, 0);
    }
}
