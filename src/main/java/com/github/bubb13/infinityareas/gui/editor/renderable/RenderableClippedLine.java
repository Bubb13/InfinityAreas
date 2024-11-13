
package com.github.bubb13.infinityareas.gui.editor.renderable;

import com.github.bubb13.infinityareas.gui.editor.Editor;
import com.github.bubb13.infinityareas.misc.DoubleCorners;
import com.github.bubb13.infinityareas.misc.ReadableDoublePoint;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import java.util.Collection;

public abstract class RenderableClippedLine<BackingPointType extends ReadableDoublePoint> extends AbstractRenderable
{
    //////////////////////
    // Protected Fields //
    //////////////////////

    protected final DoubleCorners corners = new DoubleCorners();
    protected BackingPointType backingPoint1;
    protected BackingPointType backingPoint2;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public RenderableClippedLine(final Editor editor)
    {
        super(editor);
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void setBackingPoints(final BackingPointType backingPoint1, final BackingPointType backingPoint2)
    {
        this.backingPoint1 = backingPoint1;
        this.backingPoint2 = backingPoint2;
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
        canvasContext.save();

        clipRectangles(canvasContext);

        canvasContext.beginPath();
        canvasContext.setStroke(getLineColor());
        canvasContext.setLineWidth(1);

        final Point2D canvasPoint1 = editor.sourceToCanvasDoublePosition(
            backingPoint1.getX(), backingPoint1.getY());

        final Point2D canvasPoint2 = editor.sourceToCanvasDoublePosition(
            backingPoint2.getX(), backingPoint2.getY());

        canvasContext.moveTo(canvasPoint1.getX(), canvasPoint1.getY());
        canvasContext.lineTo(canvasPoint2.getX(), canvasPoint2.getY());
        canvasContext.stroke();

        canvasContext.restore();
    }

    ///////////////////////
    // Protected Methods //
    ///////////////////////

    protected Collection<Rectangle2D> getCanvasExclusionRects()
    {
        return null;
    }

    protected Color getLineColor()
    {
        return Color.WHITE;
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void clipRectangles(final GraphicsContext canvasContext)
    {
        final Collection<Rectangle2D> exclusionRects = getCanvasExclusionRects();

        if (exclusionRects.isEmpty())
        {
            return;
        }

        final Bounds canvasBounds = editor.getCanvasBounds();

        javafx.scene.shape.Path drawingArea = new javafx.scene.shape.Path();
        drawingArea.setFill(Color.TRANSPARENT);
        drawingArea.getElements().add(new MoveTo(0, 0));
        drawingArea.getElements().add(new LineTo(canvasBounds.getWidth(), 0));
        drawingArea.getElements().add(new LineTo(canvasBounds.getWidth(), canvasBounds.getHeight()));
        drawingArea.getElements().add(new LineTo(0, canvasBounds.getHeight()));
        drawingArea.getElements().add(new ClosePath());

        javafx.scene.shape.Path clip = drawingArea;
        for (final Rectangle2D exclusion : exclusionRects)
        {
            clip = (javafx.scene.shape.Path) Shape.subtract(clip, new Rectangle(
                exclusion.getMinX() - 1, exclusion.getMinY() - 1,
                exclusion.getWidth() + 2, exclusion.getHeight() + 2));
        }

        canvasContext.beginPath();
        for (final PathElement e : clip.getElements())
        {
            if (e instanceof MoveTo moveTo)
            {
                canvasContext.moveTo(moveTo.getX(), moveTo.getY());
            }
            else if (e instanceof LineTo lineTo)
            {
                canvasContext.lineTo(lineTo.getX(), lineTo.getY());
            }
            else if (e instanceof ClosePath)
            {
                canvasContext.closePath();
            }
            else
            {
                throw new IllegalStateException();
            }
        }
        canvasContext.clip(); // TODO: Clipping is slow; cache result?
    }
}
