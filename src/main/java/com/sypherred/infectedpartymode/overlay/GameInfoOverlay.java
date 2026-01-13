package com.sypherred.infectedpartymode.overlay;

import com.sypherred.infectedpartymode.party.PartySyncManager;
import com.sypherred.infectedpartymode.model.GameSession;

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
    private final PanelComponent panel = new PanelComponent();

    @Inject
    public GameInfoOverlay(PartySyncManager partySyncManager)
    {
        this.partySyncManager = partySyncManager;

        setLayer(OverlayLayer.ABOVE_SCENE);
        setPosition(OverlayPosition.TOP_LEFT);
        setPriority(Overlay.PRIORITY_MED);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        GameSession session = partySyncManager.getGameSession();
        if (session == null || !session.isRunning())
        {
            return null;
        }

        panel.getChildren().clear();

        long remainingMillis = session.getRemainingMillis();
        long seconds = remainingMillis / 1000;
        long minutes = seconds / 60;
        seconds %= 60;

        panel.getChildren().add(
                LineComponent.builder()
                        .left("Remaining Time")
                        .right(String.format("%02d:%02d", minutes, seconds))
                        .build()
        );

        return panel.render(graphics);
    }
}
