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

/**
 * Final Arena Overlay
 * - Displays the active 8x8 chunk arena
 * - Scene-based rendering (stable)
 * - World-based logic (correct gameplay)
 */
public class ArenaBorderOverlay extends Overlay
{
    private static final Color ARENA_FILL =
            new Color(255, 0, 0, 35);
    private static final Color ARENA_BORDER =
            new Color(255, 0, 0, 200);

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
        if (local == null)
        {
            return null;
        }

        ChunkArea area = areaManager.getActiveArea();
        if (area == null)
        {
            return null;
        }

        int baseX = client.getBaseX();
        int baseY = client.getBaseY();

        /* =========================
           Render arena via SCENE
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

                int inChunkX = worldX & 7;
                int inChunkY = worldY & 7;

                boolean isBorder =
                        inChunkX == 0 || inChunkX == 7 ||
                                inChunkY == 0 || inChunkY == 7;

                LocalPoint lp = new LocalPoint(
                        sceneX * 128 + 64,
                        sceneY * 128 + 64
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
