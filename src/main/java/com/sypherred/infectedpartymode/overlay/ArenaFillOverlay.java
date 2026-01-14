package com.sypherred.infectedpartymode.overlay;

import com.sypherred.infectedpartymode.area.AreaManager;
import com.sypherred.infectedpartymode.area.ChunkArea;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;

public class ArenaFillOverlay extends Overlay
{
    private static final Color FILL_COLOR = new Color(255, 0, 0, 60);
    private static final int SCENE_SIZE = 104;

    private final Client client;
    private final AreaManager areaManager;

    @Inject
    public ArenaFillOverlay(Client client, AreaManager areaManager)
    {
        this.client = client;
        this.areaManager = areaManager;

        setLayer(OverlayLayer.ABOVE_SCENE);
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(PRIORITY_HIGH);
    }

    @Override
    public Dimension render(Graphics2D g)
    {
        ChunkArea area = areaManager.getActiveArea();
        if (area == null)
        {
            return null;
        }

        WorldView worldView = client.getTopLevelWorldView();

        for (int x = 0; x < SCENE_SIZE; x++)
        {
            for (int y = 0; y < SCENE_SIZE; y++)
            {
                WorldPoint wp = WorldPoint.fromScene(worldView, x, y, worldView.getPlane());
                if (wp == null)
                {
                    continue;
                }

                int chunkX = wp.getX() >> 3;
                int chunkY = wp.getY() >> 3;

                if (!area.containsChunk(chunkX, chunkY))
                {
                    continue;
                }

                LocalPoint lp = LocalPoint.fromScene(x, y, worldView);
                if (lp == null)
                {
                    continue;
                }

                Polygon poly = Perspective.getCanvasTilePoly(client, lp);
                if (poly == null)
                {
                    continue;
                }

                g.setColor(FILL_COLOR);
                g.fill(poly);
            }
        }

        return null;
    }
}
