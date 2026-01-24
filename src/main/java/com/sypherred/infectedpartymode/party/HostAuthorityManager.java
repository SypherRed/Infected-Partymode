package com.sypherred.infectedpartymode.party;

import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;

@Singleton
public class HostAuthorityManager
{
    private final PartyService partyService;
    private Long hostMemberId;

    @Inject
    public HostAuthorityManager(PartyService partyService)
    {
        this.partyService = partyService;
    }

    public void reset()
    {
        hostMemberId = null;
    }

    /**
     * Deterministic host:
     * - Solo / not in party -> host
     * - Party -> member with lowest memberId
     */
    public boolean isHost()
    {
        if (partyService == null || !partyService.isInParty())
        {
            return true;
        }

        ensureHostResolved();

        PartyMember local = partyService.getLocalMember();
        return local != null && local.getMemberId() == hostMemberId;
    }

    public Long getHostMemberId()
    {
        ensureHostResolved();
        return hostMemberId;
    }

    private void ensureHostResolved()
    {
        if (hostMemberId != null)
        {
            return;
        }

        Collection<PartyMember> members = partyService.getMembers();
        if (members == null || members.isEmpty())
        {
            hostMemberId = null;
            return;
        }

        hostMemberId = members.stream()
                .map(PartyMember::getMemberId)
                .min(Long::compareTo)
                .orElse(null);
    }
}
