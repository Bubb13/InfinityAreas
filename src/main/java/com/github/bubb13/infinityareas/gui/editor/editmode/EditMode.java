
package com.github.bubb13.infinityareas.gui.editor.editmode;

import com.github.bubb13.infinityareas.gui.editor.renderable.AbstractRenderable;
import com.github.bubb13.infinityareas.misc.undoredo.IUndoHandle;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public interface EditMode
{
    void reset();

    /**
     * Called after the {@link EditMode} has been started.
     */
    void onModeStart(final IUndoHandle ownedUndo);

    /**
     * Called after the {@link EditMode} has been resumed - e.g. the {@link EditMode} was superseded
     * by another {@link EditMode}, and that superseding {@link EditMode} has ended.
     */
    void onModeResume();

    /**
     * Called before the {@link EditMode} has been superseded by another {@link EditMode}.
     */
    void onModeSuspend();

    /**
     * Called after the {@link EditMode} has been ended.
     */
    void onModeEnd();

    /**
     * Called after the {@link EditMode} has been cancelled.
     */
    void onModeCancelled();

    /**
     * Called after the {@link EditMode} has been ended OR cancelled,
     * (and after the respective function has been called).
     */
    void onModeExit();

    void onDraw(GraphicsContext canvasContext);
    EditModeForceEnableState forceObjectEnableState(AbstractRenderable renderable);

    void onZoomFactorChanged(double newZoomFactor);
    void onMouseMoved(MouseEvent event);

    MouseButton customOnMousePressed(MouseEvent event);
    boolean shouldCaptureObjectPress(MouseEvent event, AbstractRenderable renderable);
    boolean onBackgroundPressed(MouseEvent event, double sourcePressX, double sourcePressY);

    void onDragDetected(MouseEvent event);
    boolean customOnMouseDragged(MouseEvent event);
    AbstractRenderable directCaptureDraggedObject(MouseEvent event);
    boolean shouldCaptureObjectDrag(MouseEvent event, AbstractRenderable renderable);
    void onObjectDragged(MouseEvent event, AbstractRenderable renderable);

    boolean customOnMouseReleased(MouseEvent event);
    boolean customOnObjectClicked(MouseEvent event, AbstractRenderable renderable);
    void onBackgroundClicked(MouseEvent event);

    void onMouseExited(MouseEvent event);

    void onKeyPressed(KeyEvent event);
}
