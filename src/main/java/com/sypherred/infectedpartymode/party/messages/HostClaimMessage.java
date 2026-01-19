package com.sypherred.infectedpartymode.party.messages;

import net.runelite.client.party.messages.PartyMemberMessage;

/**
 * Sent when a member claims host authority for this game session.
 * The host is identified by PartyMemberMessage#getMemberId().
 */
public class HostClaimMessage extends PartyMemberMessage
{
    public HostClaimMessage()
    {
        // required no-args constructor
    }
}
