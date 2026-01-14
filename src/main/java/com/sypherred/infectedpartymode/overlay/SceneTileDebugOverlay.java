package com.sypherred.infectedpartymode.overlay;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;

public class SceneTileDebugOverlay extends Overlay
{
    private static final int SCENE_SIZE = 104;

    private final Client client;

    @Inject
    public SceneTileDebugOverlay(Client client)
    {
        this.client = client;
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(PRIORITY_HIGH);
    }

    @Override
    public Dimension render(Graphics2D g)
    {
        WorldView worldView = client.getTopLevelWorldView();
        if (worldView == null)
        {
            return null;
        }

        for (int x = 0; x < SCENE_SIZE; x++)
        {
            for (int y = 0; y < SCENE_SIZE; y++)
            {
                LocalPoint lp = LocalPoint.fromScene(x, y, worldView);
                if (lp == null)
                {
                    continue;
                }

                Polygon poly = Perspective.getCanvasTilePoly(client, lp);
                if (poly == null)
                {
                    continue;
                }

                g.setColor(Color.GREEN);
                g.setStroke(new BasicStroke(1));
                g.draw(poly);
            }
        }

        return null;
    }
}
