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
 * - Draws chunk borders (8x8)
 * - Draws a moving debug tile under the player
 * - Logs when player leaves / re-enters the arena chunk
 */
public class ArenaBorderOverlay extends Overlay
{
    private static final Logger log =
            LoggerFactory.getLogger(ArenaBorderOverlay.class);

    private static final Color BORDER_OUTLINE = new Color(255, 0, 0, 220);
    private static final Color BORDER_FILL = new Color(255, 0, 0, 40);

    private static final Color DEBUG_TILE_OUTLINE = new Color(0, 255, 0, 220);
    private static final Color DEBUG_TILE_FILL = new Color(0, 255, 0, 100);

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
        ChunkArea area = areaManager.getActiveArea();
        Player local = client.getLocalPlayer();

        if (area == null || local == null)
        {
            return null;
        }

        WorldPoint playerWp = local.getWorldLocation();

        int playerChunkX = playerWp.getX() >> 3;
        int playerChunkY = playerWp.getY() >> 3;

        boolean isInside =
                area.containsChunk(playerChunkX, playerChunkY);

        // Log ONLY on state change
        if (isInside != wasInsideArena)
        {
            if (!isInside)
            {
                log.warn(
                        "DEBUG: Player LEFT arena chunk (player={}, arena={} / {})",
                        playerChunkX + "," + playerChunkY,
                        area.getBaseChunkX(),
                        area.getBaseChunkY()
                );
            }
            else
            {
                log.info("DEBUG: Player ENTERED arena chunk again");
            }
            wasInsideArena = isInside;
        }

        int plane = playerWp.getPlane();

        /* =========================
           Draw CHUNK BORDER (8x8)
           ========================= */
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

                drawTile(graphics, wp, BORDER_OUTLINE, BORDER_FILL);
            }
        }

        /* =========================
           Draw DEBUG TILE under player
           ========================= */
        drawTile(
                graphics,
                playerWp,
                DEBUG_TILE_OUTLINE,
                DEBUG_TILE_FILL
        );

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
                new BasicStroke(2)
        );
    }
}
