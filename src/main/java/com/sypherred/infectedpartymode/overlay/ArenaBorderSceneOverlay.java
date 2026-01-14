package com.sypherred.infectedpartymode.overlay;

import com.sypherred.infectedpartymode.area.AreaManager;
import com.sypherred.infectedpartymode.area.ChunkArea;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.WorldView;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.*;

public class ArenaBorderSceneOverlay extends Overlay
{
    private static final Color BORDER_COLOR = new Color(255, 0, 0, 220);
    private static final int SCENE_SIZE = 104;

    private final Client client;
    private final AreaManager areaManager;

    @Inject
    public ArenaBorderSceneOverlay(Client client, AreaManager areaManager)
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

        WorldView worldView = client.getTopLevelWorldView();
        Tile[][][] tiles = worldView.getScene().getTiles();
        int plane = worldView.getPlane();

        if (tiles == null || plane < 0)
        {
            return null;
        }

        for (int x = 0; x < SCENE_SIZE; x++)
        {
            for (int y = 0; y < SCENE_SIZE; y++)
            {
                Tile tile = tiles[plane][x][y];
                if (tile == null)
                {
                    continue;
                }

                WorldPoint wp = tile.getWorldLocation();
                int chunkX = wp.getX() >> 3;
                int chunkY = wp.getY() >> 3;

                if (!area.containsChunk(chunkX, chunkY))
                {
                    continue;
                }

                boolean isBorder =
                        (wp.getX() & 7) == 0 || (wp.getX() & 7) == 7 ||
                                (wp.getY() & 7) == 0 || (wp.getY() & 7) == 7;

                if (!isBorder)
                {
                    continue;
                }

                LocalPoint lp = LocalPoint.fromWorld(worldView, wp);
                if (lp == null)
                {
                    continue;
                }

                Polygon poly = Perspective.getCanvasTilePoly(client, lp);
                if (poly == null)
                {
                    continue;
                }

                OverlayUtil.renderPolygon(
                        graphics,
                        poly,
                        BORDER_COLOR,
                        null,
                        new BasicStroke(2)
                );
            }
        }

        return null;
    }
}
