
package com.github.bubb13.infinityareas.gui.editor.editmode;

import com.github.bubb13.infinityareas.gui.editor.renderable.AbstractRenderable;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class AbstractEditMode implements EditMode
{
    public void reset() {}

    public void onModeStart() {}

    public void onModeResume() {}

    public void onModeSuspend() {}

    public void onModeEnd() {}

    public void onDraw(final GraphicsContext canvasContext) {}

    public boolean forceEnableObject(final AbstractRenderable renderable)
    {
        return false;
    }

    public MouseButton customOnMousePressed(final MouseEvent event)
    {
        return null;
    }

    public boolean shouldCaptureObjectPress(final MouseEvent event, final AbstractRenderable renderable)
    {
        return false;
    }

    public boolean onBackgroundPressed(final MouseEvent event, final double sourcePressX, final double sourcePressY)
    {
        return false;
    }

    public boolean customOnMouseDragged(final MouseEvent event)
    {
        return false;
    }

    public AbstractRenderable directCaptureDraggedObject(final MouseEvent event)
    {
        return null;
    }

    public boolean shouldCaptureObjectDrag(final MouseEvent event, final AbstractRenderable renderable)
    {
        return false;
    }

    public void onObjectDragged(final MouseEvent event, final AbstractRenderable renderable) {}

    public boolean customOnMouseReleased(final MouseEvent event)
    {
        return false;
    }

    public void onBackgroundClicked(final MouseEvent event) {}

    public void onKeyPressed(final KeyEvent event) {}
}
