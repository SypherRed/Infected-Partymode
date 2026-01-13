package com.sypherred.infectedpartymode.model;

public class PlayerState
{
    private String playerName;
    private InfectionState infectionState;

    public PlayerState(String playerName, InfectionState infectionState)
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

    public void setInfectionState(InfectionState infectionState)
    {
        this.infectionState = infectionState;
    }
}
