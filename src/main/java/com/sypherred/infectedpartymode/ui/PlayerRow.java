package com.sypherred.infectedpartymode.ui;

import com.sypherred.infectedpartymode.model.InfectionState;
import com.sypherred.infectedpartymode.model.PlayerState;
import com.sypherred.infectedpartymode.party.PartySyncManager;

import javax.swing.*;
import java.awt.*;

public class PlayerRow extends JPanel
{
    public PlayerRow(PlayerState state, PartySyncManager sync)
    {
        setLayout(new BorderLayout());

        JLabel name = new JLabel(state.getPlayerName());
        JLabel status = new JLabel(state.getInfectionState().name());

        JButton toggle = new JButton("Toggle");
        toggle.addActionListener(e ->
        {
            InfectionState newState =
                    state.getInfectionState() == InfectionState.HEALTHY
                            ? InfectionState.INFECTED
                            : InfectionState.HEALTHY;

            sync.sendInfectionState(state.getPlayerName(), newState);
        });

        add(name, BorderLayout.WEST);
        add(status, BorderLayout.CENTER);
        add(toggle, BorderLayout.EAST);
    }
}
