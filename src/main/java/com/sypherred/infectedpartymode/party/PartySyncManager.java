package com.sypherred.infectedpartymode.party;

import com.sypherred.infectedpartymode.area.AreaManager;
import com.sypherred.infectedpartymode.model.GameSession;
import com.sypherred.infectedpartymode.model.InfectionState;
import com.sypherred.infectedpartymode.model.PlayerState;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.messages.PartyChatMessage;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class PartySyncManager
{
    private static final String PREFIX = "IPM|";

    private final PartyService partyService;
    private final AreaManager areaManager;
    private final HostAuthorityManager hostAuthorityManager;
    private final EventBus eventBus;

    private final GameSession gameSession = new GameSession();
    private final Map<String, PlayerState> playerStates = new HashMap<>();

    private boolean inParty;

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

        // IMPORTANT: PartyChanged may not fire immediately
        this.inParty = partyService != null && partyService.isInParty();
    }

    /* =========================
       Party state
       ========================= */

    /** Used by UI */
    public boolean isInParty()
    {
        return partyService != null && partyService.isInParty();
    }

    @Subscribe
    public void onPartyChanged(PartyChanged e)
    {
        inParty = e.getPartyId() != null;

        if (!inParty)
        {
            hostAuthorityManager.reset();
            playerStates.clear();
            gameSession.stop();
            postStatesUpdated();
        }
        else
        {
            hostAuthorityManager.clearIfHostLeftParty();
        }

        log.debug("PartyChanged: inParty={}", inParty);
    }

    private boolean allowHostSend()
    {
        return !isInParty() || hostAuthorityManager.isHost();
    }

    private void sendPartyString(String payload)
    {
        if (!isInParty())
        {
            return;
        }

        partyService.send(new PartyChatMessage(PREFIX + payload));
    }

    /* =========================
       Host sync
       ========================= */

    public void sendHostClaim(long memberId)
    {
        sendPartyString("HOST|" + memberId);
    }

    private void onHostClaimReceived(long memberId)
    {
        if (hostAuthorityManager.hasHost())
        {
            log.debug("Ignoring HOST claim from {} – host already set", memberId);
            return;
        }

        hostAuthorityManager.onHostClaim(memberId);
        log.info("Host claimed by memberId={}", memberId);
    }

    /* =========================
       Initialization
       ========================= */

    public void initializePlayersFromParty()
    {
        if (!allowHostSend())
        {
            return;
        }

        Collection<PartyMember> members = partyService.getMembers();
        if (members == null)
        {
            return;
        }

        for (PartyMember member : members)
        {
            if (member == null)
            {
                continue;
            }

            String name = member.getDisplayName();
            if (name == null || name.isEmpty())
            {
                continue;
            }

            playerStates.putIfAbsent(
                    name,
                    new PlayerState(name, InfectionState.HEALTHY)
            );
        }

        postStatesUpdated();
    }

    /* =========================
       Infection
       ========================= */

    public void sendInfectionState(String playerName, InfectionState state)
    {
        if (!allowHostSend())
        {
            return;
        }

        updateLocalState(playerName, state);
        sendPartyString("INFECT|" + playerName + "|" + state.name());
    }

    /* =========================
       Arena
       ========================= */

    public void sendArea()
    {
        if (!allowHostSend())
        {
            return;
        }

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
       Game start / stop
       ========================= */

    public void sendGameStart(int durationSeconds)
    {
        if (!allowHostSend())
        {
            return;
        }

        long start = System.currentTimeMillis();
        gameSession.start(start, durationSeconds);

        sendPartyString("TIMER|" + start + "|" + durationSeconds);
    }

    public void sendGameStop()
    {
        if (!allowHostSend())
        {
            return;
        }

        gameSession.stop();
        sendPartyString("STOP");
    }

    /* =========================
       Receive
       ========================= */

    @Subscribe
    public void onPartyChatMessage(PartyChatMessage msg)
    {
        String v = msg.getValue();
        if (v == null || !v.startsWith(PREFIX))
        {
            return;
        }

        String[] parts = v.substring(PREFIX.length()).split("\\|");
        if (parts.length == 0)
        {
            return;
        }

        switch (parts[0])
        {
            case "HOST":
                if (parts.length >= 2)
                {
                    Long id = tryParseLong(parts[1]);
                    if (id != null)
                    {
                        onHostClaimReceived(id);
                    }
                }
                break;

            case "INFECT":
                if (parts.length >= 3)
                {
                    InfectionState state = safeInfectionState(parts[2]);
                    if (state != null)
                    {
                        updateLocalState(parts[1], state);
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
                        eventBus.post(GameStartedFromParty.INSTANCE);
                    }
                }
                break;

            case "STOP":
                gameSession.stop();
                eventBus.post(GameStoppedFromParty.INSTANCE);
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
        playerStates.compute(playerName, (k, v) ->
        {
            if (v == null)
            {
                return new PlayerState(playerName, state);
            }
            v.setInfectionState(state);
            return v;
        });

        postStatesUpdated();
    }

    private void postStatesUpdated()
    {
        eventBus.post(PlayerStatesUpdated.INSTANCE);
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
        try { return Integer.parseInt(s); }
        catch (Exception e) { return null; }
    }

    private static Long tryParseLong(String s)
    {
        try { return Long.parseLong(s); }
        catch (Exception e) { return null; }
    }

    private static InfectionState safeInfectionState(String s)
    {
        try { return InfectionState.valueOf(s); }
        catch (Exception e) { return null; }
    }
}
