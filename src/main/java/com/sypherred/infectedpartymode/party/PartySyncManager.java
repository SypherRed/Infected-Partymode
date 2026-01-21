package com.sypherred.infectedpartymode.party;

import com.sypherred.infectedpartymode.area.AreaManager;
import com.sypherred.infectedpartymode.model.GameSession;
import com.sypherred.infectedpartymode.model.InfectionState;
import com.sypherred.infectedpartymode.model.PlayerState;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.messages.PartyChatMessage;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class PartySyncManager
{
    private static final String PREFIX = "IPM|"; // Infected PartyMode

    private final PartyService partyService;
    private final AreaManager areaManager;
    private final HostAuthorityManager hostAuthorityManager;
    private final EventBus eventBus;

    private final GameSession gameSession = new GameSession();
    private final Map<String, PlayerState> playerStates = new HashMap<>();

    private boolean inParty = false;

    @Inject
    public PartySyncManager(
            PartyService partyService,
            AreaManager areaManager,
            HostAuthorityManager hostAuthorityManager,
            EventBus eventBus
    )
    {
        this.partyService = partyService;
        this.areaManager = areaManager;
        this.hostAuthorityManager = hostAuthorityManager;
        this.eventBus = eventBus;
    }

    /* =========================
       Party state
       ========================= */

    @Subscribe
    public void onPartyChanged(PartyChanged e)
    {
        inParty = e.getPartyId() != null;

        if (!inParty)
        {
            hostAuthorityManager.reset();
            playerStates.clear();
            postStatesUpdated();
        }

        log.debug("Party changed: inParty={}", inParty);
    }

    private void sendPartyString(String payload)
    {
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
        updateLocalState(playerName, state);
        sendPartyString("INFECT|" + playerName + "|" + state.name());
    }

    /**
     * Ensures a player exists in the local state map (default HEALTHY).
     */
    public void ensurePlayerHealthy(String playerName)
    {
        if (playerName == null || playerName.trim().isEmpty())
        {
            return;
        }

        if (!playerStates.containsKey(playerName))
        {
            playerStates.put(playerName, new PlayerState(playerName, InfectionState.HEALTHY));
            postStatesUpdated();
        }
    }

    /* =========================
       Area (Region) sync
       ========================= */

    public void sendArea()
    {
        Set<Integer> regions = areaManager.getAllowedRegions();
        if (regions.isEmpty())
        {
            return;
        }

        String payload = regions.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        sendPartyString("AREA|" + payload);
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
       Host sync
       ========================= */

    public void sendHostClaim(long memberId)
    {
        sendPartyString("HOST|" + memberId);
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
                if (parts.length >= 2)
                {
                    Set<Integer> regions = parseRegionSet(parts[1]);
                    if (!regions.isEmpty())
                    {
                        areaManager.clearArea();
                        areaManager.setActiveRegions(regions);
                        log.info("Received arena regions from party: {}", regions);
                    }
                }
                break;

            case "TIMER":
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

            case "HOST":
                if (parts.length >= 2)
                {
                    Long memberId = tryParseLong(parts[1]);
                    if (memberId != null)
                    {
                        hostAuthorityManager.onHostClaim(memberId);
                        log.info("Host claimed by memberId={}", memberId);
                    }
                }
                break;

            default:
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

        postStatesUpdated();
    }

    public Map<String, PlayerState> getPlayerStates()
    {
        return playerStates;
    }

    public GameSession getGameSession()
    {
        return gameSession;
    }

    private void postStatesUpdated()
    {
        // PlayerStatesUpdated is an enum in your project -> post INSTANCE, not new(...)
        eventBus.post(PlayerStatesUpdated.INSTANCE);
    }

    /* =========================
       Helpers
       ========================= */

    private static Set<Integer> parseRegionSet(String csv)
    {
        Set<Integer> set = new HashSet<>();
        for (String s : csv.split(","))
        {
            Integer v = tryParseInt(s);
            if (v != null)
            {
                set.add(v);
            }
        }
        return set;
    }

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
