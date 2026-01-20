package com.sypherred.infectedpartymode.ui;

import com.sypherred.infectedpartymode.InfectedPartymodePlugin;
import com.sypherred.infectedpartymode.party.PartySyncManager;
import com.sypherred.infectedpartymode.model.PlayerState;
import com.sypherred.infectedpartymode.area.ArenaMode;

import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;

public class InfectedPanel extends PluginPanel
{
    private final InfectedPartymodePlugin plugin;
    private final PartySyncManager partySyncManager;

    private JButton startGame;
    private JButton rerollArena;
    private JButton stopGame;

    private final JPanel playerListPanel = new JPanel();

    @Inject
    public InfectedPanel(
            InfectedPartymodePlugin plugin,
            PartySyncManager partySyncManager
    )
    {
        this.plugin = plugin;
        this.partySyncManager = partySyncManager;

        setLayout(new BorderLayout(0, 8));

        add(buildControlPanel(), BorderLayout.NORTH);
        add(buildPlayerList(), BorderLayout.CENTER);

        refreshControls();
    }

    /* =========================
       Control Panel
       ========================= */

    private JPanel buildControlPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Game Control"));

        startGame = new JButton("▶ Start Game (10 min)");
        startGame.setAlignmentX(Component.CENTER_ALIGNMENT);
        startGame.setToolTipText("Start the game with the selected arena settings");
        startGame.addActionListener(e ->
        {
            plugin.startGame(600);
            refreshControls();
        });

        rerollArena = new JButton("🎲 Reroll Arena");
        rerollArena.setAlignmentX(Component.CENTER_ALIGNMENT);
        rerollArena.setToolTipText("Generate a new random arena (Random mode only)");
        rerollArena.addActionListener(e ->
        {
            plugin.rerollRandomArena();
            refreshControls();
        });

        stopGame = new JButton("■ Stop Game");
        stopGame.setAlignmentX(Component.CENTER_ALIGNMENT);
        stopGame.setToolTipText("Stop the running game");
        stopGame.addActionListener(e ->
        {
            plugin.stopGame();
            refreshControls();
        });

        panel.add(startGame);
        panel.add(Box.createVerticalStrut(6));
        panel.add(rerollArena);
        panel.add(Box.createVerticalStrut(6));
        panel.add(stopGame);

        return panel;
    }

    /* =========================
       Player List
       ========================= */

    private JScrollPane buildPlayerList()
    {
        playerListPanel.setLayout(new BoxLayout(playerListPanel, BoxLayout.Y_AXIS));
        playerListPanel.setBorder(BorderFactory.createTitledBorder("Players"));

        refreshPlayerList();

        JScrollPane scrollPane = new JScrollPane(playerListPanel);
        scrollPane.setBorder(null);
        return scrollPane;
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

    /* =========================
       State Reflection
       ========================= */

    public void refreshControls()
    {
        boolean running = plugin.isGameRunning();
        ArenaMode mode = plugin.getArenaMode();

        startGame.setEnabled(!running && mode != ArenaMode.NONE);
        rerollArena.setEnabled(!running && mode == ArenaMode.RANDOM);
        stopGame.setEnabled(running);
    }
}
