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

    /**
     * Gate to allow accepting a remote host claim.
     * This is ONLY opened during an explicit local claim action.
     */
    private boolean allowRemoteClaim = false;

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
        allowRemoteClaim = false;
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
     * Call BEFORE sending a HOST claim to the party.
     * Opens a short-lived window to accept the matching remote claim.
     */
    public void beginClaim()
    {
        allowRemoteClaim = true;
    }

    /**
     * Call AFTER the claim attempt is finished.
     */
    public void endClaim()
    {
        allowRemoteClaim = false;
    }

    /**
     * Claim host authority for the local player.
     * Should only be called by explicit user action (UI).
     */
    public boolean claimHost()
    {
        if (partyService == null || !partyService.isInParty())
        {
            return false;
        }

        if (hostMemberId != null)
        {
            return false; // ❗ already claimed
        }

        PartyMember local = partyService.getLocalMember();
        if (local == null)
        {
            return false;
        }

        hostMemberId = local.getMemberId();
        return true;
    }


    /**
     * Accept a host claim from the party.
     * ONLY accepted if a local claim is currently in progress.
     */
    public void onHostClaim(long memberId)
    {
        if (!allowRemoteClaim || hostMemberId != null)
        {
            return;
        }

        hostMemberId = memberId;
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
