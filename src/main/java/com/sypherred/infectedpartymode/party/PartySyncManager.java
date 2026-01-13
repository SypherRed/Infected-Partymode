package com.sypherred.infectedpartymode.party;

import com.sypherred.infectedpartymode.model.InfectionState;
import com.sypherred.infectedpartymode.model.PlayerState;
import com.sypherred.infectedpartymode.area.ChunkArea;
import com.sypherred.infectedpartymode.area.AreaManager;

import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.party.PartyService;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class PartySyncManager
{
    private final PartyService partyService;
    private final AreaManager areaManager;

    private final Map<String, PlayerState> playerStates = new HashMap<>();

    @Inject
    public PartySyncManager(PartyService partyService, AreaManager areaManager)
    {
        this.partyService = partyService;
        this.areaManager = areaManager;
    }

    /* =========================
       Infection sync
       ========================= */

    public void sendInfectionState(String playerName, InfectionState state)
    {
        partyService.send(new InfectionSyncMessage(playerName, state));
        updateLocalState(playerName, state);
    }

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

    /* =========================
       Area sync (Phase 4b)
       ========================= */

    public void sendArea(ChunkArea area)
    {
        if (area == null)
        {
            return;
        }

        partyService.send(new AreaSyncMessage(
                area.getBaseChunkX(),
                area.getBaseChunkY(),
                area.getWidthChunks(),
                area.getHeightChunks()
        ));

        areaManager.setActiveArea(area);
    }

    @Subscribe
    public void onAreaSyncMessage(AreaSyncMessage msg)
    {
        ChunkArea area = new ChunkArea(
                msg.getBaseChunkX(),
                msg.getBaseChunkY(),
                msg.getWidthChunks(),
                msg.getHeightChunks()
        );

        areaManager.setActiveArea(area);
    }
}
