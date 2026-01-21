package com.sypherred.infectedpartymode.overlay.worldmap;

import com.google.inject.Inject;
import com.sypherred.infectedpartymode.InfectedPartymodeConfig;
import com.sypherred.infectedpartymode.InfectedPartymodePlugin;
import com.sypherred.infectedpartymode.area.AreaManager;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Set;

import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class RegionDebugWorldMapOverlay extends Overlay
{
    /* =========================
       Colors
       ========================= */

    private static final Color DEBUG_FILL_COLOR   = new Color(160, 160, 160, 140);
    private static final Color PREVIEW_COLOR      = new Color(0, 180, 255, 180);
    private static final Color OOB_FILL_COLOR     = new Color(255, 0, 0, 45);
    private static final Color OOB_BORDER_COLOR   = new Color(255, 0, 0, 150);

    /* =========================
       Region math
       ========================= */

    private static final int REGION_SIZE = 1 << 6;      // 64 tiles
    private static final int REGION_TRUNCATE = ~0x3F;
    private static final int LABEL_PADDING = 4;

    private final Client client;
    private final InfectedPartymodeConfig config;
    private final InfectedPartymodePlugin plugin;
    private final AreaManager areaManager;

    @Inject
    public RegionDebugWorldMapOverlay(
            Client client,
            InfectedPartymodeConfig config,
            InfectedPartymodePlugin plugin,
            AreaManager areaManager
    )
    {
        this.client = client;
        this.config = config;
        this.plugin = plugin;
        this.areaManager = areaManager;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPriority(Overlay.PRIORITY_HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        Widget map = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
        if (map == null)
        {
            return null;
        }

        final boolean debugMode   = config.debugShowRegionIds();
        final boolean previewMode = !plugin.isGameRunning();

        final Set<Integer> previewRegions = areaManager.getPreviewRegions();
        final Set<Integer> activeRegions  = areaManager.getAllowedRegions();

        // Nothing to draw at all
        if (!debugMode && previewMode && previewRegions.isEmpty())
        {
            return null;
        }

        WorldMap worldMap = client.getWorldMap();
        float pixelsPerTile = worldMap.getWorldMapZoom();
        Rectangle worldMapRect = map.getBounds();

        graphics.setClip(worldMapRect);

        int widthInTiles  = (int) Math.ceil(worldMapRect.getWidth()  / pixelsPerTile);
        int heightInTiles = (int) Math.ceil(worldMapRect.getHeight() / pixelsPerTile);

        Point center = worldMap.getWorldMapPosition();

        int xRegionMin = (center.getX() - widthInTiles  / 2) & REGION_TRUNCATE;
        int xRegionMax = ((center.getX() + widthInTiles  / 2) & REGION_TRUNCATE) + REGION_SIZE;
        int yRegionMin = (center.getY() - heightInTiles / 2) & REGION_TRUNCATE;
        int yRegionMax = ((center.getY() + heightInTiles / 2) & REGION_TRUNCATE) + REGION_SIZE;

        int regionPixelSize = (int) Math.ceil(REGION_SIZE * pixelsPerTile);

        for (int x = xRegionMin; x < xRegionMax; x += REGION_SIZE)
        {
            for (int y = yRegionMin; y < yRegionMax; y += REGION_SIZE)
            {
                int regionId = ((x >> 6) << 8) | (y >> 6);

                boolean isPreview = previewRegions.contains(regionId);
                boolean isAllowed = activeRegions.contains(regionId);

                /* =========================
                   Decide visibility
                   ========================= */

                boolean drawPreview = previewMode && isPreview;
                boolean drawOob     = !previewMode && !isAllowed;
                boolean drawDebug   = debugMode;

                if (!drawPreview && !drawOob && !drawDebug)
                {
                    continue;
                }

                int xTileOffset = x + widthInTiles / 2 - center.getX();
                int yTileOffset = -((center.getY() - heightInTiles / 2) - y);

                int xPos = (int) (xTileOffset * pixelsPerTile) + worldMapRect.x;
                int yPos = worldMapRect.height - (int) (yTileOffset * pixelsPerTile) + worldMapRect.y;
                yPos -= regionPixelSize;

                Rectangle rect = new Rectangle(xPos, yPos, regionPixelSize, regionPixelSize);

                /* =========================
                   Draw layers
                   ========================= */

                if (drawOob)
                {
                    graphics.setColor(OOB_FILL_COLOR);
                    graphics.fillRect(rect.x, rect.y, rect.width, rect.height);

                    graphics.setColor(OOB_BORDER_COLOR);
                    graphics.drawRect(rect.x, rect.y, rect.width, rect.height);
                }
                else if (drawPreview)
                {
                    graphics.setColor(PREVIEW_COLOR);
                    graphics.drawRect(rect.x, rect.y, rect.width, rect.height);
                }
                else if (drawDebug)
                {
                    graphics.setColor(DEBUG_FILL_COLOR);
                    graphics.fillRect(rect.x, rect.y, rect.width, rect.height);
                }

                /* =========================
                   Debug text
                   ========================= */

                if (debugMode)
                {
                    String text = String.valueOf(regionId);
                    FontMetrics fm = graphics.getFontMetrics();
                    Rectangle2D tb = fm.getStringBounds(text, graphics);

                    graphics.setColor(Color.WHITE);
                    graphics.drawString(
                            text,
                            rect.x + LABEL_PADDING,
                            rect.y + LABEL_PADDING + (int) tb.getHeight()
                    );
                }
            }
        }

        return null;
    }
}
