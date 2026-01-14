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
    private static final Color BORDER_OUTLINE = new Color(255, 0, 0, 220);
    private static final Color BORDER_FILL = new Color(255, 0, 0, 40);

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

        // Scene base in world coordinates
        final int baseX = client.getBaseX();
        final int baseY = client.getBaseY();
        final int plane = client.getLocalPlayer().getWorldLocation().getPlane();

        // Iterate only tiles that are currently in the scene (0..103)
        // and draw those that are part of the arena border.
        for (int sceneX = 0; sceneX < 104; sceneX++)
        {
            for (int sceneY = 0; sceneY < 104; sceneY++)
            {
                int worldX = baseX + sceneX;
                int worldY = baseY + sceneY;

                // Convert to chunk coords (8x8)
                int chunkX = worldX >> 3;
                int chunkY = worldY >> 3;

                // If not inside arena chunk area -> skip
                if (!area.containsChunk(chunkX, chunkY))
                {
                    continue;
                }

                // Determine tile position inside its chunk (0..7)
                int inChunkX = worldX & 7;
                int inChunkY = worldY & 7;

                // Border tiles only
                boolean isBorder =
                        inChunkX == 0 || inChunkX == 7 ||
                                inChunkY == 0 || inChunkY == 7;

                if (!isBorder)
                {
                    continue;
                }

                // Build LocalPoint directly from scene coordinates
                // LocalPoint expects "local" = scene tile * 128
                LocalPoint lp = new LocalPoint(sceneX * 128, sceneY * 128);

                Polygon poly = Perspective.getCanvasTilePoly(client, lp);
                if (poly == null)
                {
                    continue;
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

        return null;
    }
}
