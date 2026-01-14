package com.sypherred.infectedpartymode.rules;

import com.sypherred.infectedpartymode.area.AreaManager;
import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;

/**
 * Checks whether the local player leaves the active region-based arena.
 */
public class OutOfBoundsManager
{
    private static final int GRACE_TICKS = 5; // ~3 seconds

    private final Client client;
    private final AreaManager areaManager;

    private int outOfBoundsTicks = 0;
    private boolean warned = false;

    @Inject
    public OutOfBoundsManager(Client client, AreaManager areaManager)
    {
        this.client = client;
        this.areaManager = areaManager;
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

        // No active arena → no checks
        if (!areaManager.hasActiveArea())
        {
            reset();
            return;
        }

        WorldPoint wp = local.getWorldLocation();

        // Player is inside allowed region(s)
        if (areaManager.isInsideArea(wp))
        {
            reset();
            return;
        }

        // Player is outside
        outOfBoundsTicks++;

        if (outOfBoundsTicks >= GRACE_TICKS && !warned)
        {
            warned = true;
            client.addChatMessage(
                    ChatMessageType.GAMEMESSAGE,
                    "",
                    "You have left the arena! Please return.",
                    null
            );

        }
    }

    private void reset()
    {
        outOfBoundsTicks = 0;
        warned = false;
    }
}
