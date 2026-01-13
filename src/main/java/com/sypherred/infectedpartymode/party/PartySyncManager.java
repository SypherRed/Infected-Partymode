package com.sypherred.infectedpartymode.party;

import com.sypherred.infectedpartymode.model.InfectionState;
import com.sypherred.infectedpartymode.model.PlayerState;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.party.PartyService;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class PartySyncManager
{
    private final PartyService partyService;
    private final Map<String, PlayerState> playerStates = new HashMap<>();

    @Inject
    public PartySyncManager(PartyService partyService)
    {
        this.partyService = partyService;
    }

    /**
     * Host sends infection state
     */
    public void sendInfectionState(String playerName, InfectionState state)
    {
        partyService.send(new InfectionSyncMessage(playerName, state));
        updateLocalState(playerName, state);
    }

    /**
     * THIS is how Party messages are received
     */
    @Subscribe
    public void onInfectionSyncMessage(InfectionSyncMessage msg)
    {
        updateLocalState(msg.getPlayerName(), msg.getInfectionState());
    }

    private void updateLocalState(String playerName, InfectionState state)
    {
        playerStates.compute(playerName, (name, existing) ->
        {
            if (existing == null)
            {
                return new PlayerState(name, state);
            }
            existing.setInfectionState(state);
            return existing;
        });
    }

    public Map<String, PlayerState> getPlayerStates()
    {
        return playerStates;
    }
}
