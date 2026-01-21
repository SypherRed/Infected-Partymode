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
 * Tracks whether the local player is outside the active arena
 * and provides escalation state for visual feedback.
 */
public class OutOfBoundsManager
{
    private static final int GRACE_TICKS = 5;   // ~3 seconds
    private static final int DANGER_TICKS = 12; // ~7 seconds
    private static final String PREFIX = "[Infected] ";

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
        if (local == null || !areaManager.hasActiveArea())
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

        // Player is outside the arena
        outOfBoundsTicks++;

        if (outOfBoundsTicks >= GRACE_TICKS && !warned)
        {
            warned = true;
            client.addChatMessage(
                    ChatMessageType.GAMEMESSAGE,
                    "",
                    PREFIX + "<col=ff0000>You have left the arena! Return immediately!</col>",
                    null
            );
        }
    }

    private void reset()
    {
        outOfBoundsTicks = 0;
        warned = false;
    }

    /* =========================
       Overlay state accessors
       ========================= */

    public boolean isOutOfBounds()
    {
        return outOfBoundsTicks > 0;
    }

    public boolean isDanger()
    {
        return outOfBoundsTicks >= DANGER_TICKS;
    }

    public int getOutOfBoundsTicks()
    {
        return outOfBoundsTicks;
    }
}
