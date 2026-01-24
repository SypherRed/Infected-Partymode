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

    /**
     * Single source of truth for host authority.
     * - null  -> no host claimed yet
     * - value -> memberId of current host
     */
    private Long hostMemberId = null;

    @Inject
    public HostAuthorityManager(PartyService partyService)
    {
        this.partyService = partyService;
    }

    /* =========================
       Basic state
       ========================= */

    public void reset()
    {
        hostMemberId = null;
    }

    public boolean hasHost()
    {
        return hostMemberId != null;
    }

    public Long getHostMemberId()
    {
        return hostMemberId;
    }

    /* =========================
       Authority checks
       ========================= */

    /**
     * Determines whether the local player is the current host.
     *
     * Rules:
     * - Solo / not in party -> always host
     * - Party & no host set -> no one is host
     * - Party & host set   -> only matching memberId is host
     */
    public boolean isHost()
    {
        if (partyService == null || !partyService.isInParty())
        {
            return true;
        }

        if (hostMemberId == null)
        {
            return false;
        }

        PartyMember local = partyService.getLocalMember();
        return local != null && local.getMemberId() == hostMemberId;
    }

    /* =========================
       Host lifecycle
       ========================= */

    /**
     * Claim host authority for the local player.
     * Should only be called by explicit user action (UI).
     */
    public void claimHost()
    {
        if (partyService == null || !partyService.isInParty())
        {
            return;
        }

        PartyMember local = partyService.getLocalMember();
        if (local == null)
        {
            return;
        }

        // First claim wins
        if (hostMemberId == null)
        {
            hostMemberId = local.getMemberId();
        }
    }

    /**
     * Accept a host claim from the party.
     * Used for PARTY sync.
     */
    public void onHostClaim(long memberId)
    {
        if (hostMemberId == null)
        {
            hostMemberId = memberId;
        }
    }

    /**
     * Transfer host authority to another party member.
     * Should only be invoked by the current host.
     */
    public void transferHost(long targetMemberId)
    {
        hostMemberId = targetMemberId;
    }

    /**
     * Clears host authority if the current host
     * is no longer part of the party.
     *
     * Call this on PartyChanged.
     */
    public void clearIfHostLeftParty()
    {
        if (hostMemberId == null)
        {
            return;
        }

        if (partyService == null || !partyService.isInParty())
        {
            hostMemberId = null;
            return;
        }

        Collection<PartyMember> members = partyService.getMembers();
        if (members == null)
        {
            return;
        }

        boolean hostStillPresent = members.stream()
                .anyMatch(m -> m.getMemberId() == hostMemberId);

        if (!hostStillPresent)
        {
            hostMemberId = null;
        }
    }
}
