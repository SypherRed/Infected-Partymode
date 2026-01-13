package com.sypherred.infectedpartymode.party;

import com.sypherred.infectedpartymode.area.AreaManager;
import com.sypherred.infectedpartymode.area.ChunkArea;
import com.sypherred.infectedpartymode.model.GameSession;
import com.sypherred.infectedpartymode.model.InfectionState;
import com.sypherred.infectedpartymode.model.PlayerState;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.messages.PartyChatMessage;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class PartySyncManager
{
    private static final String PREFIX = "IPM|"; // Infected PartyMode

    private final PartyService partyService;
    private final AreaManager areaManager;

    private final GameSession gameSession = new GameSession();
    private final Map<String, PlayerState> playerStates = new HashMap<>();

    private boolean inParty = false;

    @Inject
    public PartySyncManager(PartyService partyService, AreaManager areaManager)
    {
        this.partyService = partyService;
        this.areaManager = areaManager;
    }

    /* =========================
       Party state
       ========================= */

    @Subscribe
    public void onPartyChanged(PartyChanged e)
    {
        // partyId ist null, wenn du in keiner Party bist
        inParty = e.getPartyId() != null;
        log.debug("Party changed: inParty={}", inParty);
    }

    private void sendPartyString(String payload)
    {
        // Wenn du nicht in einer Party bist, darfst du NICHT senden (sonst: "no session id")
        if (!inParty)
        {
            log.debug("Not in party -> skip send: {}", payload);
            return;
        }

        partyService.send(new PartyChatMessage(PREFIX + payload));
    }

    /* =========================
       Infection sync
       ========================= */

    public void sendInfectionState(String playerName, InfectionState state)
    {
        // Local immer aktualisieren
        updateLocalState(playerName, state);

        // Remote via PartyChatMessage
        sendPartyString("INFECT|" + playerName + "|" + state.name());
    }

    /* =========================
       Area sync
       ========================= */

    public void sendArea(ChunkArea area)
    {
        if (area == null)
        {
            return;
        }

        areaManager.setActiveArea(area);

        sendPartyString("AREA|"
                + area.getBaseChunkX() + "|"
                + area.getBaseChunkY() + "|"
                + area.getWidthChunks() + "|"
                + area.getHeightChunks());
    }

    /* =========================
       Timer / Game start sync
       ========================= */

    public void sendGameStart(int durationSeconds)
    {
        long start = System.currentTimeMillis();

        gameSession.start(start, durationSeconds);

        sendPartyString("TIMER|" + start + "|" + durationSeconds);
    }

    /* =========================
       Receive PartyChatMessage
       ========================= */

    @Subscribe
    public void onPartyChatMessage(PartyChatMessage msg)
    {
        String v = msg.getValue();
        if (v == null || !v.startsWith(PREFIX))
        {
            return;
        }

        String payload = v.substring(PREFIX.length());
        String[] parts = payload.split("\\|");
        if (parts.length == 0)
        {
            return;
        }

        switch (parts[0])
        {
            case "INFECT":
                // INFECT|player|STATE
                if (parts.length >= 3)
                {
                    String player = parts[1];
                    InfectionState state = safeInfectionState(parts[2]);
                    if (state != null)
                    {
                        updateLocalState(player, state);
                    }
                }
                break;

            case "AREA":
                // AREA|baseX|baseY|w|h
                if (parts.length >= 5)
                {
                    Integer baseX = tryParseInt(parts[1]);
                    Integer baseY = tryParseInt(parts[2]);
                    Integer w = tryParseInt(parts[3]);
                    Integer h = tryParseInt(parts[4]);

                    if (baseX != null && baseY != null && w != null && h != null)
                    {
                        areaManager.setActiveArea(new ChunkArea(baseX, baseY, w, h));
                    }
                }
                break;

            case "TIMER":
                // TIMER|startMillis|durationSeconds
                if (parts.length >= 3)
                {
                    Long start = tryParseLong(parts[1]);
                    Integer dur = tryParseInt(parts[2]);

                    if (start != null && dur != null)
                    {
                        gameSession.start(start, dur);
                    }
                }
                break;

            default:
                // ignore
                break;
        }
    }

    /* =========================
       Local state
       ========================= */

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

    public GameSession getGameSession()
    {
        return gameSession;
    }

    /* =========================
       Helpers
       ========================= */

    private static Integer tryParseInt(String s)
    {
        try
        {
            return Integer.parseInt(s);
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    private static Long tryParseLong(String s)
    {
        try
        {
            return Long.parseLong(s);
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    private static InfectionState safeInfectionState(String s)
    {
        try
        {
            return InfectionState.valueOf(s);
        }
        catch (Exception ex)
        {
            return null;
        }
    }
}
