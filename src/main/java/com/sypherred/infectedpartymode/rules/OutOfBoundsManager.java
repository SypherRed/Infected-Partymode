package com.sypherred.infectedpartymode.rules;

import com.sypherred.infectedpartymode.area.AreaManager;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.infobox.Timer;
import net.runelite.client.Notifier;

import javax.inject.Inject;

/**
 * Checks whether the local player leaves the active arena.
 */
public class OutOfBoundsManager
{
    private static final int GRACE_TICKS = 5; // ~3 seconds

    private final Client client;
    private final AreaManager areaManager;
    private final Notifier notifier;

    private int outOfBoundsTicks = 0;
    private boolean warned = false;

    @Inject
    public OutOfBoundsManager(Client client, AreaManager areaManager, Notifier notifier)
    {
        this.client = client;
        this.areaManager = areaManager;
        this.notifier = notifier;
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        Player local = client.getLocalPlayer();
        if (local == null)
        {
            reset();
            return;
        }

        if (areaManager.getActiveArea() == null)
        {
            reset();
            return;
        }

        WorldPoint wp = local.getWorldLocation();

        if (areaManager.isInsideArea(wp))
        {
            reset();
            return;
        }

        outOfBoundsTicks++;

        if (outOfBoundsTicks >= GRACE_TICKS && !warned)
        {
            warned = true;
            notifier.notify("You have left the arena! Return back!");
        }
    }

    private void reset()
    {
        outOfBoundsTicks = 0;
        warned = false;
    }
}
