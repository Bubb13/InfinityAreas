
package com.github.bubb13.infinityareas.gui.region;

import com.github.bubb13.infinityareas.gui.misc.VisibleNotifiable;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
import java.util.function.Consumer;

public class PartiallyRenderedImage extends Region implements VisibleNotifiable
{
    private final Canvas canvas = new Canvas();
    private final GraphicsContext graphics = canvas.getGraphicsContext2D();
    private Consumer<GraphicsContext> drawCallback;
    private BufferedImage image;
    private double zoomFactor = 1;

    public PartiallyRenderedImage()
    {
        getChildren().add(canvas);
    }

    public void setImage(final BufferedImage image)
    {
        if (!Platform.isFxApplicationThread())
        {
            throw new IllegalStateException();
        }

        this.image = image;
        requestLayout();
    }

    public void setZoomFactor(double zoomFactor)
    {
        this.zoomFactor = zoomFactor;
        requestLayout();
    }

    public BufferedImage getImage()
    {
        return image;
    }

    @Override
    protected double computePrefWidth(double height)
    {
        return image == null ? 0 : image.getWidth() * zoomFactor;
    }

    @Override
    protected double computePrefHeight(double width)
    {
        return image == null ? 0 : image.getHeight() * zoomFactor;
    }

    @Override
    protected void layoutChildren()
    {
        super.layoutChildren();
        draw();
    }

    private void draw()
    {
        final Bounds layout = canvas.getBoundsInParent();
        final int renderX = (int)layout.getMinX();
        final int renderY = (int)layout.getMinY();
        final int renderW = (int)layout.getWidth();
        final int renderH = (int)layout.getHeight();

        if (renderX < 0 || renderY < 0 || renderW <= 0 || renderH <= 0
            || (renderX + renderW) / zoomFactor > image.getWidth()
            || (renderY + renderH) / zoomFactor > image.getHeight())
        {
            return;
        }

        final WritableRaster srcRaster = image.getRaster();
        final int srcWidth = image.getWidth();

        int[] srcBuffer;
        if (srcRaster.getDataBuffer() instanceof DataBufferInt dataBufferInt)
        {
            srcBuffer = dataBufferInt.getData();
        }
        else
        {
            throw new UnsupportedOperationException();
        }

        // Copy scaled data from the buffered image
        final int[] dst = new int[renderW * renderH];
        for (int y = 0, curDstOffset = 0; y < renderH; ++y)
        {
            final int curSrcY = (int)((renderY + y) / zoomFactor);
            for (int x = 0; x < renderW; ++x)
            {
                final int curSrcX = (int)((renderX + x) / zoomFactor);
                dst[curDstOffset++] = srcBuffer[curSrcY * srcWidth + curSrcX];
            }
        }

        final WritableImage toDrawImage = new WritableImage(renderW, renderH);
        toDrawImage.getPixelWriter().setPixels(0, 0, renderW, renderH,
            PixelFormat.getIntArgbInstance(), dst, 0, renderW);

        graphics.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        graphics.drawImage(toDrawImage, 0, 0);
        if (drawCallback != null)
        {
            drawCallback.accept(graphics);
        }
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

    public Point2D absoluteCanvasToSourcePosition(final int canvasX, final int canvasY)
    {
        return new Point2D((int)(canvasX / zoomFactor), (int)(canvasY / zoomFactor));
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
        final double snappedWidth = (int)Math.ceil(bounds.getWidth() + extraW);
        final double snappedHeight = (int)Math.ceil(bounds.getHeight() + extraH);

        canvas.relocate(snappedMinX, snappedMinY);
        canvas.setWidth(snappedWidth);
        canvas.setHeight(snappedHeight);
    }
}
