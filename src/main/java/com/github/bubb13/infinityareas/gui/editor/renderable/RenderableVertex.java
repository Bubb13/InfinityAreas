
package com.github.bubb13.infinityareas.gui.editor.renderable;

import com.github.bubb13.infinityareas.gui.editor.Editor;
import com.github.bubb13.infinityareas.gui.editor.GenericPolygon;
import com.github.bubb13.infinityareas.misc.DoubleCorners;
import com.github.bubb13.infinityareas.misc.SimpleLinkedList;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.awt.Point;

public class RenderableVertex extends AbstractRenderable
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final Editor editor;
    private final RenderablePolygon<? extends GenericPolygon> renderablePolygon;
    private final GenericPolygon.Vertex vertex;
    private final SimpleLinkedList<RenderableVertex>.Node renderableVertexNode;
    private final DoubleCorners corners = new DoubleCorners();

    private boolean selected = false;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public RenderableVertex(
        final Editor editor,
        final RenderablePolygon<? extends GenericPolygon> renderablePolygon,
        final GenericPolygon.Vertex vertex,
        final SimpleLinkedList<RenderableVertex>.Node renderableVertexNode)
    {
        this.editor = editor;
        this.renderablePolygon = renderablePolygon;
        this.vertex = vertex;
        this.renderableVertexNode = renderableVertexNode;
        recalculateCorners();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public RenderableVertex addNewVertexAfter(final int x, final int y)
    {
        final GenericPolygon.Vertex addedVertex = vertex.getNode().addAfter(
            (node) -> renderablePolygon.getPolygon().newVertex(node, x, y)).value();

        final RenderableVertex addedRenderableVertex = renderableVertexNode.addAfter(
            (node) -> new RenderableVertex(editor, renderablePolygon, addedVertex, node)).value();

        checkExpandPolygonBounds();
        renderablePolygon.updateCornersFromPolygonBounds();
        return addedRenderableVertex;
    }

    public void checkExpandPolygonBounds()
    {
        final GenericPolygon polygon = renderablePolygon.getPolygon();
        final int x = vertex.x();
        final int y = vertex.y();
        if (x < polygon.getBoundingBoxLeft()) polygon.setBoundingBoxLeft(x);
        if (y < polygon.getBoundingBoxTop()) polygon.setBoundingBoxTop(y);
        if (x > polygon.getBoundingBoxRight()) polygon.setBoundingBoxRight(x);
        if (y > polygon.getBoundingBoxBottom()) polygon.setBoundingBoxBottom(y);
    }

    public void move(final int x, final int y)
    {
        vertex.setX(x);
        vertex.setY(y);
        recalculateCorners();
        renderablePolygon.recalculateBoundsAndCornersFromVertices();
    }

    public RenderableVertex previous()
    {
        final var previousNode = renderableVertexNode.previous();
        return previousNode == null ? renderablePolygon.getLastVertex() : previousNode.value();
    }

    public RenderableVertex next()
    {
        final var nextNode = renderableVertexNode.next();
        return nextNode == null ? renderablePolygon.getFirstVertex() : nextNode.value();
    }

    public RenderablePolygon<? extends GenericPolygon> getRenderablePolygon()
    {
        return renderablePolygon;
    }

    public GenericPolygon.Vertex getVertex()
    {
        return vertex;
    }

    @Override
    public int sortWeight()
    {
        return renderablePolygon.sortWeight() + 1;
    }

    @Override
    public boolean isEnabled()
    {
        return renderablePolygon.isEnabled();
    }

    @Override
    public boolean isHidden()
    {
        return renderablePolygon.isHidden();
    }

    @Override
    public void onRender(final GraphicsContext canvasContext, final double scaleCorrection)
    {
        Color color = selected ? Color.BLUE : Color.TEAL;
        if (renderablePolygon.isDrawing() && renderableVertexNode.next() == null)
        {
            color = selected
                ? Color.rgb(0, 255, 0)
                : Color.rgb(0, 127, 0);
        }

        canvasContext.setLineWidth(1);
        canvasContext.setStroke(color);
        final Point2D p1 = editor.sourceToCanvasPosition(vertex.x() - 1, vertex.y() - 1);
        final Point2D p2 = editor.sourceToCanvasPosition(vertex.x() + 1, vertex.y() + 1);
        canvasContext.strokeRect(p1.getX(), p1.getY(), p2.getX() - p1.getX(), p2.getY() - p1.getY());
    }

    @Override
    public DoubleCorners getCorners()
    {
        return corners;
    }

    @Override
    public boolean offerPressCapture(final MouseEvent event)
    {
        return event.getButton() == MouseButton.PRIMARY;
    }

    @Override
    public void onClicked(final MouseEvent event)
    {
        if (!event.isShiftDown() && !event.isControlDown())
        {
            editor.unselectAll();
        }

        if (event.isControlDown())
        {
            if (selected)
            {
                editor.unselect(this);
            }
            else
            {
                editor.select(this);
            }
        }
        else
        {
            editor.select(this);
        }
    }

    @Override
    public boolean offerDragCapture(final MouseEvent event)
    {
        return true;
    }

    @Override
    public void onDragged(MouseEvent event)
    {
        boolean wasNotSelected = false;
        if (!selected)
        {
            wasNotSelected = true;
            editor.select(this);
        }

        final Point newSourcePos = editor.getEventSourcePosition(event);
        final int deltaX = newSourcePos.x - vertex.x();
        final int deltaY = newSourcePos.y - vertex.y();

        for (final AbstractRenderable renderable : editor.selectedObjects())
        {
            if (!(renderable instanceof RenderableVertex movingRenderableVertex))
            {
                continue;
            }

            final GenericPolygon.Vertex movingVertex = movingRenderableVertex.getVertex();
            final int newX = movingVertex.x() + deltaX;
            final int newY = movingVertex.y() + deltaY;

            movingRenderableVertex.move(newX, newY);
        }

        if (wasNotSelected)
        {
            editor.unselect(this);
        }
    }

    @Override
    public void onBeforeSelected()
    {
        selected = true;
        editor.requestDraw();
    }

    @Override
    public void onUnselected()
    {
        selected = false;
        editor.requestDraw();
    }

    @Override
    public void delete()
    {
        super.delete();
        editor.removeRenderable(this);
        renderableVertexNode.remove();
        vertex.getNode().remove();
        renderablePolygon.recalculateBoundsAndCornersFromVertices();
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void recalculateCorners()
    {
        final int x = vertex.x();
        final int y = vertex.y();
        corners.setTopLeftX(x - 1);
        corners.setTopLeftY(y - 1);
        corners.setBottomRightExclusiveX(x + 1);
        corners.setBottomRightExclusiveY(y + 1);
        editor.addRenderable(this);
    }
}
