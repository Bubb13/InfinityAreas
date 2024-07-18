
package com.github.bubb13.infinityareas.gui.region;

import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.gui.misc.VisibleNotifiable;
import com.github.bubb13.infinityareas.misc.Corners;
import com.github.bubb13.infinityareas.util.ImageUtil;
import javafx.animation.AnimationTimer;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * This class has been carefully designed to implement a custom scaling algorithm (nearest neighbor) while
 * avoiding constant memory allocations. The only way to draw pixels to a GraphicsContext object without
 * causing an (allocate + copy) of the buffer to be performed is to draw a WritableImage. <p>
 *
 * Some considerations: <p>
 *
 *   1) WritableImage instances hide their internal buffer. The only way to modify their pixels is via pixel writers.
 *      Writing pixels to a WritableImage instance one-by-one in this way is really slow. Thus, scaling operations
 *      save their result to an intermediate buffer, and this buffer is then written to a WritableImage instance. <p>
 *
 *   2) WritableImage instances always convert pixel data to the native format of the system. Thus, writing pixels
 *      that exist in a non-native format is also really slow. To mitigate this, the source image is converted to
 *      the correct format when it is initially set. <p>
 *
 *   3) Due to the asynchronous nature of the GraphicsContext object, multiple updates might be performed before a
 *      render pulse occurs. Therefore, multiple WritableImages might be created in order to queue multiple draws. <p>
 */
public class PartiallyRenderedImage extends Region implements VisibleNotifiable
{
    private final Canvas canvas = new Canvas();
    private final GraphicsContext graphics = canvas.getGraphicsContext2D();

    /**
     * Since GraphicsContext::drawImage() executes some time in the future, the passed image cannot be
     * altered until after the next render pulse. This ArrayList, along with `toDrawImageIndex`, tracks
     * which cached WritableImage can be used to draw the next update. `toDrawImageIndex` is incremented
     * after every draw, and is reset to 0 on a render pulse. New WritableImage objects are allocated
     * as needed; they are never freed.
     */
    private final ArrayList<WritableImage> toDrawImages = new ArrayList<>();

    private BufferedImage sourceImage;
    private double zoomFactor = 1;
    private Consumer<GraphicsContext> drawCallback;

