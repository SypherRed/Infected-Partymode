package com.sypherred.infectedpartymode.party;

import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class HostAuthorityManager
{
    private final PartyService partyService;

    private Long hostMemberId = null;

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
     * Host rules:
     * - Solo: always host
     * - Party & no host yet: allow local player to start (first-claim wins)
     * - Party & host set: only host allowed
     */
    public boolean isHost()
    {
        // Solo / not in party -> always host (debug-friendly)
        if (partyService == null || !partyService.isInParty())
        {
            return true;
        }

        // No host yet -> allow first starter
        if (hostMemberId == null)
        {
            return true;
        }

        final PartyMember local = partyService.getLocalMember();
        if (local == null)
        {
            return false;
        }

        return local.getMemberId() == hostMemberId;
    }

    public void claimHost()
    {
        if (partyService == null || !partyService.isInParty())
        {
            return;
        }

        final PartyMember local = partyService.getLocalMember();
        if (local == null)
        {
            return;
        }

        hostMemberId = local.getMemberId();
    }

    public void onHostClaim(long memberId)
    {
        if (hostMemberId == null)
        {
            hostMemberId = memberId;
        }
    }

    public Long getHostMemberId()
    {
        return hostMemberId;
    }
}
