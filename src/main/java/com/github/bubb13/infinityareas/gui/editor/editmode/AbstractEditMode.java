
package com.github.bubb13.infinityareas.gui.editor.editmode;

import com.github.bubb13.infinityareas.gui.editor.renderable.AbstractRenderable;
import com.github.bubb13.infinityareas.misc.undoredo.IUndoHandle;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class AbstractEditMode implements EditMode
{
    //////////////////////
    // Protected Fields //
    //////////////////////

    protected IUndoHandle ownedUndo;

    ////////////////////
    // Public Methods //
    ////////////////////

    //--------------------//
    // EditMode Overrides //
    //--------------------//

    @Override
    public void reset()
    {
        ownedUndo = null;
    }

    @Override
    public void onModeStart(final IUndoHandle ownedUndo)
    {
        this.ownedUndo = ownedUndo;
    }

    @Override public void onModeResume() {}

    @Override public void onModeSuspend() {}

    @Override public void onModeEnd() {}

    @Override public void onModeCancelled() {}

    @Override
    public void onModeExit()
    {
        ownedUndo = null;
    }

    @Override public void onDraw(final GraphicsContext canvasContext) {}

    @Override
    public EditModeForceEnableState forceObjectEnableState(final AbstractRenderable renderable)
    {
        return EditModeForceEnableState.NO;
    }

    @Override public void onZoomFactorChanged(final double newZoomFactor) {}

    @Override public void onMouseMoved(final MouseEvent event) {}

    @Override
    public MouseButton customOnMousePressed(final MouseEvent event)
    {
        return null;
    }

    @Override
    public boolean shouldCaptureObjectPress(final MouseEvent event, final AbstractRenderable renderable)
    {
        return false;
    }

    @Override
    public boolean shouldCaptureBackgroundPress(final MouseEvent event, final double sourcePressX, final double sourcePressY)
    {
        return true;
    }

    @Override public void onDragDetected(final MouseEvent event) {}

    @Override
    public boolean customOnMouseDragged(final MouseEvent event)
    {
        return false;
    }

    @Override
    public AbstractRenderable directCaptureDraggedObject(final MouseEvent event)
    {
        return null;
    }

    @Override
    public boolean shouldCaptureObjectDrag(final MouseEvent event, final AbstractRenderable renderable)
    {
        return false;
    }

    @Override public void onObjectDragged(final MouseEvent event, final AbstractRenderable renderable) {}

    @Override
    public boolean customOnMouseReleased(final MouseEvent event)
    {
        return false;
    }

    @Override
    public boolean customOnObjectClicked(final MouseEvent event, final AbstractRenderable renderable)
    {
        return false;
    }

    @Override public void onBackgroundClicked(final MouseEvent event) {}

    @Override public void onMouseExited(final MouseEvent event) {}

    @Override public void onKeyPressed(final KeyEvent event) {}
}
