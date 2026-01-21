package com.sypherred.infectedpartymode.ui;

import com.sypherred.infectedpartymode.InfectedPartymodePlugin;
import com.sypherred.infectedpartymode.party.PartySyncManager;
import com.sypherred.infectedpartymode.party.HostAuthorityManager;
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
    private final HostAuthorityManager hostAuthorityManager;

    private JButton startGame;
    private JButton rerollArena;
    private JButton stopGame;

    private JLabel gameStatusLabel;
    private JLabel hostLabel;
    private JLabel timerLabel;

    private final JPanel playerListPanel = new JPanel();

    @Inject
    public InfectedPanel(
            InfectedPartymodePlugin plugin,
            PartySyncManager partySyncManager,
            HostAuthorityManager hostAuthorityManager
    )
    {
        this.plugin = plugin;
        this.partySyncManager = partySyncManager;
        this.hostAuthorityManager = hostAuthorityManager;

        setLayout(new BorderLayout(0, 8));

        add(buildStatusPanel(), BorderLayout.NORTH);
        add(buildControlPanel(), BorderLayout.CENTER);
        add(buildPlayerList(), BorderLayout.SOUTH);

        refreshControls();
    }

    private JPanel buildStatusPanel()
    {
        JPanel panel = new JPanel(new GridLayout(3, 1, 0, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Game Status"));

        gameStatusLabel = new JLabel();
        hostLabel = new JLabel();
        timerLabel = new JLabel();

        panel.add(gameStatusLabel);
        panel.add(hostLabel);
        panel.add(timerLabel);

        return panel;
    }

    private JPanel buildControlPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Game Control"));

        startGame = new JButton("▶ Start Game (10 min)");
        startGame.setAlignmentX(Component.CENTER_ALIGNMENT);
        startGame.addActionListener(e ->
        {
            plugin.startGame(600);
            refreshControls();
        });

        rerollArena = new JButton("🎲 Reroll Arena");
        rerollArena.setAlignmentX(Component.CENTER_ALIGNMENT);
        rerollArena.addActionListener(e ->
        {
            plugin.rerollRandomArena();
            refreshControls();
        });

        stopGame = new JButton("■ Stop Game");
        stopGame.setAlignmentX(Component.CENTER_ALIGNMENT);
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

    public void refreshControls()
    {
        boolean running = plugin.isGameRunning();
        ArenaMode mode = plugin.getArenaMode();

        startGame.setEnabled(!running && mode != ArenaMode.NONE && hostAuthorityManager.isHost());
        rerollArena.setEnabled(!running && mode == ArenaMode.RANDOM && hostAuthorityManager.isHost());
        stopGame.setEnabled(running && hostAuthorityManager.isHost());

        if (running)
        {
            gameStatusLabel.setText("Status: RUNNING");
            gameStatusLabel.setForeground(Color.GREEN.darker());
        }
        else if (mode != ArenaMode.NONE)
        {
            gameStatusLabel.setText("Status: PREVIEW");
            gameStatusLabel.setForeground(Color.ORANGE.darker());
        }
        else
        {
            gameStatusLabel.setText("Status: STOPPED");
            gameStatusLabel.setForeground(Color.GRAY);
        }

        hostLabel.setText(
                hostAuthorityManager.isHost()
                        ? "Role: HOST 👑"
                        : "Role: PLAYER"
        );

        int seconds = plugin.getRemainingSeconds();
        timerLabel.setText(
                seconds > 0
                        ? "Time Left: " + formatTime(seconds)
                        : "Time Left: --:--"
        );
    }

    private String formatTime(int seconds)
    {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}
