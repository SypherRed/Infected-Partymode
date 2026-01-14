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

import javax.inject.Inject;
import java.awt.*;

public class ArenaBorderOverlay extends Overlay
{
    private static final Color BORDER_COLOR =
            new Color(255, 0, 0, 180);

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
        if (area == null)
        {
            return null;
        }

        int plane = client.getLocalPlayer().getWorldLocation().getPlane();

        int baseChunkX = area.getBaseChunkX();
        int baseChunkY = area.getBaseChunkY();

        // Single chunk (1x1)
        drawChunkBorder(graphics, baseChunkX, baseChunkY, plane);

        return null;
    }

    private void drawChunkBorder(Graphics2D graphics, int chunkX, int chunkY, int plane)
    {
        int startX = chunkX * 8;
        int startY = chunkY * 8;

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

        graphics.setColor(BORDER_COLOR);
        graphics.draw(poly);
    }
}
