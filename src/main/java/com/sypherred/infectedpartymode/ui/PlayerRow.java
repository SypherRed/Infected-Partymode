package com.sypherred.infectedpartymode.ui;

import com.sypherred.infectedpartymode.model.InfectionState;
import com.sypherred.infectedpartymode.model.PlayerState;

import javax.swing.*;
import java.awt.*;

public class PlayerRow extends JPanel
{
    private final PlayerState state;

    public PlayerRow(PlayerState state)
    {
        this.state = state;

        setLayout(new BorderLayout(6, 0));
        setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

        JLabel nameLabel = new JLabel(state.getPlayerName());
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));

        JLabel statusLabel = new JLabel(state.getInfectionState().name());
        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        applyStateStyling(nameLabel, statusLabel);

        add(nameLabel, BorderLayout.WEST);
        add(statusLabel, BorderLayout.EAST);
    }

    private void applyStateStyling(JLabel name, JLabel status)
    {
        if (state.getInfectionState() == InfectionState.INFECTED)
        {
            name.setForeground(new Color(180, 50, 50));
            status.setForeground(new Color(180, 50, 50));
        }
        else
        {
            name.setForeground(new Color(50, 150, 50));
            status.setForeground(new Color(50, 150, 50));
        }
    }
}
