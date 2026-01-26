package com.sypherred.infectedpartymode.ui;

import com.sypherred.infectedpartymode.InfectedPartymodePlugin;
import com.sypherred.infectedpartymode.area.ArenaMode;
import com.sypherred.infectedpartymode.model.InfectionState;
import com.sypherred.infectedpartymode.model.PlayerState;
import com.sypherred.infectedpartymode.party.HostAuthorityManager;
import com.sypherred.infectedpartymode.party.PartySyncManager;
import com.sypherred.infectedpartymode.party.PlayerStatesUpdated;

import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;

public class InfectedPanel extends PluginPanel
{
    private final InfectedPartymodePlugin plugin;
    private final PartySyncManager partySyncManager;
    private final HostAuthorityManager hostAuthorityManager;
    private final EventBus eventBus;
    private final PartyService partyService;

    private JButton claimHost;
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
            HostAuthorityManager hostAuthorityManager,
            EventBus eventBus,
            PartyService partyService
    )
    {
        this.plugin = plugin;
        this.partySyncManager = partySyncManager;
        this.hostAuthorityManager = hostAuthorityManager;
        this.eventBus = eventBus;
        this.partyService = partyService;

        setLayout(new BorderLayout(0, 8));

        add(buildStatusPanel(), BorderLayout.NORTH);
        add(buildControlPanel(), BorderLayout.CENTER);
        add(buildPlayerList(), BorderLayout.SOUTH);

        eventBus.register(this);

        refreshControls();
    }

    /* =========================
       Event handling
       ========================= */

    @Subscribe
    public void onPlayerStatesUpdated(PlayerStatesUpdated e)
    {
        refreshPlayerList();
    }

    @Subscribe
    public void onPartyChanged(PartyChanged e)
    {
        refreshControls();
    }

    /* =========================
       UI builders
       ========================= */

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

        claimHost = new JButton("👑 Claim Host");
        claimHost.setAlignmentX(Component.CENTER_ALIGNMENT);
        claimHost.addActionListener(e ->
        {
            hostAuthorityManager.claimHost();

            PartyMember local = partyService != null ? partyService.getLocalMember() : null;
            if (local != null && hostAuthorityManager.hasHost())
            {
                partySyncManager.sendHostClaim(local.getMemberId());
            }

            refreshControls();
        });

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

        panel.add(claimHost);
        panel.add(Box.createVerticalStrut(6));
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

    /* =========================
       Player list
       ========================= */

    public void refreshPlayerList()
    {
        playerListPanel.removeAll();

        JPanel healthyPanel = new JPanel();
        healthyPanel.setLayout(new BoxLayout(healthyPanel, BoxLayout.Y_AXIS));
        healthyPanel.setBorder(BorderFactory.createTitledBorder("Healthy Players"));

        JPanel infectedPanel = new JPanel();
        infectedPanel.setLayout(new BoxLayout(infectedPanel, BoxLayout.Y_AXIS));
        infectedPanel.setBorder(BorderFactory.createTitledBorder("Infected Players"));

        for (PlayerState state : partySyncManager.getPlayerStates().values())
        {
            PlayerRow row = new PlayerRow(state);

            if (state.getInfectionState() == InfectionState.INFECTED)
            {
                infectedPanel.add(row);
            }
            else
            {
                healthyPanel.add(row);
            }
        }

        playerListPanel.add(healthyPanel);
        playerListPanel.add(Box.createVerticalStrut(6));
        playerListPanel.add(infectedPanel);

        playerListPanel.revalidate();
        playerListPanel.repaint();
    }

    /* =========================
       Control / status
       ========================= */

    public void refreshControls()
    {
        boolean running = plugin.isGameRunning();
        ArenaMode mode = plugin.getArenaMode();

        boolean inPartyNow = partyService != null && partyService.isInParty();
        boolean isHost = hostAuthorityManager.isHost();
        boolean hasHost = hostAuthorityManager.hasHost();

        // Claim visible ONLY when we are in a party and no host is set yet
        claimHost.setVisible(inPartyNow && !hasHost);

        startGame.setEnabled(!running && mode != ArenaMode.NONE && isHost);
        rerollArena.setEnabled(!running && mode == ArenaMode.RANDOM && isHost);
        stopGame.setEnabled(running && isHost);

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

        hostLabel.setText(isHost ? "Role: HOST 👑" : "Role: PLAYER");

        int seconds = plugin.getRemainingSeconds();
        timerLabel.setText(
                seconds > 0
                        ? "Time Left: " + formatTime(seconds)
                        : "Time Left: --:--"
        );

        revalidate();
        repaint();
    }

    private String formatTime(int seconds)
    {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}
