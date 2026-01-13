package com.sypherred.infectedpartymode.ui;

import com.sypherred.infectedpartymode.InfectedPartymodePlugin;
import com.sypherred.infectedpartymode.party.PartySyncManager;
import com.sypherred.infectedpartymode.model.PlayerState;

import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfectedPanel extends PluginPanel
{
    private static final Logger log = LoggerFactory.getLogger(InfectedPanel.class);

    private final InfectedPartymodePlugin plugin;
    private final PartySyncManager partySyncManager;

    private final JPanel playerListPanel = new JPanel();

    @Inject
    public InfectedPanel(
            InfectedPartymodePlugin plugin,
            PartySyncManager partySyncManager
    )
    {
        this.plugin = plugin;
        this.partySyncManager = partySyncManager;

        setLayout(new BorderLayout());

        add(buildControlPanel(), BorderLayout.NORTH);
        add(buildPlayerList(), BorderLayout.CENTER);
    }

    private JPanel buildControlPanel()
    {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));

        JButton startGame = new JButton("Start Game (10 min)");
        startGame.addActionListener(e ->
        {
            log.info("Start Game button clicked");
            plugin.startGame(600);
        });

        JButton stopGame = new JButton("Stop Game");
        stopGame.addActionListener(e ->
        {
            log.info("Stop Game button clicked");
            plugin.stopGame();
        });

        panel.add(startGame);
        panel.add(stopGame);

        return panel;
    }

    private JScrollPane buildPlayerList()
    {
        playerListPanel.setLayout(new BoxLayout(playerListPanel, BoxLayout.Y_AXIS));
        refreshPlayerList();
        return new JScrollPane(playerListPanel);
    }

    public void refreshPlayerList()
    {
        playerListPanel.removeAll();

        for (PlayerState state : partySyncManager.getPlayerStates().values())
        {
            playerListPanel.add(new PlayerRow(state, partySyncManager));
        }

        playerListPanel.revalidate();
        playerListPanel.repaint();
    }
}
