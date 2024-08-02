
package com.github.bubb13.infinityareas.gui.editor.editmode;

import com.github.bubb13.infinityareas.gui.dialog.WarningAlertTwoOptions;
import com.github.bubb13.infinityareas.gui.editor.Editor;
import com.github.bubb13.infinityareas.gui.editor.EditorCommons;
import com.github.bubb13.infinityareas.gui.editor.GenericPolygon;
import com.github.bubb13.infinityareas.gui.editor.renderable.AbstractRenderable;
import com.github.bubb13.infinityareas.gui.editor.renderable.RenderablePolygon;
import com.github.bubb13.infinityareas.gui.editor.renderable.RenderableVertex;
import com.github.bubb13.infinityareas.misc.ReferenceHolder;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public abstract class DrawPolygonEditMode<BackingPolygonType extends GenericPolygon> extends LabeledEditMode
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private RenderablePolygon<BackingPolygonType> drawingPolygon;
    private final ReferenceHolder<RenderablePolygon<BackingPolygonType>> drawingPolygonRef =
        (reference) -> drawingPolygon = null;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public DrawPolygonEditMode(final Editor editor)
    {
        super(editor, "Draw Polygon Mode");
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    @Override
    public boolean forceEnableObject(final AbstractRenderable renderable)
    {
        return renderable instanceof RenderablePolygon || renderable instanceof RenderableVertex;
    }

    @Override
    public boolean shouldCaptureObjectPress(final MouseEvent event, final AbstractRenderable renderable)
    {
        return renderable instanceof RenderableVertex;
    }

    @Override
    public boolean onBackgroundPressed(final MouseEvent event, final double sourcePressX, final double sourcePressY)
    {
        if (event.getButton() != MouseButton.PRIMARY) return false;

        final int sourcePressXInt = (int)sourcePressX;
        final int sourcePressYInt = (int)sourcePressY;

        if (drawingPolygon == null)
        {
            final BackingPolygonType polygon = createBackingPolygon();
            polygon.setBoundingBoxLeft(sourcePressXInt);
            polygon.setBoundingBoxRight(sourcePressXInt + 1);
            polygon.setBoundingBoxTop(sourcePressYInt);
            polygon.setBoundingBoxBottom(sourcePressYInt + 1);

            drawingPolygon = createRenderablePolygon(polygon);
            drawingPolygon.addedTo(drawingPolygonRef);
            drawingPolygon.setRenderImpliedFinalLine(false);
            drawingPolygon.setDrawing(true);
        }

        drawingPolygon.addNewVertex(sourcePressXInt, sourcePressYInt);
        return true;
    }

    @Override
    public AbstractRenderable directCaptureDraggedObject(final MouseEvent event)
    {
        if (event.getButton() == MouseButton.PRIMARY)
        {
            return drawingPolygon.getRenderablePolygonVertices().getLast();
        }
        return null;
    }

    @Override
    public boolean shouldCaptureObjectDrag(final MouseEvent event, final AbstractRenderable renderable)
    {
        return renderable instanceof RenderableVertex;
    }

    @Override
    public void onObjectDragged(final MouseEvent event, final AbstractRenderable renderable)
    {
        renderable.onDragged(event);
    }

    @Override
    public void onKeyPressed(final KeyEvent event)
    {
        final KeyCode key = event.getCode();

        switch (key)
        {
            case ESCAPE ->
            {
                if (editor.selectedCount() > 0)
                {
                    event.consume();
                    editor.unselectAll();
                }
                else
                {
                    event.consume();
                    cancelDrawPolygonMode();
                }
            }
            case ENTER, SPACE ->
            {
                event.consume();
                endDrawPolygonMode();
            }
            case B ->
            {
                event.consume();
                EditorCommons.onBisectLine(editor);
            }
            case DELETE ->
            {
                event.consume();
                EditorCommons.deleteSelected(editor);
            }
            case Q ->
            {
                event.consume();
                editor.enterEditMode(QuickSelectEditMode.class);
            }
        }
    }

    ////////////////////////////////
    // Protected Abstract Methods //
    ////////////////////////////////

    protected abstract BackingPolygonType createBackingPolygon();
    protected abstract RenderablePolygon<BackingPolygonType> createRenderablePolygon(BackingPolygonType backingPolygon);
    protected abstract void saveBackingPolygon(BackingPolygonType polygon);

    protected void cleanUpPolygonDrawingModeRenderObjects()
    {
        if (drawingPolygon == null) return;

        // Remove all renderable objects from the quadtree
        for (final RenderableVertex vertex : drawingPolygon.getRenderablePolygonVertices())
        {
            editor.removeRenderable(vertex);
        }
        editor.removeRenderable(drawingPolygon);
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void cancelDrawPolygonMode()
    {
        cleanUpPolygonDrawingModeRenderObjects();
        drawingPolygon = null;
        editor.exitEditMode();
    }

    private void endDrawPolygonMode()
    {
        boolean endMode = true;

        if (drawingPolygon != null)
        {
            final BackingPolygonType polygon = drawingPolygon.getPolygon();

            // Detect invalid polygon and warn that it will be deleted
            if (polygon.getVertices().size() < 3)
            {
                final boolean[] deletePolygon = new boolean[] { false };
                WarningAlertTwoOptions.openAndWait(
                    "The drawn polygon has less than 3 vertices.\n\n" +
                        "Ending polygon drawing mode will delete the drawn vertices.\n\n" +
                        "Do you still wish to end polygon drawing mode?",
                    "Yes", () -> deletePolygon[0] = true,
                    "Cancel", null);

                // Delete the drawn polygon if prompted to
                if (deletePolygon[0])
                {
                    cleanUpPolygonDrawingModeRenderObjects();
                    drawingPolygon = null;
                }
                else
                {
                    endMode = false;
                }
            }
            else
            {
                drawingPolygon.setDrawing(false);
                drawingPolygon.setRenderImpliedFinalLine(true);
                drawingPolygon = null;
                saveBackingPolygon(polygon);
            }
        }

        if (endMode)
        {
            editor.exitEditMode();
        }
    }
}
