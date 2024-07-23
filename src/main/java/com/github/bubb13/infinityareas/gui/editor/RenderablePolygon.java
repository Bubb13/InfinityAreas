
package com.github.bubb13.infinityareas.gui.editor;

import com.github.bubb13.infinityareas.misc.Corners;
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
    private final PolygonDelegator polygonDelegator;
    private final GenericPolygon polygon;
    private final SimpleLinkedList<RenderableVertex> renderableVertices = new SimpleLinkedList<>();
    private final Corners corners = new Corners();

    private boolean renderImpliedFinalLine;
    private boolean drawing = false;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public RenderablePolygon(
        final Editor editor, final PolygonDelegator polygonDelegator, final GenericPolygon polygon,
        final boolean renderImpliedFinalLine, final boolean drawing)
    {
        this.editor = editor;
        this.polygonDelegator = polygonDelegator;
        this.polygon = polygon;

        for (final GenericVertex vertex : polygon.getVertices())
        {
            addNewRenderableVertex(vertex);
        }

        this.renderImpliedFinalLine = renderImpliedFinalLine;
        this.drawing = drawing;
        calculateCorners();
        editor.addRenderable(this);
    }

    public RenderablePolygon(
        final Editor editor, final PolygonDelegator polygonDelegator, final GenericPolygon polygon)
    {
        this(editor, polygonDelegator, polygon, true, false);
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

    public PolygonDelegator getPolygonDelegator()
    {
        return polygonDelegator;
    }

    @Override
    public boolean isEnabled()
    {
        return polygonDelegator.enabled();
    }

    @Override
    public void render(final GraphicsContext canvasContext)
    {
        final SimpleLinkedList<GenericVertex> vertices = polygon.getVertices();
        final int limit = vertices.size() - 1;

        canvasContext.setLineWidth(1D);
        canvasContext.setStroke(Color.WHITE);

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

    @Override
    public Corners getCorners()
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
        polygonDelegator.delete(polygon);
        editor.requestDraw();
    }

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
}
