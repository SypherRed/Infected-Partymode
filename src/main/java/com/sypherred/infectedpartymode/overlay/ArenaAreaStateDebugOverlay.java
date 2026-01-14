package com.sypherred.infectedpartymode.overlay;

import com.sypherred.infectedpartymode.area.AreaManager;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;

public class ArenaAreaStateDebugOverlay extends Overlay
{
    private final Client client;
    private final AreaManager areaManager;

    @Inject
    public ArenaAreaStateDebugOverlay(Client client, AreaManager areaManager)
    {
        this.client = client;
        this.areaManager = areaManager;

        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(PRIORITY_HIGHEST);
    }

    @Override
    public Dimension render(Graphics2D g)
    {
        int y = 40;

        g.setFont(new Font("Arial", Font.BOLD, 18));

        if (areaManager.getActiveArea() == null)
        {
            g.setColor(Color.RED);
            g.drawString("ACTIVE AREA = NULL", 40, y);
        }
        else
        {
            g.setColor(Color.GREEN);
            g.drawString("ACTIVE AREA IS SET", 40, y);
        }

        return null;
    }
}
