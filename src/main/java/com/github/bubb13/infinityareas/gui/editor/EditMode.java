
package com.github.bubb13.infinityareas.gui.editor;

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
    boolean forceEnableObject(Renderable renderable);

    MouseButton customOnMousePressed(MouseEvent event);
    boolean shouldCaptureObjectPress(MouseEvent event, Renderable renderable);
    void onBackgroundPressed(MouseEvent event, int sourcePressX, int sourcePressY);

    boolean customOnMouseDragged(MouseEvent event);
    Renderable directCaptureDraggedObject(MouseEvent event);
    boolean shouldCaptureObjectDrag(MouseEvent event, Renderable renderable);
    void onObjectDragged(MouseEvent event, Renderable renderable);

    boolean customOnMouseReleased(MouseEvent event);

    void onKeyPressed(KeyEvent event);
}
