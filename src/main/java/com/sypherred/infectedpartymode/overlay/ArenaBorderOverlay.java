package com.sypherred.infectedpartymode.overlay;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.*;

public class ArenaBorderOverlay extends Overlay
{
    private static final Color DEBUG_FILL =
            new Color(0, 255, 0, 100);
    private static final Color DEBUG_OUTLINE =
            new Color(0, 255, 0, 220);

    private final Client client;

    @Inject
    public ArenaBorderOverlay(Client client)
    {
        this.client = client;

        setLayer(OverlayLayer.ABOVE_SCENE);
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(PRIORITY_HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (client.getLocalPlayer() == null)
        {
            return null;
        }

        LocalPoint lp = client.getLocalPlayer().getLocalLocation();
        if (lp == null)
        {
            return null;
        }

        Polygon poly = Perspective.getCanvasTilePoly(client, lp);
        if (poly == null)
        {
            return null;
        }

        OverlayUtil.renderPolygon(
                graphics,
                poly,
                DEBUG_OUTLINE,
                DEBUG_FILL,
                new BasicStroke(2)
        );

        return null;
    }
}
