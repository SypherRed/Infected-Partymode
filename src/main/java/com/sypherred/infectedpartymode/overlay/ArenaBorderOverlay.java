package com.sypherred.infectedpartymode.overlay;

import com.sypherred.infectedpartymode.area.AreaManager;
import com.sypherred.infectedpartymode.area.ChunkArea;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FINAL DEBUG Overlay (CORRECT LocalPoint math)
 */
public class ArenaBorderOverlay extends Overlay
{
    private static final Logger log =
            LoggerFactory.getLogger(ArenaBorderOverlay.class);

    private static final Color CHUNK_FILL =
            new Color(255, 0, 0, 30);
    private static final Color BORDER_OUTLINE =
            new Color(255, 0, 0, 220);

    private static final Color DEBUG_TILE_OUTLINE =
            new Color(0, 255, 0, 220);
    private static final Color DEBUG_TILE_FILL =
            new Color(0, 255, 0, 120);

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

        /* =========================
           DEBUG TILE under player
           ========================= */
        drawLocalTile(
                graphics,
                local.getLocalLocation(),
                DEBUG_TILE_OUTLINE,
                DEBUG_TILE_FILL
        );

        ChunkArea area = areaManager.getActiveArea();
        if (area == null)
        {
            return null;
        }

        int baseX = client.getBaseX();
        int baseY = client.getBaseY();

        int playerChunkX = local.getWorldLocation().getX() >> 3;
        int playerChunkY = local.getWorldLocation().getY() >> 3;

        boolean inside = area.containsChunk(playerChunkX, playerChunkY);
        if (inside != wasInsideArena)
        {
            log.warn(inside
                    ? "DEBUG: Player ENTERED arena chunk"
                    : "DEBUG: Player LEFT arena chunk");
            wasInsideArena = inside;
        }

        /* =========================
           Iterate over SCENE tiles
           ========================= */
        for (int sceneX = 0; sceneX < 104; sceneX++)
        {
            for (int sceneY = 0; sceneY < 104; sceneY++)
            {
                int worldX = baseX + sceneX;
                int worldY = baseY + sceneY;

                int chunkX = worldX >> 3;
                int chunkY = worldY >> 3;

                if (!area.containsChunk(chunkX, chunkY))
                {
                    continue;
                }

                // CENTER of tile (THIS WAS THE BUG)
                LocalPoint lp = new LocalPoint(
                        sceneX * 128 + 64,
                        sceneY * 128 + 64
                );

                drawLocalTile(
                        graphics,
                        lp,
                        BORDER_OUTLINE,
                        CHUNK_FILL
                );
            }
        }

        return null;
    }

    private void drawLocalTile(
            Graphics2D graphics,
            LocalPoint lp,
            Color outline,
            Color fill
    )
    {
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
