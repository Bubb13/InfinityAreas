
package com.github.bubb13.infinityareas.gui.editor.renderable;

import com.github.bubb13.infinityareas.gui.editor.Editor;
import com.github.bubb13.infinityareas.misc.DoubleCorners;
import com.github.bubb13.infinityareas.misc.ReadableDoublePoint;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public abstract class RenderableAnchoredLine<BackingPointType extends ReadableDoublePoint> extends AbstractRenderable
{
    //////////////////////
    // Protected Fields //
    //////////////////////

    protected final Editor editor;
    protected final DoubleCorners corners = new DoubleCorners();
    protected BackingPointType backingPoint1;
    protected BackingPointType backingPoint2;
    protected DoubleCorners anchor1;
    protected DoubleCorners anchor2;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public RenderableAnchoredLine(final Editor editor)
    {
        this.editor = editor;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void setBackingObjects(
        final BackingPointType backingPoint1, final BackingPointType backingPoint2,
        final DoubleCorners anchor1, final DoubleCorners anchor2)
    {
        this.backingPoint1 = backingPoint1;
        this.backingPoint2 = backingPoint2;
        this.anchor1 = anchor1;
        this.anchor2 = anchor2;
        recalculateCorners();
    }

    public void recalculateCorners()
    {
        final double x1 = backingPoint1.getX();
        final double y1 = backingPoint1.getY();
        final double x2 = backingPoint2.getX();
        final double y2 = backingPoint2.getY();
        corners.setTopLeftX(Math.min(x1, x2));
        corners.setTopLeftY(Math.min(y1, y2));
        corners.setBottomRightExclusiveX(Math.max(x1, x2) + 1);
        corners.setBottomRightExclusiveY(Math.max(y1, y2) + 1);
        editor.addRenderable(this);
    }

    @Override
    public DoubleCorners getCorners()
    {
        return corners;
    }

    @Override
    public boolean isEnabled()
    {
        return true;
    }

    @Override
    public void onRender(final GraphicsContext canvasContext, final double scaleCorrection)
    {
        final double x1 = backingPoint1.getX();
        final double y1 = backingPoint1.getY();
        final double x2 = backingPoint2.getX();
        final double y2 = backingPoint2.getY();

        final Point2D p1 = getIntersection(x1, y1, x2, y2, anchor1);
        if (p1 == null) return;

        final Point2D p2 = getIntersection(x1, y1, x2, y2, anchor2);
        if (p2 == null) return;

        final Point2D canvasP1 = editor.sourceToCanvasDoublePosition(p1.getX(), p1.getY());
        final Point2D canvasP2 = editor.sourceToCanvasDoublePosition(p2.getX(), p2.getY());

        // Hack so that the line doesn't render over the second anchor
        final int xAdj = canvasP2.getX() < canvasP1.getX() ? 1 : -1;
        final int yAdj = canvasP2.getY() < canvasP1.getY() ? 1 : -1;

        canvasContext.setLineWidth(1);
        canvasContext.setStroke(getLineColor());
        canvasContext.strokeLine(
            canvasP1.getX(), canvasP1.getY(),
            canvasP2.getX() + xAdj,
            canvasP2.getY() + yAdj
        );
    }

    ///////////////////////
    // Protected Methods //
    ///////////////////////

    protected Color getLineColor()
    {
        return Color.WHITE;
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private static Point2D getIntersection(
        final double pX1, final double pY1, final double pX2, final double pY2, final DoubleCorners corners)
    {
        Point2D intersection;

        // Top Line
        final double x1 = corners.topLeftX();
        final double y1 = corners.topLeftY();
        final double x2 = corners.bottomRightExclusiveX();
        final double y2 = corners.topLeftY();
        intersection = getIntersectionPoint(pX1, pY1, pX2, pY2, x1, y1, x2, y2);
        if (intersection != null) return intersection;

        // Bottom Line
        final double x3 = corners.topLeftX();
        final double y3 = corners.bottomRightExclusiveY();
        final double x4 = corners.bottomRightExclusiveX();
        final double y4 = corners.bottomRightExclusiveY();
        intersection = getIntersectionPoint(pX1, pY1, pX2, pY2, x3, y3, x4, y4);
        if (intersection != null) return intersection;

        // Left Line
        final double x5 = corners.topLeftX();
        final double y5 = corners.topLeftY();
        final double x6 = corners.topLeftX();
        final double y6 = corners.bottomRightExclusiveY();
        intersection = getIntersectionPoint(pX1, pY1, pX2, pY2, x5, y5, x6, y6);
        if (intersection != null) return intersection;

        // Bottom Line
        final double x7 = corners.bottomRightExclusiveX();
        final double y7 = corners.topLeftY();
        final double x8 = corners.bottomRightExclusiveX();
        final double y8 = corners.bottomRightExclusiveY();
        intersection = getIntersectionPoint(pX1, pY1, pX2, pY2, x7, y7, x8, y8);
        return intersection;
    }

    private static Point2D getIntersectionPoint(
        // Line 1
        final double x1, final double y1,
        final double x2, final double y2,
        // Line 2
        final double x3, final double y3,
        final double x4, final double y4)
    {
        final double denominator = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (denominator == 0) return null;

        final double ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denominator;
        final double ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denominator;

        if (ua >= 0 && ua <= 1 && ub >= 0 && ub <= 1)
        {
            final double x = x1 + ua * (x2 - x1);
            final double y = y1 + ua * (y2 - y1);
            return new Point2D(x, y);
        }

        return null;
    }
}
