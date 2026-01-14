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
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DEBUG Overlay:
 * - Always draws a green debug tile under the player
 * - Draws a FILLED 8x8 chunk arena (transparent)
 * - Draws a red border around the chunk
 * - Logs when player leaves / re-enters the arena
 */
public class ArenaBorderOverlay extends Overlay
{
    private static final Logger log =
            LoggerFactory.getLogger(ArenaBorderOverlay.class);

    private static final Color CHUNK_FILL =
            new Color(255, 0, 0, 25);   // very transparent red
    private static final Color BORDER_OUTLINE =
            new Color(255, 0, 0, 220);

    private static final Color DEBUG_TILE_OUTLINE =
            new Color(0, 255, 0, 220);
    private static final Color DEBUG_TILE_FILL =
            new Color(0, 255, 0, 100);

    private final Client client;
    private final AreaManager areaManager;

    private boolean wasInsideArena = true;

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
        Player local = client.getLocalPlayer();
        if (local == null)
        {
            return null;
        }

        WorldPoint playerWp = local.getWorldLocation();

        /* =========================
           DEBUG TILE under player (always)
           ========================= */
        drawTile(graphics, playerWp, DEBUG_TILE_OUTLINE, DEBUG_TILE_FILL);

        ChunkArea area = areaManager.getActiveArea();
        if (area == null)
        {
            return null;
        }

        int playerChunkX = playerWp.getX() >> 3;
        int playerChunkY = playerWp.getY() >> 3;

        boolean isInside = area.containsChunk(playerChunkX, playerChunkY);

        if (isInside != wasInsideArena)
        {
            if (!isInside)
            {
                log.warn("DEBUG: Player LEFT arena chunk");
            }
            else
            {
                log.info("DEBUG: Player ENTERED arena chunk");
            }
            wasInsideArena = isInside;
        }

        int plane = playerWp.getPlane();

        int baseChunkX = area.getBaseChunkX();
        int baseChunkY = area.getBaseChunkY();

        int startX = baseChunkX * 8;
        int startY = baseChunkY * 8;

        /* =========================
           Draw FULL chunk (8x8)
           ========================= */
        for (int dx = 0; dx < 8; dx++)
        {
            for (int dy = 0; dy < 8; dy++)
            {
                WorldPoint wp = new WorldPoint(
                        startX + dx,
                        startY + dy,
                        plane
                );

                drawTile(graphics, wp, BORDER_OUTLINE, CHUNK_FILL);
            }
        }

        return null;
    }

    private void drawTile(
            Graphics2D graphics,
            WorldPoint worldPoint,
            Color outline,
            Color fill
    )
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
                outline,
                fill,
                new BasicStroke(1.5f)
        );
    }
}
