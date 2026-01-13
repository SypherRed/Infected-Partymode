package com.sypherred.infectedpartymode.ui;

import com.sypherred.infectedpartymode.party.PartySyncManager;
import com.sypherred.infectedpartymode.area.AreaManager;
import com.sypherred.infectedpartymode.model.InfectionState;
import com.sypherred.infectedpartymode.model.PlayerState;

import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfectedPanel extends PluginPanel
{
    private final PartySyncManager partySyncManager;
    private final AreaManager areaManager;

    private final JPanel playerListPanel = new JPanel();

    private static final Logger log = LoggerFactory.getLogger(InfectedPanel.class);

    @Inject
    public InfectedPanel(PartySyncManager partySyncManager, AreaManager areaManager)
    {
        this.partySyncManager = partySyncManager;
        this.areaManager = areaManager;

        setLayout(new BorderLayout());

        add(buildControlPanel(), BorderLayout.NORTH);
        add(buildPlayerList(), BorderLayout.CENTER);
    }

    private JPanel buildControlPanel()
    {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));

        JButton generateArea = new JButton("Generate Arena");
        generateArea.addActionListener(e ->
        {
            log.info("Generate Arena button clicked");
            areaManager.generateRandomArea(2);
        });

        JButton startGame = new JButton("Start Game (10 min)");
        startGame.addActionListener(e ->
        {
            log.info("Start Game button clicked");
            partySyncManager.sendGameStart(600);
        });

        panel.add(generateArea);
        panel.add(startGame);

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
