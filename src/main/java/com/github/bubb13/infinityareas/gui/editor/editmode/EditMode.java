
package com.github.bubb13.infinityareas.gui.editor.editmode;

import com.github.bubb13.infinityareas.gui.editor.renderable.AbstractRenderable;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public interface EditMode
{
    void reset();

    void onEnterMode();
    void onExitMode();

    void onDraw(GraphicsContext canvasContext);
    boolean forceEnableObject(AbstractRenderable renderable);

    MouseButton customOnMousePressed(MouseEvent event);
    boolean shouldCaptureObjectPress(MouseEvent event, AbstractRenderable renderable);
    void onBackgroundPressed(MouseEvent event, double sourcePressX, double sourcePressY);

    boolean customOnMouseDragged(MouseEvent event);
    AbstractRenderable directCaptureDraggedObject(MouseEvent event);
    boolean shouldCaptureObjectDrag(MouseEvent event, AbstractRenderable renderable);
    void onObjectDragged(MouseEvent event, AbstractRenderable renderable);

    boolean customOnMouseReleased(MouseEvent event);

    void onKeyPressed(KeyEvent event);
}
