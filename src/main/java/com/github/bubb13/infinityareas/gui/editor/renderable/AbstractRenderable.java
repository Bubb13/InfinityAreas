
package com.github.bubb13.infinityareas.gui.editor.renderable;

import com.github.bubb13.infinityareas.misc.ReferenceHolder;
import com.github.bubb13.infinityareas.misc.ReferenceTrackable;
import com.github.bubb13.infinityareas.misc.ReferenceTracker;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

public abstract class AbstractRenderable implements RenderableInterface, ReferenceTrackable
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final ReferenceTracker referenceTracker = new ReferenceTracker(this);
    private boolean isHidden = false;

    ////////////////////
    // Public Methods //
    ////////////////////

    public void setHidden(final boolean hidden)
    {
        isHidden = hidden;
    }

    @Override
    public boolean isHidden()
    {
        return isHidden;
    }

    @Override
    public boolean snapshotable()
    {
        return false;
    }

    @Override
    public void snapshot(final GraphicsContext graphicsContext)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addedTo(final ReferenceHolder<?> referenceHolder)
    {
        referenceTracker.addedTo(referenceHolder);
    }

    @Override
    public void removedFrom(final ReferenceHolder<?> referenceHolder)
    {
        referenceTracker.removedFrom(referenceHolder);
    }

    @Override
    public void delete()
    {
        referenceTracker.delete();
    }

    @Override public int sortWeight() { return 0; }

    @Override
    public boolean contains(final Point2D point)
    {
        return getCorners().contains(point);
    }

    @Override public boolean offerPressCapture(final MouseEvent event) { return false; }
    @Override public void onClicked(final MouseEvent mouseEvent) {}
    @Override public boolean offerDragCapture(final MouseEvent event) { return false; }
    @Override public void onBeforeSelected() {}
    @Override public void onBeforeAdditionalObjectSelected(final AbstractRenderable renderable) {}
    @Override public void onReceiveKeyPress(final KeyEvent event) {}
    @Override public void onUnselected() {}
    @Override public void onDragged(final MouseEvent event) {}
    @Override public boolean listensToZoomFactorChanges() { return false; }
    @Override public void onZoomFactorChanged(final double zoomFactor) {}
}
