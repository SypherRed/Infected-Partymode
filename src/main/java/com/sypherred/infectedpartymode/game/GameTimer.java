package com.sypherred.infectedpartymode.game;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class GameTimer
{
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> task;

    private int remainingSeconds;

    public GameTimer(ScheduledExecutorService executor)
    {
        this.executor = executor;
    }

    public void start(int seconds)
    {
        stop();
        remainingSeconds = seconds;

        task = executor.scheduleAtFixedRate(
                () -> {
                    if (remainingSeconds > 0)
                    {
                        remainingSeconds--;
                    }
                },
                1,
                1,
                TimeUnit.SECONDS
        );
    }

    public void stop()
    {
        if (task != null)
        {
            task.cancel(false);
            task = null;
        }
    }

    public int getRemainingSeconds()
    {
        return remainingSeconds;
    }
}
