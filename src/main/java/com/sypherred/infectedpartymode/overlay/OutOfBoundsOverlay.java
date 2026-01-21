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
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!outOfBoundsManager.isOutOfBounds())
        {
            return null;
        }

        // Use full canvas size instead of clip bounds (more reliable)
        Rectangle bounds = graphics.getDeviceConfiguration()
                .getBounds();

        // Stronger, clearly visible escalation
        int alpha = outOfBoundsManager.isDanger() ? 130 : 70;

        graphics.setColor(new Color(255, 0, 0, alpha));
        graphics.fillRect(0, 0, bounds.width, bounds.height);

        return null;
    }
}
