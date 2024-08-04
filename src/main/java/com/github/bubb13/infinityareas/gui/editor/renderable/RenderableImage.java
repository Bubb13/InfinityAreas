
package com.github.bubb13.infinityareas.gui.editor.renderable;

import com.github.bubb13.infinityareas.gui.editor.Editor;
import com.github.bubb13.infinityareas.gui.region.PartiallyRenderedImageLogic;
import com.github.bubb13.infinityareas.misc.DoubleCorners;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;

import java.awt.image.BufferedImage;

public class RenderableImage extends AbstractRenderable
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final Editor editor;
    private final DoubleCorners corners = new DoubleCorners();
    private final PartiallyRenderedImageLogic logic = new PartiallyRenderedImageLogic();

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public RenderableImage(
        final Editor editor, final BufferedImage image,
        final double x, final double y, final double width, final double height, final double opacity)
    {
        this.editor = editor;
        logic.setSourceImage(image);
        logic.setOpacity(opacity);
        setRectangle(x, y, width, height);
    }

    public RenderableImage(
        final Editor editor, final BufferedImage image,
        final double x, final double y, final double width, final double height)
    {
        this(editor, image, x, y, width, height, 1);
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void setRectangle(final double x, final double y, final double width, final double height)
    {
        corners.setTopLeftX(x);
        corners.setTopLeftY(y);
        corners.setBottomRightExclusiveX(x + width);
        corners.setBottomRightExclusiveY(y + height);
        recalculateSourceScaleFactor();
        editor.addRenderable(this);
    }

    public void setOpacity(final double opacity)
    {
        logic.setOpacity(opacity);
        editor.requestDraw();
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
        final double topLeftX = corners.topLeftX();
        final double topLeftY = corners.topLeftY();
        final double bottomRightExclusiveX = corners.bottomRightExclusiveX();
        final double bottomRightExclusiveY = corners.bottomRightExclusiveY();

        final Point2D canvasTopLeft = editor.sourceToCanvasDoublePosition(topLeftX, topLeftY);
        final Point2D canvasBottomRightExclusive = editor.sourceToCanvasDoublePosition(
            bottomRightExclusiveX, bottomRightExclusiveY);

        double canvasTopLeftX = canvasTopLeft.getX();
        double canvasTopLeftY = canvasTopLeft.getY();
        double canvasBottomRightExclusiveX = canvasBottomRightExclusive.getX();
        double canvasBottomRightExclusiveY = canvasBottomRightExclusive.getY();

        double srcX = 0;
        double srcY = 0;

        if (canvasTopLeftX < 0)
        {
            // Handle image top-left-x being offscreen
            srcX = -canvasTopLeftX;
            canvasTopLeftX = 0;
        }

        if (canvasTopLeftY < 0)
        {
            // Handle image top-left-y being offscreen
            srcY = -canvasTopLeftY;
            canvasTopLeftY = 0;
        }

        final Bounds canvasBounds = editor.getCanvasBounds();

        // Handle image bottom-right-x being offscreen
        canvasBottomRightExclusiveX = Math.min(canvasBottomRightExclusiveX, canvasBounds.getWidth());

        // Handle image bottom-right-y being offscreen
        canvasBottomRightExclusiveY = Math.min(canvasBottomRightExclusiveY, canvasBounds.getHeight());

        final double dstWidth = canvasBottomRightExclusiveX - canvasTopLeftX;
        final double dstHeight = canvasBottomRightExclusiveY - canvasTopLeftY;

        logic.draw(canvasContext,
            srcX, srcY,
            canvasTopLeftX, canvasTopLeftY,
            dstWidth, dstHeight
        );
    }

    @Override
    public boolean listensToZoomFactorChanges()
    {
        return true;
    }

    @Override
    public void onZoomFactorChanged(final double zoomFactor)
    {
        recalculateSourceScaleFactor();
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void recalculateSourceScaleFactor()
    {
        final double width = corners.bottomRightExclusiveX() - corners.topLeftX();
        final double height = corners.bottomRightExclusiveY() - corners.topLeftY();

        final BufferedImage sourceImage = logic.getSourceImage();
        final double stretchFactorX = width / sourceImage.getWidth();
        final double stretchFactorY = height / sourceImage.getHeight();

        final double zoomFactor = editor.getZoomFactor();
        final double srcScaleFactorX = zoomFactor * stretchFactorX;
        final double srcScaleFactorY = zoomFactor * stretchFactorY;

        logic.setSrcScaleFactor(srcScaleFactorX, srcScaleFactorY);
    }
}
