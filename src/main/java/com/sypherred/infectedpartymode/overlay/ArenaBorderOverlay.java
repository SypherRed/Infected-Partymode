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

/**
 * Renders the borders of the active chunk arena.
 */
public class ArenaBorderOverlay extends Overlay
{
    private static final Color BORDER_COLOR = new Color(255, 50, 50, 180);

    private final Client client;
    private final AreaManager areaManager;

    @Inject
    public ArenaBorderOverlay(Client client, AreaManager areaManager)
    {
        this.client = client;
        this.areaManager = areaManager;

        setLayer(OverlayLayer.ABOVE_SCENE);
        setPosition(OverlayPosition.DYNAMIC);

        setPriority(Overlay.PRIORITY_HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        ChunkArea area = areaManager.getActiveArea();
        if (area == null)
        {
            return null;
        }

        int startChunkX = area.getBaseChunkX();
        int startChunkY = area.getBaseChunkY();
        int endChunkX = startChunkX + area.getWidthChunks() - 1;
        int endChunkY = startChunkY + area.getHeightChunks() - 1;

        // Iterate over border chunks only
        for (int cx = startChunkX; cx <= endChunkX; cx++)
        {
            for (int cy = startChunkY; cy <= endChunkY; cy++)
            {
                boolean isBorder =
                        cx == startChunkX || cx == endChunkX ||
                                cy == startChunkY || cy == endChunkY;

                if (!isBorder)
                {
                    continue;
                }

                renderChunkBorder(graphics, cx, cy);
            }
        }
        return null;
    }

    private void renderChunkBorder(Graphics2D graphics, int chunkX, int chunkY)
    {
        int tileStartX = chunkX * 8;
        int tileStartY = chunkY * 8;

        // plane() via WorldView (client.getPlane is deprecated)
        int plane = client.getTopLevelWorldView().getPlane();

        // Render only outer tiles of the chunk
        for (int dx = 0; dx < 8; dx++)
        {
            for (int dy = 0; dy < 8; dy++)
            {
                boolean tileBorder =
                        dx == 0 || dx == 7 ||
                                dy == 0 || dy == 7;

                if (!tileBorder)
                {
                    continue;
                }

                WorldPoint wp = new WorldPoint(tileStartX + dx, tileStartY + dy, plane);
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