    private byte[] scaleBuffer;
    private int curToDrawImagesWidth = 0;
    private int curToDrawImagesHeight = 0;
    private int toDrawImageIndex = 0;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public PartiallyRenderedImage()
    {
        getChildren().add(canvas);

        new AnimationTimer()
        {
            @Override
            public void handle(final long now)
            {
                onRenderPulse();
            }
        }.start();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void setSourceImage(final BufferedImage sourceImage)
    {
        if (sourceImage.getType() == BufferedImage.TYPE_INT_ARGB)
        {
            this.sourceImage = ImageUtil.convertArgbToBgraPre(sourceImage);
        }
        else
        {
            throw new UnsupportedOperationException();
        }

        requestLayout();
    }

    public BufferedImage getSourceImage()
    {
        return sourceImage;
    }

    public void setZoomFactor(double zoomFactor)
    {
        this.zoomFactor = zoomFactor;
        requestLayout();
    }

    public GraphicsContext getGraphics()
    {
        return graphics;
    }

    public void setDrawCallback(final Consumer<GraphicsContext> drawCallback)
    {
        this.drawCallback = drawCallback;
    }

    public Point2D sourceToAbsoluteCanvasPosition(final int srcX, final int srcY)
    {
        final Bounds layout = canvas.getBoundsInParent();
        final int renderX = (int)layout.getMinX();
        final int renderY = (int)layout.getMinY();
        return new Point2D((int)(srcX * zoomFactor - renderX), (int)(srcY * zoomFactor - renderY));
    }

    public Point2D absoluteToRelativeCanvasPosition(final int canvasX, final int canvasY)
    {
        final Bounds layout = canvas.getBoundsInParent();
        final int renderX = (int)layout.getMinX();
        final int renderY = (int)layout.getMinY();
        return new Point2D(canvasX - renderX, canvasY - renderY);
    }

    public Point absoluteCanvasToSourcePosition(final int canvasX, final int canvasY)
    {
        return new Point((int)(canvasX / zoomFactor), (int)(canvasY / zoomFactor));
    }

    public Rectangle2D getVisibleSourceRect()
    {
        final Bounds layout = canvas.getBoundsInParent();
        final int renderX = (int)layout.getMinX();
        final int renderY = (int)layout.getMinY();
        final int renderW = (int)layout.getWidth();
        final int renderH = (int)layout.getHeight();
        return new Rectangle2D(
            (int)(renderX / zoomFactor), (int)(renderY / zoomFactor),
            (int)(renderW / zoomFactor), (int)(renderH / zoomFactor)
        );
    }

    public Corners getVisibleSourceCorners()
    {
        final Bounds layout = canvas.getBoundsInParent();
        final int renderTopLeftX = (int)layout.getMinX();
        final int renderTopLeftY = (int)layout.getMinY();
        final int renderBottomRightExclusiveX = (int)layout.getMaxX();
        final int renderBottomRightExclusiveY = (int)layout.getMaxY();
        return new Corners(
            (int)(renderTopLeftX / zoomFactor), (int)(renderTopLeftY / zoomFactor),
            (int)(renderBottomRightExclusiveX / zoomFactor), (int)(renderBottomRightExclusiveY / zoomFactor)
        );
    }

    @Override
    public void notifyVisible(final Bounds bounds)
    {
        if (bounds.isEmpty())
        {
            return;
        }

        final double minX = bounds.getMinX();
        final double minY = bounds.getMinY();
        final int snappedMinX = (int)minX;
        final int snappedMinY = (int)minY;
        final double extraW = minX - snappedMinX;
        final double extraH = minY - snappedMinY;
        final int snappedWidth = (int)Math.ceil(bounds.getWidth() + extraW);
        final int snappedHeight = (int)Math.ceil(bounds.getHeight() + extraH);

        // If a viewport dimension is greater than the currently cached dimensions,
        // grow the scale buffer and clear the drawing images (to be reallocated later).
        if (snappedWidth > curToDrawImagesWidth || snappedHeight > curToDrawImagesHeight)
        {
            if (snappedWidth > curToDrawImagesWidth) curToDrawImagesWidth = snappedWidth;
            if (snappedHeight > curToDrawImagesHeight) curToDrawImagesHeight = snappedHeight;
            //System.out.printf("Growing cached objects to [%d,%d]\n", curToDrawImagesWidth, curToDrawImagesHeight);

            scaleBuffer = new byte[curToDrawImagesWidth * curToDrawImagesHeight * 4];
            toDrawImages.clear();
            toDrawImageIndex = 0;
        }

        // Move the canvas so that it encompasses the visible part of the parent (ScrollPane)
        canvas.relocate(snappedMinX, snappedMinY);
        canvas.setWidth(snappedWidth);
        canvas.setHeight(snappedHeight);
    }

    ///////////////////////
    // Protected Methods //
    ///////////////////////

    @Override
    protected double computePrefWidth(double height)
    {
        return sourceImage == null ? 0 : sourceImage.getWidth() * zoomFactor;
    }

    @Override
    protected double computePrefHeight(double width)
    {
        return sourceImage == null ? 0 : sourceImage.getHeight() * zoomFactor;
    }

    @Override
    protected void layoutChildren()
    {
        super.layoutChildren();
        draw();
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void draw()
    {
        final Bounds layout = canvas.getBoundsInParent();
        final int renderX = (int)layout.getMinX();
        final int renderY = (int)layout.getMinY();
        final int renderW = (int)layout.getWidth();
        final int renderH = (int)layout.getHeight();

        if (renderW <= 0 || renderH <= 0)
        {
            return;
        }

        switch (GlobalState.getNativePixelFormatType())
        {
            case INT_ARGB_PRE, INT_ARGB, BYTE_BGRA, BYTE_RGB, BYTE_INDEXED
                -> throw new UnsupportedOperationException();
            case BYTE_BGRA_PRE ->
            {
                WritableImage toDrawImage;

                if (toDrawImages.size() == toDrawImageIndex)
                {
                    toDrawImage = new WritableImage(curToDrawImagesWidth, curToDrawImagesHeight);
                    toDrawImages.add(toDrawImage);
                    //System.out.printf("Created new image with dimensions: [%d,%d]\n",
                    //    (int)toDrawImage.getWidth(), (int)toDrawImage.getHeight());
                }
                else
                {
                    toDrawImage = toDrawImages.get(toDrawImageIndex);
                }

                //System.out.printf("Drawing from image %d with size [%d,%d], renderW: %d, renderH: %d\n",
                //    toDrawImageIndex, (int)toDrawImage.getWidth(), (int)toDrawImage.getHeight(),
                //    renderW, renderH);

                ++toDrawImageIndex;

                scaleBgra(renderX, renderY, renderW, renderH);
                toDrawImage.getPixelWriter().setPixels(0, 0, renderW, renderH,
                    PixelFormat.getByteBgraPreInstance(), scaleBuffer, 0, renderW * 4);

                graphics.clearRect(0, 0, renderW, renderH);
                graphics.drawImage(toDrawImage,
                    0, 0, renderW, renderH,
                    0, 0, renderW, renderH);
            }
        }

        if (drawCallback != null)
        {
            drawCallback.accept(graphics);
        }
    }

    /**
     * Resets `toDrawImageIndex` on each render pulse, signaling that all cached image objects can be used again.
     */
    private void onRenderPulse()
    {
        toDrawImageIndex = 0;
    }

    private void scaleBgra(
        final int renderX, final int renderY, final int renderW, final int renderH)
    {
        final byte[] src = ((DataBufferByte)sourceImage.getRaster().getDataBuffer()).getData();
        final int srcWidth = sourceImage.getWidth();

        for (int y = 0, curDstOffset = 0; y < renderH; ++y)
        {
            final int curSrcY = (int)((renderY + y) / zoomFactor);
            for (int x = 0; x < renderW; ++x, curDstOffset += 4)
            {
                final int curSrcX = (int)((renderX + x) / zoomFactor);
                final int srcOffset = (curSrcY * srcWidth + curSrcX) * 4;
                System.arraycopy(src, srcOffset, scaleBuffer, curDstOffset, 4);
            }
        }
    }
}
