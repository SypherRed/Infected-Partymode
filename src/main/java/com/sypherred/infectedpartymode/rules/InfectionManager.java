package com.sypherred.infectedpartymode.rules;

import com.sypherred.infectedpartymode.party.PartySyncManager;
import com.sypherred.infectedpartymode.party.HostAuthorityManager;
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
 * Host-authoritative infection handling.
 */
public class InfectionManager
{
    private static final int INFECTION_RADIUS_TILES = 1;
    private static final String PREFIX = "[Infected] ";

    private final Client client;
    private final PartySyncManager partySyncManager;
    private final HostAuthorityManager hostAuthorityManager;

    private boolean infectionProcessedThisTick = false;

    @Inject
    public InfectionManager(
            Client client,
            PartySyncManager partySyncManager,
            HostAuthorityManager hostAuthorityManager
    )
    {
        this.client = client;
        this.partySyncManager = partySyncManager;
        this.hostAuthorityManager = hostAuthorityManager;
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        // reset guard every tick
        infectionProcessedThisTick = false;

        // only host decides infections
        if (!hostAuthorityManager.isHost())
        {
            return;
        }

        Map<String, PlayerState> states = partySyncManager.getPlayerStates();
        if (states.isEmpty())
        {
            return;
        }

        List<Player> players = client.getPlayers();

        for (Player attacker : players)
        {
            if (attacker == null)
            {
                continue;
            }

            PlayerState attackerState = states.get(attacker.getName());
            if (attackerState == null || attackerState.getInfectionState() != InfectionState.INFECTED)
            {
                continue;
            }

            WorldPoint attackerPos = attacker.getWorldLocation();

            for (Player victim : players)
            {
                if (victim == null || victim == attacker)
                {
                    continue;
                }

                PlayerState victimState = states.get(victim.getName());
                if (victimState == null || victimState.getInfectionState() != InfectionState.HEALTHY)
                {
                    continue;
                }

                if (infectionProcessedThisTick)
                {
                    return;
                }

                WorldPoint victimPos = victim.getWorldLocation();
                if (attackerPos.distanceTo(victimPos) <= INFECTION_RADIUS_TILES)
                {
                    infect(attacker.getName(), victim.getName(), states);
                    return; // exactly one infection per tick
                }
            }
        }
    }

    private void infect(String attacker, String victim, Map<String, PlayerState> states)
    {
        infectionProcessedThisTick = true;

        // update state via party sync
        partySyncManager.sendInfectionState(victim, InfectionState.INFECTED);

        int healthyLeft = (int) states.values().stream()
                .filter(s -> s.getInfectionState() == InfectionState.HEALTHY)
                .count() - 1;

        client.addChatMessage(
                ChatMessageType.GAMEMESSAGE,
                "",
                PREFIX + attacker + " has infected " + victim +
                        ". There are " + healthyLeft + " healthy players left.",
                null
        );
    }
}
