
package com.github.bubb13.infinityareas.gui.editor.renderable;

import com.github.bubb13.infinityareas.gui.editor.Editor;
import com.github.bubb13.infinityareas.gui.editor.GenericPolygon;
import com.github.bubb13.infinityareas.misc.DoubleCorners;
import com.github.bubb13.infinityareas.misc.SimpleLinkedList;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;

public abstract class RenderablePolygon<PolygonType extends GenericPolygon> extends AbstractRenderable
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final Editor editor;
    private final PolygonType polygon;
    private final SimpleLinkedList<RenderableVertex> renderableVertices = new SimpleLinkedList<>();
    private final DoubleCorners corners = new DoubleCorners();

    private boolean renderFill = false;
    private boolean renderImpliedFinalLine = true;
    private boolean drawing = false;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public RenderablePolygon(final Editor editor, final PolygonType polygon)
    {
        this.editor = editor;
        this.polygon = polygon;

        for (final GenericPolygon.Vertex vertex : polygon.getVertices())
        {
            addNewRenderableVertexInternal(vertex);
        }

        updateCornersFromPolygonBounds();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public RenderableVertex addNewVertex(final int x, final int y)
    {
        final RenderableVertex newVertex = addNewRenderableVertexInternal(polygon.addVertex(x, y));
        updateCornersFromPolygonBounds();
        return newVertex;
    }

    public void recalculateBoundsAndCornersFromVertices()
    {
        int topLeftX = Integer.MAX_VALUE;
        int topLeftY = Integer.MAX_VALUE;
        int bottomRightExclusiveX = Integer.MIN_VALUE;
        int bottomRightExclusiveY = Integer.MIN_VALUE;

        // TODO: Potentially expensive
        for (final RenderableVertex renderableVertex : renderableVertices)
        {
            final GenericPolygon.Vertex vertex = renderableVertex.getVertex();
            final int x = vertex.x();
            final int y = vertex.y();
            if (x < topLeftX) topLeftX = x;
            if (y < topLeftY) topLeftY = y;
            if (x > bottomRightExclusiveX) bottomRightExclusiveX = x;
            if (y > bottomRightExclusiveY) bottomRightExclusiveY = y;
        }

        polygon.setBoundingBoxLeft(topLeftX);
        polygon.setBoundingBoxRight(bottomRightExclusiveX);
        polygon.setBoundingBoxTop(topLeftY);
        polygon.setBoundingBoxBottom(bottomRightExclusiveY);
        updateCornersFromPolygonBounds();
    }

    public void updateCornersFromPolygonBounds()
    {
        corners.setTopLeftX(polygon.getBoundingBoxLeft());
        corners.setTopLeftY(polygon.getBoundingBoxTop());
        corners.setBottomRightExclusiveX(polygon.getBoundingBoxRight());
        corners.setBottomRightExclusiveY(polygon.getBoundingBoxBottom());
        editor.addRenderable(this);
    }

    public RenderableVertex getFirstVertex()
    {
        return renderableVertices.getFirst();
    }

    public RenderableVertex getLastVertex()
    {
        return renderableVertices.getLast();
    }

    public PolygonType getPolygon()
    {
        return polygon;
    }

    public SimpleLinkedList<RenderableVertex> getRenderablePolygonVertices()
    {
        return renderableVertices;
    }

    public void setDrawing(final boolean drawing)
    {
        this.drawing = drawing;
    }

    public boolean isDrawing()
    {
        return drawing;
    }

    public void setRenderImpliedFinalLine(boolean renderImpliedFinalLine)
    {
        this.renderImpliedFinalLine = renderImpliedFinalLine;
    }

    public void setRenderFill(boolean renderFill)
    {
        this.renderFill = renderFill;
    }

    @Override
    public boolean isEnabled()
    {
        return true;
    }

    @Override
    public void onRender(final GraphicsContext canvasContext)
    {
        if (renderFill)
        {
            renderFill(canvasContext);
        }

        renderOutline(canvasContext);
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
    public boolean contains(final Point2D point)
    {
        return polygon.contains(point.getX(), point.getY());
    }

    @Override
    public void delete()
    {
        for (final RenderableVertex renderable : renderableVertices)
        {
            editor.removeRenderable(renderable);
        }
        editor.removeRenderable(this);
        deleteBackingObject();
        editor.requestDraw();
    }

    ///////////////////////
    // Protected Methods //
    ///////////////////////

    protected Color getLineColor()
    {
        return Color.WHITE;
    }

    ////////////////////////////////
    // Protected Abstract Methods //
    ////////////////////////////////

    protected abstract void deleteBackingObject();

    /////////////////////
    // Private Methods //
    /////////////////////

    private RenderableVertex addNewRenderableVertexInternal(final GenericPolygon.Vertex vertex)
    {
        final RenderableVertex newRenderableVertex = renderableVertices.addTail((node)
            -> new RenderableVertex(editor, this, vertex, node)).value();

        newRenderableVertex.checkExpandPolygonBounds();
        return newRenderableVertex;
    }

    private void renderLine(
        final GraphicsContext canvasContext, final GenericPolygon.Vertex v1, final GenericPolygon.Vertex v2)
    {
        final Point2D p1 = editor.sourceToAbsoluteCanvasPosition(v1.x(), v1.y());
        final Point2D p2 = editor.sourceToAbsoluteCanvasPosition(v2.x(), v2.y());
        canvasContext.strokeLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    private void renderFill(final GraphicsContext canvasContext)
    {
        final Color lineColor = getLineColor();
        final Color fillColor = Color.color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 0.25);

        final Bounds canvasBounds = editor.getCanvasBounds();
        final double zoomFactor = editor.getZoomFactor();

        final Affine savedAffine = canvasContext.getTransform();

        canvasContext.translate(-canvasBounds.getMinX(), -canvasBounds.getMinY());
        canvasContext.scale(zoomFactor, zoomFactor);

        canvasContext.setFill(fillColor);
        canvasContext.fillPolygon(polygon.xArray(), polygon.yArray(), polygon.getVertices().size());

        canvasContext.setTransform(savedAffine);
    }

    private void renderOutline(final GraphicsContext canvasContext)
    {
        final SimpleLinkedList<GenericPolygon.Vertex> vertices = polygon.getVertices();
        final int limit = vertices.size() - 1;

        canvasContext.setLineWidth(1D);
        canvasContext.setStroke(getLineColor());

        var curNode = vertices.getFirstNode();
        for (int i = 0; i < limit; ++i)
        {
            final var nextNode = curNode.next();
            renderLine(canvasContext, curNode.value(), nextNode.value());
            curNode = nextNode;
        }

        if (renderImpliedFinalLine)
        {
            final GenericPolygon.Vertex vFirst = vertices.getFirst();
            final GenericPolygon.Vertex vLast = vertices.getLast();
            renderLine(canvasContext, vFirst, vLast);
        }

//        final Bounds canvasBounds = editor.getCanvasBounds();
//        final double zoomFactor = editor.getZoomFactor();
//
//        final Affine savedAffine = canvasContext.getTransform();
//
//        canvasContext.setLineWidth(1 / zoomFactor);
//        canvasContext.setStroke(getLineColor());
//
//        canvasContext.translate(-canvasBounds.getMinX(), -canvasBounds.getMinY());
//        canvasContext.scale(zoomFactor, zoomFactor);
//
//        if (renderImpliedFinalLine)
//        {
//            canvasContext.strokePolygon(polygon.xArray(), polygon.yArray(), polygon.getVertices().size());
//        }
//        else
//        {
//            canvasContext.strokePolyline(polygon.xArray(), polygon.yArray(), polygon.getVertices().size());
//        }
//
//        canvasContext.setTransform(savedAffine);
    }
}
