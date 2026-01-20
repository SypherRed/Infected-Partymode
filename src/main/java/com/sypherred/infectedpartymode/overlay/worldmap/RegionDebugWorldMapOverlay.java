package com.sypherred.infectedpartymode.overlay.worldmap;

import com.google.inject.Inject;
import com.sypherred.infectedpartymode.InfectedPartymodeConfig;

import java.awt.*;
import java.awt.geom.Rectangle2D;

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
    private static final Color FILL_COLOR = new Color(255, 0, 0, 45);
    private static final Color GRID_COLOR = new Color(255, 0, 0, 150);

    private static final int REGION_SIZE = 1 << 6;      // 64 tiles
    private static final int REGION_TRUNCATE = ~0x3F;   // snap to region
    private static final int LABEL_PADDING = 4;

    private final Client client;
    private final InfectedPartymodeConfig config;

    @Inject
    public RegionDebugWorldMapOverlay(Client client, InfectedPartymodeConfig config)
    {
        this.client = client;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPriority(Overlay.PRIORITY_HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.debugShowRegionIds())
        {
            return null;
        }

        Widget map = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
        if (map == null)
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

        // snap visible area to region grid (CRITICAL)
        int xRegionMin = (center.getX() - widthInTiles  / 2) & REGION_TRUNCATE;
        int xRegionMax = ((center.getX() + widthInTiles  / 2) & REGION_TRUNCATE) + REGION_SIZE;
        int yRegionMin = (center.getY() - heightInTiles / 2) & REGION_TRUNCATE;
        int yRegionMax = ((center.getY() + heightInTiles / 2) & REGION_TRUNCATE) + REGION_SIZE;

        int regionPixelSize = (int) Math.ceil(REGION_SIZE * pixelsPerTile);

        for (int x = xRegionMin; x < xRegionMax; x += REGION_SIZE)
        {
            for (int y = yRegionMin; y < yRegionMax; y += REGION_SIZE)
            {
                // tile offsets (IDENTICAL to region-locker)
                int xTileOffset = x + widthInTiles / 2 - center.getX();
                int yTileOffset = -( (center.getY() - heightInTiles / 2) - y );

                int xPos = (int) (xTileOffset * pixelsPerTile) + worldMapRect.x;
                int yPos = worldMapRect.height - (int) (yTileOffset * pixelsPerTile) + worldMapRect.y;
                yPos -= regionPixelSize; // critical Y-fix

                int regionId = ((x >> 6) << 8) | (y >> 6);
                String text = String.valueOf(regionId);

                Rectangle rect = new Rectangle(xPos, yPos, regionPixelSize, regionPixelSize);

                graphics.setColor(FILL_COLOR);
                graphics.fillRect(rect.x, rect.y, rect.width, rect.height);

                graphics.setColor(GRID_COLOR);
                graphics.drawRect(rect.x, rect.y, rect.width, rect.height);

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

        return null;
    }
}
