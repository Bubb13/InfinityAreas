
package com.github.bubb13.infinityareas.gui.editor;

import com.github.bubb13.infinityareas.misc.Corners;
import com.github.bubb13.infinityareas.misc.SimpleLinkedList;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.awt.Point;

public class RenderableVertex extends AbstractRenderable
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final Editor editor;
    private final RenderablePolygon renderablePolygon;
    private final GenericVertex vertex;
    private final SimpleLinkedList<RenderableVertex>.Node renderableVertexNode;
    private final Corners corners = new Corners();

    private boolean selected = false;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public RenderableVertex(
        final Editor editor,
        final RenderablePolygon renderablePolygon,
        final GenericVertex vertex,
        final SimpleLinkedList<RenderableVertex>.Node renderableVertexNode)
    {
        this.editor = editor;
        this.renderablePolygon = renderablePolygon;
        this.vertex = vertex;
        this.renderableVertexNode = renderableVertexNode;
        calculateCorners();
        editor.addRenderable(this);
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public RenderableVertex addNewVertexAfter(final int x, final int y)
    {
        final GenericVertex addedVertex = vertex.getNode().addAfter(
            (node)-> new GenericVertex(node, x, y)).value();

        final RenderableVertex addedRenderableVertex = renderableVertexNode.addAfter(
            (node) -> new RenderableVertex(editor, renderablePolygon, addedVertex, node)).value();

        addedRenderableVertex.recalculatePolygonCorners();
        return addedRenderableVertex;
    }

    public void recalculatePolygonCorners()
    {
        final int x = vertex.x();
        final int y = vertex.y();

        final GenericPolygon polygon = renderablePolygon.getPolygon();
        if (x < polygon.getBoundingBoxLeft()) polygon.setBoundingBoxLeft(x);
        if (y < polygon.getBoundingBoxTop()) polygon.setBoundingBoxTop(y);
        if (x > polygon.getBoundingBoxRight()) polygon.setBoundingBoxRight(x);
        if (y > polygon.getBoundingBoxBottom()) polygon.setBoundingBoxBottom(y);

        renderablePolygon.recalculateCorners();
    }

    public void move(final int x, final int y)
    {
        vertex.setX(x);
        vertex.setY(y);
        calculateCorners();
        editor.addRenderable(this);
        recalculatePolygonCorners();
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

    public RenderablePolygon getRenderablePolygon()
    {
        return renderablePolygon;
    }

    public GenericVertex getVertex()
    {
        return vertex;
    }

    @Override
    public boolean isEnabled()
    {
        return renderablePolygon.getPolygonDelegator().enabled();
    }

    @Override
    public void render(final GraphicsContext canvasContext)
    {
        Color color = selected ? Color.BLUE : Color.TEAL;
        if (renderablePolygon.isDrawing() && renderableVertexNode.next() == null)
        {
            color = selected
                ? Color.rgb(0, 255, 0)
                : Color.rgb(0, 127, 0);
        }

        canvasContext.setStroke(color);
        final Point2D p1 = editor.sourceToAbsoluteCanvasPosition(vertex.x() - 1, vertex.y() - 1);
        final Point2D p2 = editor.sourceToAbsoluteCanvasPosition(vertex.x() + 1, vertex.y() + 1);
        canvasContext.strokeRect(p1.getX(), p1.getY(), p2.getX() - p1.getX(), p2.getY() - p1.getY());
    }

    @Override
    public Corners getCorners()
    {
        return corners;
    }

    @Override
    public void clicked(final MouseEvent event)
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

        for (final Renderable renderable : editor.selectedObjects())
        {
            if (!(renderable instanceof RenderableVertex movingRenderableVertex))
            {
                continue;
            }

            final GenericVertex movingVertex = movingRenderableVertex.getVertex();
            final int newX = movingVertex.x() + deltaX;
            final int newY = movingVertex.y() + deltaY;

            movingRenderableVertex.move(newX, newY);
            editor.requestDraw();
        }

        if (wasNotSelected)
        {
            editor.unselect(this);
        }
    }

    @Override
    public void selected()
    {
        selected = true;
        editor.requestDraw();
    }

    @Override
    public void unselected()
    {
        selected = false;
        editor.requestDraw();
    }

    @Override
    public void delete()
    {
        editor.removeRenderable(this);
        renderableVertexNode.remove();
        vertex.getNode().remove();
        editor.requestDraw();
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void calculateCorners()
    {
        final int x = vertex.x();
        final int y = vertex.y();
        corners.setTopLeftX(x - 1);
        corners.setTopLeftY(y - 1);
        corners.setBottomRightExclusiveX(x + 1);
        corners.setBottomRightExclusiveY(y + 1);
    }
}
