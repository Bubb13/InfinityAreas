
package com.github.bubb13.infinityareas.gui.pane;

import com.github.bubb13.infinityareas.gui.misc.VisibleNotifiable;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;

public class NotifyingScrollPane extends ScrollPane
{
    public NotifyingScrollPane()
    {
        super();
        init();
    }

    private void init()
    {
        hvalueProperty().addListener((obs, oldBounds, newBounds) -> checkVisibility());
        vvalueProperty().addListener((obs, oldBounds, newBounds) -> checkVisibility());
    }

    @Override
    protected void layoutChildren()
    {
        super.layoutChildren();
        checkVisibility();
    }

    private void checkVisibility()
    {
        final Pane content = (Pane)getContent();

        final Bounds viewportBounds = getViewportBounds();
        final double viewportWidth = viewportBounds.getWidth();
        final double viewportHeight = viewportBounds.getHeight();

        final Bounds contentBounds = content.getBoundsInLocal();
        final double contentWidth = contentBounds.getWidth();
        final double contentHeight = contentBounds.getHeight();

        final double hMax = getHmax();
        final double vMax = getVmax();

        final double hRel = getHvalue() / hMax;
        final double vRel = getVvalue() / vMax;

        final double xLeft = hRel * (contentWidth - viewportWidth);
        final double yTop = vRel * (contentHeight - viewportHeight);

        final Bounds seenBounds = new BoundingBox(xLeft, yTop, viewportWidth, viewportHeight);

        for (final Node child : content.getChildren())
        {
            if (child instanceof VisibleNotifiable childNotifiable)
            {
                final Bounds childBounds = new BoundingBox(
                    child.getLayoutX(), child.getLayoutY(),
                    child.prefWidth(-1), child.prefHeight(-1)
                );
                final Bounds bounds = getIntersection(childBounds, seenBounds);
                childNotifiable.notifyVisible(bounds);
            }
        }
    }

    private Bounds getIntersection(final Bounds bounds1, final Bounds bounds2)
    {
        final double minX = Math.max(bounds1.getMinX(), bounds2.getMinX());
        final double minY = Math.max(bounds1.getMinY(), bounds2.getMinY());
        final double maxX = Math.min(bounds1.getMaxX(), bounds2.getMaxX());
        final double maxY = Math.min(bounds1.getMaxY(), bounds2.getMaxY());

        if (minX < maxX && minY < maxY)
        {
            return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
        }
        else
        {
            return new BoundingBox(-1, -1, -1, -1);
        }
    }
}
