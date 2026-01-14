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

        // 1) Beweis: Overlay läuft
        graphics.setColor(Color.MAGENTA);
        graphics.setFont(new Font("Arial", Font.BOLD, 16));
        graphics.drawString("OVERLAY ACTIVE", 20, 40);

        // 2) Beweis: zeichne IMMER die 5x5 Tiles um den Spieler
        LocalPoint center = local.getLocalLocation();
        int cx = center.getX();
        int cy = center.getY();

        for (int dx = -2; dx <= 2; dx++)
        {
            for (int dy = -2; dy <= 2; dy++)
            {
                LocalPoint lp = new LocalPoint(
                        cx + dx * 128,
                        cy + dy * 128
                );

                Polygon poly = Perspective.getCanvasTilePoly(client, lp);
                if (poly == null)
                {
                    continue;
                }

                graphics.setColor(new Color(255, 0, 0, 120));
                graphics.fill(poly);
                graphics.setColor(Color.RED);
                graphics.draw(poly);
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
