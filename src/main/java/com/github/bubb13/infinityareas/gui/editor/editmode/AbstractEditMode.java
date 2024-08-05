
package com.github.bubb13.infinityareas.gui.editor.editmode;

import com.github.bubb13.infinityareas.gui.editor.renderable.AbstractRenderable;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class AbstractEditMode implements EditMode
{
    @Override public void reset() {}

    @Override public void onModeStart() {}

    @Override public void onModeResume() {}

    @Override public void onModeSuspend() {}

    @Override public void onModeEnd() {}

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
    public boolean onBackgroundPressed(final MouseEvent event, final double sourcePressX, final double sourcePressY)
    {
        return false;
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
