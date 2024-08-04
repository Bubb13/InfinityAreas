
package com.github.bubb13.infinityareas.gui.region;

import com.github.bubb13.infinityareas.gui.VisibleNotifiable;
import com.github.bubb13.infinityareas.misc.Corners;
import com.github.bubb13.infinityareas.misc.DoubleCorners;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class PartiallyRenderedImageRegion extends Region implements VisibleNotifiable
{
    ///////////////////////////
    // Private Static Fields //
    ///////////////////////////

    /**
     * Single-pixel canvas drawing starts breaking down after this value
     */
    private final double MAX_ZOOM_FACTOR = 2500;

    ////////////////////
    // Private Fields //
    ////////////////////

    private final Canvas canvas = new Canvas();
    private final GraphicsContext graphics = canvas.getGraphicsContext2D();
    private final PartiallyRenderedImageLogic logic = new PartiallyRenderedImageLogic();

    private double zoomFactor = 1;
    private Consumer<GraphicsContext> drawCallback;
    private boolean blockDraw = false;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public PartiallyRenderedImageRegion()
    {
        getChildren().add(canvas);
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void blockDraw(final boolean value)
    {
        blockDraw = value;
    }

    public void setSourceImage(final BufferedImage sourceImage)
    {
        logic.setSourceImage(sourceImage);
        requestLayout();
    }

    public BufferedImage getSourceImage()
    {
        return logic.getSourceImage();
    }

    public double setZoomFactor(double newZoomFactor)
    {
        zoomFactor = Math.min(newZoomFactor, MAX_ZOOM_FACTOR);
        logic.setSrcScaleFactor(zoomFactor, zoomFactor);
        requestLayout();
        return zoomFactor;
    }

    public GraphicsContext getGraphics()
    {
        return graphics;
    }

    public WritableImage getLatestCanvasBackgroundImage()
    {
        return logic.getLatestCanvasBackgroundImage();
    }

    public void setDrawCallback(final Consumer<GraphicsContext> drawCallback)
    {
        this.drawCallback = drawCallback;
    }

    public Point2D sourceToCanvasPosition(final int srcX, final int srcY)
    {
        final Bounds layout = canvas.getBoundsInParent();
        final int renderX = (int)layout.getMinX();
        final int renderY = (int)layout.getMinY();
        return new Point2D((int)(srcX * zoomFactor - renderX), (int)(srcY * zoomFactor - renderY));
    }

    public Point2D sourceToCanvasDoublePosition(final double srcX, final double srcY)
    {
        final Bounds layout = canvas.getBoundsInParent();
        return new Point2D(srcX * zoomFactor - layout.getMinX(), srcY * zoomFactor - layout.getMinY());
    }

    public Point canvasToSourcePosition(final int canvasX, final int canvasY)
    {
        final Bounds layout = canvas.getBoundsInParent();
        return new Point(
            (int)((layout.getMinX() + canvasX) / zoomFactor),
            (int)((layout.getMinY() + canvasY) / zoomFactor)
        );
    }

    public Point2D canvasToSourceDoublePosition(final double canvasX, final double canvasY)
    {
        final Bounds layout = canvas.getBoundsInParent();
        return new Point2D((layout.getMinX() + canvasX) / zoomFactor, (layout.getMinY() + canvasY) / zoomFactor);
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

    public Point2D absoluteCanvasToSourceDoublePosition(final double canvasX, final double canvasY)
    {
        return new Point2D(canvasX / zoomFactor, canvasY / zoomFactor);
    }

    public Bounds getCanvasBounds()
    {
        return canvas.getBoundsInParent();
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

    public DoubleCorners getVisibleSourceDoubleCorners()
    {
        final Bounds layout = canvas.getBoundsInParent();
        final double renderTopLeftX = layout.getMinX();
        final double renderTopLeftY = layout.getMinY();
        final double renderBottomRightExclusiveX = layout.getMaxX();
        final double renderBottomRightExclusiveY = layout.getMaxY();
        return new DoubleCorners(
            renderTopLeftX / zoomFactor,
            renderTopLeftY / zoomFactor,
            renderBottomRightExclusiveX / zoomFactor,
            renderBottomRightExclusiveY / zoomFactor
        );
    }

    @Override
    public void notifyVisible(final Bounds bounds)
    {
        if (bounds.isEmpty())
        {
            //System.out.printf("Bad bounds\n");
            return;
        }

        // Move the canvas so that it encompasses the visible part of the parent (ScrollPane)
        canvas.relocate(bounds.getMinX(), bounds.getMinY());
        canvas.setWidth(bounds.getWidth());
        canvas.setHeight(bounds.getHeight());
    }

    ///////////////////////
    // Protected Methods //
    ///////////////////////

    @Override
    protected double computePrefWidth(double height)
    {
        final BufferedImage sourceImage = logic.getSourceImage();
        return sourceImage == null ? 0 : sourceImage.getWidth() * zoomFactor;
    }

    @Override
    protected double computePrefHeight(double width)
    {
        final BufferedImage sourceImage = logic.getSourceImage();
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
        if (blockDraw)
        {
            return;
        }

        final double renderW = canvas.getWidth();
        final double renderH = canvas.getHeight();

        if (renderW <= 0 || renderH <= 0)
        {
            return;
        }

        logic.draw(graphics, canvas.getLayoutX(), canvas.getLayoutY(), 0, 0, renderW, renderH);

        if (drawCallback != null)
        {
            drawCallback.accept(graphics);
        }
    }
}
