package com.sypherred.infectedpartymode.party;

import com.sypherred.infectedpartymode.model.InfectionState;
import net.runelite.client.party.messages.PartyMemberMessage;

public class InfectionSyncMessage extends PartyMemberMessage
{
    private String playerName;
    private InfectionState infectionState;

    public InfectionSyncMessage()
    {
        // required for deserialization
    }

    public InfectionSyncMessage(String playerName, InfectionState infectionState)
    {
        this.playerName = playerName;
        this.infectionState = infectionState;
    }

    public String getPlayerName()
    {
        return playerName;
    }

    public InfectionState getInfectionState()
    {
        return infectionState;
    }
}
