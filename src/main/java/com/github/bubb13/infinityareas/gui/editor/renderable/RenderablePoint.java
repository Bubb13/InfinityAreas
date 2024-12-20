
package com.github.bubb13.infinityareas.gui.editor.renderable;

import com.github.bubb13.infinityareas.gui.editor.Editor;
import com.github.bubb13.infinityareas.misc.DoubleCorners;
import com.github.bubb13.infinityareas.misc.IntPoint;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

public class RenderablePoint<BackingObjectType extends IntPoint> extends AbstractRenderable
{
    //////////////////////
    // Protected Fields //
    //////////////////////

    protected final DoubleCorners corners = new DoubleCorners();
    protected BackingObjectType backingObject;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public RenderablePoint(final Editor editor)
    {
        super(editor);
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void setBackingObject(final BackingObjectType backingObject)
    {
        this.backingObject = backingObject;
        recalculateCorners();
    }

    public void recalculateCorners()
    {
        final int sourceX = backingObject.getX();
        final int sourceY = backingObject.getY();
        corners.setTopLeftX(sourceX - 1);
        corners.setTopLeftY(sourceY - 1);
        corners.setBottomRightExclusiveX(sourceX + 1);
        corners.setBottomRightExclusiveY(sourceY + 1);
        editor.addRenderable(this);
    }

    @Override
    public boolean isEnabled()
    {
        return true;
    }

    @Override
    public DoubleCorners getCorners()
    {
        return corners;
    }

    @Override
    public void onRender(final GraphicsContext canvasContext, final double scaleCorrection)
    {
        final Point2D canvasPointTopLeft = editor.sourceToCanvasDoublePosition(
            corners.topLeftX(), corners.topLeftY());

        final Point2D canvasPointBottomRight = editor.sourceToCanvasDoublePosition(
            corners.bottomRightExclusiveX(), corners.bottomRightExclusiveY());

        canvasContext.setLineWidth(1);
        canvasContext.setStroke(getLineColor());
        canvasContext.strokeRect(
            canvasPointTopLeft.getX(), canvasPointTopLeft.getY(),
            canvasPointBottomRight.getX() - canvasPointTopLeft.getX(),
            canvasPointBottomRight.getY() - canvasPointTopLeft.getY()
        );
    }

    @Override
    public void onDragged(final MouseEvent event)
    {
        final double x = event.getX();
        final double y = event.getY();
        final Point2D sourcePoint = editor.absoluteCanvasToSourceDoublePosition(x, y);
        backingObject.setX((int)Math.round(sourcePoint.getX()));
        backingObject.setY((int)Math.round(sourcePoint.getY()));
        recalculateCorners();
    }

    @Override
    public void delete()
    {
        super.delete();
        editor.removeRenderable(this, false);
    }

    ///////////////////////
    // Protected Methods //
    ///////////////////////

    protected Color getLineColor()
    {
        return Color.WHITE;
    }
}
