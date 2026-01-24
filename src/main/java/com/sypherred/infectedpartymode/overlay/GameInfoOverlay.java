package com.sypherred.infectedpartymode.overlay;

import com.sypherred.infectedpartymode.InfectedPartymodePlugin;
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
    private final InfectedPartymodePlugin plugin;
    private final PanelComponent panel = new PanelComponent();

    @Inject
    public GameInfoOverlay(InfectedPartymodePlugin plugin)
    {
        this.plugin = plugin;

        setLayer(OverlayLayer.ABOVE_SCENE);
        setPosition(OverlayPosition.TOP_LEFT);
        setPriority(Overlay.PRIORITY_MED);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        panel.getChildren().clear();

        panel.getChildren().add(
                LineComponent.builder()
                        .left("Infected Partymode")
                        .build()
        );

        panel.getChildren().add(
                LineComponent.builder()
                        .left("State:")
                        .right(plugin.isGameRunning() ? "RUNNING" : "IDLE")
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

        if (plugin.isGameRunning())
        {
            int remaining = plugin.getRemainingSeconds();
            int minutes = remaining / 60;
            int seconds = remaining % 60;

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
