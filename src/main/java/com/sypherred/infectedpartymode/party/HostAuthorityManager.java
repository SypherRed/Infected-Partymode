package com.sypherred.infectedpartymode.party;

import com.sypherred.infectedpartymode.party.messages.HostClaimMessage;
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

    public boolean isHost()
    {
        // Solo debug: not in party => always host
        if (partyService == null || !partyService.isInParty())
        {
            return true;
        }

        final PartyMember local = partyService.getLocalMember();
        if (local == null)
        {
            return false;
        }

        // Until someone claims host, nobody is host (prevents two people starting at once)
        if (hostMemberId == null)
        {
            return false;
        }

        return local.getMemberId() == hostMemberId;
    }

    /**
     * Claim host authority. In solo this becomes host immediately.
     * In party this sets hostMemberId to local and broadcasts HostClaimMessage.
     */
    public void claimHost()
    {
        if (partyService == null || !partyService.isInParty())
        {
            hostMemberId = -1L; // solo marker (not used in comparison)
            return;
        }

        final PartyMember local = partyService.getLocalMember();
        if (local == null)
        {
            return;
        }

        hostMemberId = local.getMemberId();

        final HostClaimMessage msg = new HostClaimMessage();
        msg.setMemberId(hostMemberId);
        partyService.send(msg);
    }

    /**
     * Called when a HostClaimMessage is received from the party.
     * First claim wins (for now).
     */
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
