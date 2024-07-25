
package com.github.bubb13.infinityareas.gui.editor.editmode;

import com.github.bubb13.infinityareas.gui.editor.renderable.Renderable;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class AbstractEditMode implements EditMode
{
    public void reset() {}

    public void onEnterMode() {}

    public void onExitMode() {}

    public void onDraw(final GraphicsContext canvasContext) {}

    public boolean forceEnableObject(final Renderable renderable)
    {
        return false;
    }

    public MouseButton customOnMousePressed(final MouseEvent event)
    {
        return null;
    }

    public boolean shouldCaptureObjectPress(final MouseEvent event, final Renderable renderable)
    {
        return false;
    }

    public void onBackgroundPressed(final MouseEvent event, final double sourcePressX, final double sourcePressY) {}

    public boolean customOnMouseDragged(final MouseEvent event)
    {
        return false;
    }

    public Renderable directCaptureDraggedObject(final MouseEvent event)
    {
        return null;
    }

    public boolean shouldCaptureObjectDrag(final MouseEvent event, final Renderable renderable)
    {
        return false;
    }

    public void onObjectDragged(final MouseEvent event, final Renderable renderable) {}

    public boolean customOnMouseReleased(final MouseEvent event)
    {
        return false;
    }

    public void onKeyPressed(final KeyEvent event) {}
}
