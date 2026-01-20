package com.sypherred.infectedpartymode.rules;

import com.sypherred.infectedpartymode.party.PartySyncManager;
import com.sypherred.infectedpartymode.model.InfectionState;
import com.sypherred.infectedpartymode.model.PlayerState;

import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

/**
 * Handles infection spread between players.
 */
public class InfectionManager
{
    private static final int INFECTION_RADIUS_TILES = 1;
    private static final int REQUIRED_TICKS = 3;
    private static final String PREFIX = "[Infected] ";

    private final Client client;
    private final PartySyncManager partySyncManager;

    private int nearInfectedTicks = 0;

    @Inject
    public InfectionManager(
            Client client,
            PartySyncManager partySyncManager
    )
    {
        this.client = client;
        this.partySyncManager = partySyncManager;
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        Player local = client.getLocalPlayer();
        if (local == null)
        {
            nearInfectedTicks = 0;
            return;
        }

        Map<String, PlayerState> states = partySyncManager.getPlayerStates();
        PlayerState localState = states.get(local.getName());

        // Only healthy players can get infected
        if (localState == null || localState.getInfectionState() != InfectionState.HEALTHY)
        {
            nearInfectedTicks = 0;
            return;
        }

        List<Player> players = client.getPlayers();
        WorldPoint localPos = local.getWorldLocation();

        boolean nearInfected = false;

        for (Player p : players)
        {
            if (p == null || p == local)
            {
                continue;
            }

            PlayerState otherState = states.get(p.getName());
            if (otherState == null || otherState.getInfectionState() != InfectionState.INFECTED)
            {
                continue;
            }

            WorldPoint otherPos = p.getWorldLocation();
            if (localPos.distanceTo(otherPos) <= INFECTION_RADIUS_TILES)
            {
                nearInfected = true;
                break;
            }
        }

        if (nearInfected)
        {
            nearInfectedTicks++;

            if (nearInfectedTicks >= REQUIRED_TICKS)
            {
                infectLocalPlayer(local.getName());
                nearInfectedTicks = 0;
            }
        }
        else
        {
            nearInfectedTicks = 0;
        }
    }

    private void infectLocalPlayer(String playerName)
    {
        client.addChatMessage(
                ChatMessageType.GAMEMESSAGE,
                "",
                PREFIX + "You got infected!",
                null
        );

        partySyncManager.sendInfectionState(
                playerName,
                InfectionState.INFECTED
        );
    }
}
