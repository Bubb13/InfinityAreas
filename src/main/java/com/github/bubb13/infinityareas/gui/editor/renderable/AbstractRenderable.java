
package com.github.bubb13.infinityareas.gui.editor.renderable;

import com.github.bubb13.infinityareas.gui.editor.Editor;
import com.github.bubb13.infinityareas.misc.referencetracking.ReferenceHolder;
import com.github.bubb13.infinityareas.misc.referencetracking.ReferenceTrackable;
import com.github.bubb13.infinityareas.misc.referencetracking.ReferenceTracker;
import javafx.geometry.Point2D;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

public abstract class AbstractRenderable implements RenderableInterface, ReferenceTrackable
{
    //////////////////////
    // Protected Fields //
    //////////////////////

    protected final Editor editor;

    ////////////////////
    // Private Fields //
    ////////////////////

    private final ReferenceTracker referenceTracker = new ReferenceTracker(this);
    private boolean isHidden = false;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public AbstractRenderable(final Editor editor)
    {
        this.editor = editor;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void setHidden(final boolean hidden)
    {
        isHidden = hidden;
    }

    //-------------------------------//
    // RenderableInterface Overrides //
    //-------------------------------//

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
    @Override public void onDragStart(final MouseEvent event) {}
    @Override public void onDragged(final MouseEvent event) {}
    @Override public void onDragEnd(final MouseEvent event) {}
    @Override public boolean listensToZoomFactorChanges() { return false; }
    @Override public void onZoomFactorChanged(final double zoomFactor) {}
    @Override public void onBeforeRemoved(final boolean undoable) {}

    //------------------------------//
    // ReferenceTrackable Overrides //
    //------------------------------//

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
    public void softDelete()
    {
        editor.performAsTransaction(() ->
        {
            editor.pushUndo("AbstractRenderable::softDelete", () ->
            {
                restore();
                editor.pushUndo("AbstractRenderable::softDelete", this::softDelete);
            });

            // Needs to be before Editor::removeRenderable() so that selection is preserved.
            //   Renderable needs to be soft-deleted from the 'selected' list before the renderable
            //   is removed from the editor, otherwise the renderable's removal results in a hard-removal
            //   from the 'selected' list.
            referenceTracker.softDelete();
            editor.removeRenderable(this, true);
        });
    }

    @Override
    public void restore()
    {
        referenceTracker.restore();
    }

    @Override
    public void delete()
    {
        referenceTracker.delete();
    }
}
