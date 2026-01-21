package com.sypherred.infectedpartymode.overlay;

import com.sypherred.infectedpartymode.rules.OutOfBoundsManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;

/**
 * Red screen overlay when player is outside the arena.
 */
public class OutOfBoundsOverlay extends Overlay
{
    private final OutOfBoundsManager outOfBoundsManager;

    @Inject
    public OutOfBoundsOverlay(OutOfBoundsManager outOfBoundsManager)
    {
        this.outOfBoundsManager = outOfBoundsManager;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!outOfBoundsManager.isOutOfBounds())
        {
            return null;
        }

        Rectangle bounds = graphics.getClipBounds();
        if (bounds == null)
        {
            return null;
        }

        // Escalate intensity if danger
        int alpha = outOfBoundsManager.isDanger() ? 90 : 45;

        graphics.setColor(new Color(255, 0, 0, alpha));
        graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

        return null;
    }
}
