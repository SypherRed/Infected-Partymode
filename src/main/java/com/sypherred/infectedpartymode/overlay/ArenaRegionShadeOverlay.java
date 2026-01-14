package com.sypherred.infectedpartymode.overlay;

import com.sypherred.infectedpartymode.area.AreaManager;
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
 * - Player region remains normal
 * - All regions NOT allowed by AreaManager are shaded uniformly
 */
public class ArenaRegionShadeOverlay extends Overlay
{
    private static final Color SHADE_COLOR = new Color(90, 90, 90, 120);
    private static final int SCENE_SIZE = 104;

    private final Client client;
    private final AreaManager areaManager;

    @Inject
    public ArenaRegionShadeOverlay(Client client, AreaManager areaManager)
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
        // No active arena → nothing to render
        if (!areaManager.hasActiveArea() || client.getLocalPlayer() == null)
        {
            return null;
        }

        WorldView worldView = client.getTopLevelWorldView();
        int plane = worldView.getPlane();

        for (int sceneX = 0; sceneX < SCENE_SIZE; sceneX++)
        {
            for (int sceneY = 0; sceneY < SCENE_SIZE; sceneY++)
            {
                WorldPoint wp = WorldPoint.fromScene(worldView, sceneX, sceneY, plane);
                if (wp == null)
                {
                    continue;
                }

                // Shade everything that is NOT inside the allowed region set
                if (areaManager.isInsideArea(wp))
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
