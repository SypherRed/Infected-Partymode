package com.sypherred.infectedpartymode.overlay;

import com.sypherred.infectedpartymode.area.AreaManager;
import com.sypherred.infectedpartymode.area.ChunkArea;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DEBUG VERSION:
 * Renders ALL tiles of the active arena as filled 1x1 tiles.
 */
public class ArenaBorderOverlay extends Overlay
{
    private static final Logger log =
            LoggerFactory.getLogger(ArenaBorderOverlay.class);

    private static final Color TILE_FILL = new Color(255, 0, 0, 90);
    private static final Color TILE_OUTLINE = new Color(255, 0, 0, 180);

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
        Player local = client.getLocalPlayer();

        if (area == null || local == null)
        {
            return null;
        }

        int plane = local.getWorldLocation().getPlane();

        log.debug("Rendering FULL arena tiles on plane {}", plane);

        int startChunkX = area.getBaseChunkX();
        int startChunkY = area.getBaseChunkY();
        int endChunkX = startChunkX + area.getWidthChunks() - 1;
        int endChunkY = startChunkY + area.getHeightChunks() - 1;

        for (int cx = startChunkX; cx <= endChunkX; cx++)
        {
            for (int cy = startChunkY; cy <= endChunkY; cy++)
            {
                renderFullChunk(graphics, cx, cy, plane);
            }
        }

        return null;
    }

    private void renderFullChunk(Graphics2D graphics, int chunkX, int chunkY, int plane)
    {
        int tileStartX = chunkX * 8;
        int tileStartY = chunkY * 8;

        for (int dx = 0; dx < 8; dx++)
        {
            for (int dy = 0; dy < 8; dy++)
            {
                WorldPoint wp = new WorldPoint(
                        tileStartX + dx,
                        tileStartY + dy,
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

        graphics.setColor(TILE_FILL);
        graphics.fill(poly);

        graphics.setColor(TILE_OUTLINE);
        graphics.draw(poly);
    }
}
