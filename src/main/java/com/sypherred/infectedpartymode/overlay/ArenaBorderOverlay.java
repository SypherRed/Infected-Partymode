package com.sypherred.infectedpartymode.overlay;

import com.sypherred.infectedpartymode.area.AreaManager;
import com.sypherred.infectedpartymode.area.ChunkArea;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.*;

public class ArenaBorderOverlay extends Overlay
{
    private static final Color BORDER_OUTLINE =
            new Color(255, 0, 0, 220);
    private static final Color BORDER_FILL =
            new Color(255, 0, 0, 40);

    private final Client client;
    private final AreaManager areaManager;

    @Inject
    public ArenaBorderOverlay(Client client, AreaManager areaManager)
    {
        this.client = client;
        this.areaManager = areaManager;

        setLayer(OverlayLayer.ABOVE_SCENE);
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(PRIORITY_HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        ChunkArea area = areaManager.getActiveArea();
        if (area == null || client.getLocalPlayer() == null)
        {
            return null;
        }

        int plane = client.getLocalPlayer()
                .getWorldLocation()
                .getPlane();

        int baseChunkX = area.getBaseChunkX();
        int baseChunkY = area.getBaseChunkY();

        int startX = baseChunkX * 8;
        int startY = baseChunkY * 8;

        for (int dx = 0; dx < 8; dx++)
        {
            for (int dy = 0; dy < 8; dy++)
            {
                boolean isBorder =
                        dx == 0 || dx == 7 ||
                                dy == 0 || dy == 7;

                if (!isBorder)
                {
                    continue;
                }

                WorldPoint wp = new WorldPoint(
                        startX + dx,
                        startY + dy,
                        plane
                );

                drawTile(graphics, wp);
            }
        }

        return null;
    }

    private void drawTile(Graphics2D graphics, WorldPoint worldPoint)
    {
        LocalPoint lp = LocalPoint.fromWorld(client, worldPoint);
        if (lp == null)
        {
            return;
        }

        Polygon poly = Perspective.getCanvasTilePoly(client, lp);
        if (poly == null)
        {
            return;
        }

        OverlayUtil.renderPolygon(
                graphics,
                poly,
                BORDER_OUTLINE,
                BORDER_FILL,
                new BasicStroke(2)
        );
    }
}
