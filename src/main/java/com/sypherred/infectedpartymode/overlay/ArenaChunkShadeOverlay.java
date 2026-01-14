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

/**
 * Arena visualization in Region-Locker style:
 * - Player chunk remains normal
 * - All other arena chunks are shaded uniformly
 */
public class ArenaChunkShadeOverlay extends Overlay
{
    private static final Color SHADE_COLOR = new Color(90, 90, 90, 120);
    private static final int SCENE_SIZE = 104;

    private final Client client;
    private final AreaManager areaManager;

    @Inject
    public ArenaChunkShadeOverlay(Client client, AreaManager areaManager)
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
        if (area == null || client.getLocalPlayer() == null)
        {
            return null;
        }

        WorldView worldView = client.getTopLevelWorldView();
        int plane = worldView.getPlane();

        WorldPoint playerWp = client.getLocalPlayer().getWorldLocation();
        int playerChunkX = playerWp.getX() >> 3;
        int playerChunkY = playerWp.getY() >> 3;

        for (int sceneX = 0; sceneX < SCENE_SIZE; sceneX++)
        {
            for (int sceneY = 0; sceneY < SCENE_SIZE; sceneY++)
            {
                WorldPoint wp = WorldPoint.fromScene(worldView, sceneX, sceneY, plane);
                if (wp == null)
                {
                    continue;
                }

                int chunkX = wp.getX() >> 3;
                int chunkY = wp.getY() >> 3;

                // Only shade tiles that belong to the arena
                if (!area.containsChunk(chunkX, chunkY))
                {
                    continue;
                }

                // Do NOT shade the player's current chunk
                if (chunkX == playerChunkX && chunkY == playerChunkY)
                {
                    continue;
                }

                LocalPoint lp = LocalPoint.fromScene(sceneX, sceneY, worldView);
                if (lp == null)
                {
                    continue;
                }

                Polygon poly = Perspective.getCanvasTilePoly(client, lp);
                if (poly == null)
                {
                    continue;
                }

                g.setColor(SHADE_COLOR);
                g.fill(poly);
            }
        }

        return null;
    }
}
