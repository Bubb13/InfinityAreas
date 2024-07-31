
package com.github.bubb13.infinityareas.gui.pane;

import com.github.bubb13.infinityareas.gui.VisibleNotifiable;
import com.github.bubb13.infinityareas.gui.control.CustomScrollPane;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

public class NotifyingScrollPane extends CustomScrollPane
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private boolean blockNotify = false;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public NotifyingScrollPane()
    {
        super();
        init();
    }

    ///////////////////////
    // Protected Methods //
    ///////////////////////

    @Override
    protected void layoutChildren()
    {
        super.layoutChildren();
        checkVisibility();
    }

    protected void blockNotify(final boolean newValue)
    {
        blockNotify = newValue;
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init()
    {
        hvalueProperty().addListener((obs, oldBounds, newBounds) -> checkVisibility());
        vvalueProperty().addListener((obs, oldBounds, newBounds) -> checkVisibility());
    }

    private void checkVisibility()
    {
        if (blockNotify)
        {
            return;
        }

        final Pane content = (Pane)getContent();

        final Bounds viewportBounds = getViewportBounds();
        final double viewportWidth = viewportBounds.getWidth();
        final double viewportHeight = viewportBounds.getHeight();

        // Important: Using `getWidth()` and `getHeight()` instead of `content.getBoundsInLocal()` to see the
        //            content's dimensions as they are RIGHT NOW, (`content.getBoundsInLocal()` returns the
        //            values as they were before the current layout pass). Since this function might be called
        //            during a layout pass, it is import to use the new dimensions, otherwise invalid bounds
        //            might be passed to children in the below code.
        final double contentWidth = content.getWidth();
        final double contentHeight = content.getHeight();

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
                //System.out.printf("Notifying, seen: [%f,%f,%f,%f], child: [%f,%f,%f,%f]\n",
                //    seenBounds.getMinX(), seenBounds.getMinY(), seenBounds.getWidth(), seenBounds.getHeight(),
                //    childBounds.getMinX(), childBounds.getMinY(), childBounds.getWidth(), childBounds.getHeight());

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
