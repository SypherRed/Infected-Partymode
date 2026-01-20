package com.sypherred.infectedpartymode.overlay;

import com.sypherred.infectedpartymode.InfectedPartymodePlugin;
import com.sypherred.infectedpartymode.party.PartySyncManager;
import com.sypherred.infectedpartymode.model.GameSession;
import com.sypherred.infectedpartymode.area.ArenaMode;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.LineComponent;

import javax.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;

public class GameInfoOverlay extends Overlay
{
    private final PartySyncManager partySyncManager;
    private final InfectedPartymodePlugin plugin;
    private final PanelComponent panel = new PanelComponent();

    @Inject
    public GameInfoOverlay(
            PartySyncManager partySyncManager,
            InfectedPartymodePlugin plugin
    )
    {
        this.partySyncManager = partySyncManager;
        this.plugin = plugin;

        setLayer(OverlayLayer.ABOVE_SCENE);
        setPosition(OverlayPosition.TOP_LEFT);
        setPriority(Overlay.PRIORITY_MED);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        panel.getChildren().clear();

        GameSession session = partySyncManager.getGameSession();

        panel.getChildren().add(
                LineComponent.builder()
                        .left("Infected Partymode")
                        .build()
        );

        panel.getChildren().add(
                LineComponent.builder()
                        .left("State:")
                        .right(session != null && session.isRunning() ? "RUNNING" : "IDLE")
                        .build()
        );

        ArenaMode mode = plugin.getArenaMode();
        panel.getChildren().add(
                LineComponent.builder()
                        .left("Mode:")
                        .right(mode.name())
                        .build()
        );

        panel.getChildren().add(
                LineComponent.builder()
                        .left("Regions:")
                        .right(String.valueOf(plugin.getActiveRegionCount()))
                        .build()
        );

        panel.getChildren().add(
                LineComponent.builder()
                        .left("Host:")
                        .right(plugin.isHost() ? "You" : "Other")
                        .build()
        );

        // Remaining time nur anzeigen, wenn Spiel läuft
        if (session != null && session.isRunning())
        {
            long remainingMillis = session.getRemainingMillis();
            long seconds = remainingMillis / 1000;
            long minutes = seconds / 60;
            seconds %= 60;

            panel.getChildren().add(
                    LineComponent.builder()
                            .left("Remaining:")
                            .right(String.format("%02d:%02d", minutes, seconds))
                            .build()
            );
        }

        return panel.render(graphics);
    }
}
