package com.sypherred.infectedpartymode.overlay;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;

public class ArenaDebugOverlay extends Overlay
{
    private final Client client;

    @Inject
    public ArenaDebugOverlay(Client client)
    {
        this.client = client;

        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(PRIORITY_HIGHEST);
    }

    @Override
    public Dimension render(Graphics2D g)
    {
        int w = client.getCanvasWidth();
        int h = client.getCanvasHeight();

        // FETTER RAHMEN
        g.setColor(Color.RED);
        g.setStroke(new BasicStroke(6));
        g.drawRect(20, 20, w - 40, h - 40);

        // FETTER TEXT
        g.setFont(new Font("Arial", Font.BOLD, 32));
        g.drawString("DEBUG OVERLAY ACTIVE", 40, 80);

        return null;
    }
}
