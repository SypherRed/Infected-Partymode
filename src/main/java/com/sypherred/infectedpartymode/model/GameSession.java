package com.sypherred.infectedpartymode.model;

public class GameSession
{
    private boolean running;
    private long startTimeMillis;
    private int durationSeconds;

    public boolean isRunning()
    {
        return running;
    }

    public void start(int durationSeconds)
    {
        this.running = true;
        this.startTimeMillis = System.currentTimeMillis();
        this.durationSeconds = durationSeconds;
    }

    public void stop()
    {
        this.running = false;
        this.startTimeMillis = 0;
        this.durationSeconds = 0;
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
