
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

    protected final Editor editor;
    protected final DoubleCorners corners = new DoubleCorners();
    protected final BackingObjectType backingObject;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public RenderablePoint(final Editor editor, final BackingObjectType backingObject)
    {
        this.editor = editor;
        this.backingObject = backingObject;
        recalculateCorners();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

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
    public void onRender(final GraphicsContext canvasContext)
    {
        final Point2D canvasPointTopLeft = editor.sourceToAbsoluteCanvasDoublePosition(
            corners.topLeftX(), corners.topLeftY());

        final Point2D canvasPointBottomRight = editor.sourceToAbsoluteCanvasDoublePosition(
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
        editor.requestDraw();
    }

    @Override
    public void delete()
    {
        editor.removeRenderable(this);
        deleteBackingObject();
        editor.requestDraw();
    }

    ///////////////////////
    // Protected Methods //
    ///////////////////////

    protected void recalculateCorners()
    {
        final int sourceX = backingObject.getX();
        final int sourceY = backingObject.getY();
        corners.setTopLeftX(sourceX - 1);
        corners.setTopLeftY(sourceY - 1);
        corners.setBottomRightExclusiveX(sourceX + 1);
        corners.setBottomRightExclusiveY(sourceY + 1);
        editor.addRenderable(this);
    }

    protected Color getLineColor()
    {
        return Color.WHITE;
    }

    protected void deleteBackingObject() {}
}
