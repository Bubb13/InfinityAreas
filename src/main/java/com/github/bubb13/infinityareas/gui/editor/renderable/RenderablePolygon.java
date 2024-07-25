
package com.github.bubb13.infinityareas.gui.editor.renderable;

import com.github.bubb13.infinityareas.gui.editor.Editor;
import com.github.bubb13.infinityareas.gui.editor.GenericPolygon;
import com.github.bubb13.infinityareas.gui.editor.GenericVertex;
import com.github.bubb13.infinityareas.misc.DoubleCorners;
import com.github.bubb13.infinityareas.misc.SimpleLinkedList;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class RenderablePolygon extends AbstractRenderable
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final Editor editor;
    private final GenericPolygon polygon;
    private final SimpleLinkedList<RenderableVertex> renderableVertices = new SimpleLinkedList<>();
    private final DoubleCorners corners = new DoubleCorners();

    private boolean renderFill = false;
    private boolean renderImpliedFinalLine = true;
    private boolean drawing = false;

    private double[] fillXPointsArray;
    private double[] fillYPointsArray;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public RenderablePolygon(final Editor editor, final GenericPolygon polygon)
    {
        this.editor = editor;
        this.polygon = polygon;

        for (final GenericVertex vertex : polygon.getVertices())
        {
            addNewRenderableVertex(vertex);
        }

        calculateCorners();
        editor.addRenderable(this);
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public RenderableVertex addNewVertex(final int x, final int y)
    {
        return addNewRenderableVertex(polygon.addVertex(x, y));
    }

    public void recalculateCorners()
    {
        calculateCorners();
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

    public GenericPolygon getPolygon()
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

    protected void deleteBackingObject() {}

    /////////////////////
    // Private Methods //
    /////////////////////

    private RenderableVertex addNewRenderableVertex(final GenericVertex vertex)
    {
        final RenderableVertex newRenderableVertex = renderableVertices.addTail((node)
            -> new RenderableVertex(editor, this, vertex, node)).value();

        newRenderableVertex.recalculatePolygonCorners();
        return newRenderableVertex;
    }

    private void calculateCorners()
    {
        corners.setTopLeftX(polygon.getBoundingBoxLeft());
        corners.setTopLeftY(polygon.getBoundingBoxTop());
        corners.setBottomRightExclusiveX(polygon.getBoundingBoxRight());
        corners.setBottomRightExclusiveY(polygon.getBoundingBoxBottom());
    }

    private void renderLine(final GraphicsContext canvasContext, final GenericVertex v1, final GenericVertex v2)
    {
        final Point2D p1 = editor.sourceToAbsoluteCanvasPosition(v1.x(), v1.y());
        final Point2D p2 = editor.sourceToAbsoluteCanvasPosition(v2.x(), v2.y());
        canvasContext.strokeLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    private void renderFill(final GraphicsContext canvasContext)
    {
        final SimpleLinkedList<GenericVertex> vertices = polygon.getVertices();
        if (fillXPointsArray == null || fillXPointsArray.length != vertices.size())
        {
            fillXPointsArray = new double[vertices.size()];
            fillYPointsArray = new double[vertices.size()];
        }

        int i = 0;
        for (final RenderableVertex renderableVertex : renderableVertices)
        {
            final GenericVertex vertex = renderableVertex.getVertex();
            final Point2D canvasPos = editor.sourceToAbsoluteCanvasDoublePosition(vertex.x(), vertex.y());
            fillXPointsArray[i] = canvasPos.getX();
            fillYPointsArray[i] = canvasPos.getY();
            ++i;
        }

        final Color lineColor = getLineColor();
        final Color fillColor = Color.color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 0.25);

        canvasContext.setFill(fillColor);
        canvasContext.fillPolygon(fillXPointsArray, fillYPointsArray, vertices.size());
    }

    private void renderOutline(final GraphicsContext canvasContext)
    {
        final SimpleLinkedList<GenericVertex> vertices = polygon.getVertices();
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
            final GenericVertex vFirst = vertices.getFirst();
            final GenericVertex vLast = vertices.getLast();
            renderLine(canvasContext, vFirst, vLast);
        }
    }
}
