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

/**
 * Final stable arena overlay
 * - Renders arena RELATIVE to player (stable)
 * - Arena logic stays chunk-based (correct)
 */
public class ArenaBorderOverlay extends Overlay
{
    private static final Color ARENA_FILL =
            new Color(255, 0, 0, 40);
    private static final Color ARENA_BORDER =
            new Color(255, 0, 0, 220);

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
        Player local = client.getLocalPlayer();
        ChunkArea area = areaManager.getActiveArea();

        if (local == null || area == null)
        {
            return null;
        }

        WorldPoint playerWp = local.getWorldLocation();
        LocalPoint playerLp = local.getLocalLocation();

        int baseWorldX = playerWp.getX();
        int baseWorldY = playerWp.getY();

        int baseChunkX = area.getBaseChunkX();
        int baseChunkY = area.getBaseChunkY();

        /* =========================
           Render tiles around player
           ========================= */
        for (int dx = -16; dx <= 16; dx++)
        {
            for (int dy = -16; dy <= 16; dy++)
            {
                int worldX = baseWorldX + dx;
                int worldY = baseWorldY + dy;

                int chunkX = worldX >> 3;
                int chunkY = worldY >> 3;

                if (!area.containsChunk(chunkX, chunkY))
                {
                    continue;
                }

                boolean isBorder =
                        (worldX & 7) == 0 || (worldX & 7) == 7 ||
                                (worldY & 7) == 0 || (worldY & 7) == 7;

                LocalPoint lp = new LocalPoint(
                        playerLp.getX() + dx * 128,
                        playerLp.getY() + dy * 128
                );

                Polygon poly = Perspective.getCanvasTilePoly(client, lp);
                if (poly == null)
                {
                    continue;
                }

                OverlayUtil.renderPolygon(
                        graphics,
                        poly,
                        isBorder ? ARENA_BORDER : null,
                        ARENA_FILL,
                        new BasicStroke(2)
                );
            }
        }

        return null;
    }
}
