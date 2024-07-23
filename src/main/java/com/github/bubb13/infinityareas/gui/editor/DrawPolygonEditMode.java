
package com.github.bubb13.infinityareas.gui.editor;

import com.github.bubb13.infinityareas.gui.dialog.WarningAlertTwoOptions;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class DrawPolygonEditMode extends LabeledEditMode
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final PolygonDelegator polygonDelegator;
    private RenderablePolygon drawingPolygon;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public DrawPolygonEditMode(final Editor editor, final PolygonDelegator polygonDelegator)
    {
        super(editor, "Draw Polygon Mode");
        this.polygonDelegator = polygonDelegator;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    @Override
    public boolean forceEnableObject(final Renderable renderable)
    {
        return renderable instanceof RenderablePolygon || renderable instanceof RenderableVertex;
    }

    @Override
    public boolean shouldCaptureObjectPress(final MouseEvent event, final Renderable renderable)
    {
        return renderable instanceof RenderableVertex;
    }

    @Override
    public void onBackgroundPressed(final MouseEvent event, final int sourcePressX, final int sourcePressY)
    {
        if (event.getButton() != MouseButton.PRIMARY) return;

        if (drawingPolygon == null)
        {
            final GenericPolygon polygon = polygonDelegator.create();
            polygon.setBoundingBoxLeft(sourcePressX);
            polygon.setBoundingBoxRight(sourcePressX + 1);
            polygon.setBoundingBoxTop(sourcePressY);
            polygon.setBoundingBoxBottom(sourcePressY + 1);

            drawingPolygon = new RenderablePolygon(editor, polygonDelegator,
                polygon, false, true);
        }

        drawingPolygon.addNewVertex(sourcePressX, sourcePressY);
        editor.requestDraw();
    }

    @Override
    public Renderable directCaptureDraggedObject(final MouseEvent event)
    {
        if (event.getButton() == MouseButton.PRIMARY)
        {
            return drawingPolygon.getRenderablePolygonVertices().getLast();
        }
        return null;
    }

    @Override
    public boolean shouldCaptureObjectDrag(final MouseEvent event, final Renderable renderable)
    {
        return renderable instanceof RenderableVertex;
    }

    @Override
    public void onObjectDragged(final MouseEvent event, final Renderable renderable)
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
            case Q ->
            {
                event.consume();
                editor.enterEditMode(QuickSelectEditMode.class);
            }
            case DELETE ->
            {
                event.consume();
                EditorCommons.deleteSelected(editor);
            }
        }
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
            final GenericPolygon polygon = drawingPolygon.getPolygon();

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
                polygonDelegator.add(polygon);
            }
        }

        if (endMode)
        {
            editor.exitEditMode();
        }
    }

    private void cleanUpPolygonDrawingModeRenderObjects()
    {
        if (drawingPolygon == null) return;

        // Remove all renderable objects from the quadtree
        for (final RenderableVertex vertex : drawingPolygon.getRenderablePolygonVertices())
        {
            editor.removeRenderable(vertex);
        }
        editor.removeRenderable(drawingPolygon);
    }
}
