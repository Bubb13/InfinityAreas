
package com.github.bubb13.infinityareas.gui.pane;

import com.github.bubb13.infinityareas.gui.region.PartiallyRenderedImage;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class ZoomPane extends NotifyingScrollPane
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final PartiallyRenderedImage partialImage = new PartiallyRenderedImage();
    private double zoomFactor = 1;
    private Consumer<Double> zoomFactorListener;
    private Consumer<MouseEvent> mouseDraggedListener;
    private Consumer<MouseEvent> mouseClickedListener;
    private Consumer<MouseEvent> mousePressedListener;
    private Consumer<MouseEvent> mouseReleasedListener;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public ZoomPane()
    {
        super();
        init();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void setZoomFactorListener(final Consumer<Double> zoomFactorListener)
    {
        this.zoomFactorListener = zoomFactorListener;
    }

    public double getZoomFactor()
    {
        return zoomFactor;
    }

    public void setImage(final BufferedImage image, final boolean resetZoomFactor)
    {
        final BufferedImage previousImage = partialImage.getSourceImage();
        partialImage.setSourceImage(image);

        if (resetZoomFactor)
        {
            setZoomFactor(1);
            partialImage.setZoomFactor(zoomFactor);
        }

        if (resetZoomFactor
            || previousImage.getWidth() != image.getWidth()
            || previousImage.getHeight() != image.getHeight())
        {
            setHvalue(0);
            setVvalue(0);
        }
    }

    public void setImage(final BufferedImage image)
    {
        setImage(image, true);
    }

    public GraphicsContext getGraphics()
    {
        return partialImage.getGraphics();
    }

    public void setDrawCallback(final Consumer<GraphicsContext> drawCallback)
    {
        partialImage.setDrawCallback(drawCallback);
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void setZoomFactor(final double newValue)
    {
        if (zoomFactor != newValue)
        {
            zoomFactor = newValue;
            if (zoomFactorListener != null)
            {
                zoomFactorListener.accept(zoomFactor);
            }
        }
    }

    private void init()
    {
        final Pane pane = new Pane();
        pane.getChildren().add(partialImage);

        setContent(pane);
        setPannable(true);
        setFocusTraversable(false);
        setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        addEventFilter(ScrollEvent.SCROLL, this::onScroll);
        partialImage.setOnMouseClicked(this::onMouseClicked);
        partialImage.setOnMouseDragged(this::onMouseDragged);
        partialImage.setOnMousePressed(this::onMousePressed);
        partialImage.setOnMouseReleased(this::onMouseReleased);
    }

    private void onMouseClicked(final MouseEvent event)
    {
        if (mouseClickedListener != null)
        {
            mouseClickedListener.accept(event);
        }
    }

    private void onMouseDragged(final MouseEvent event)
    {
        if (mouseDraggedListener != null)
        {
            mouseDraggedListener.accept(event);
        }
    }

    private void onMousePressed(final MouseEvent event)
    {
        if (mousePressedListener != null)
        {
            mousePressedListener.accept(event);
        }
    }

    private void onMouseReleased(final MouseEvent event)
    {
        if (mouseReleasedListener != null)
        {
            mouseReleasedListener.accept(event);
        }
    }

    private void onScroll(final ScrollEvent event)
    {
        if (event.isControlDown())
        {
            event.consume();
            onZoom(event.getDeltaY());
        }
    }

    public Point2D sourceToAbsoluteCanvasPosition(final int srcX, final int srcY)
    {
        return partialImage.sourceToAbsoluteCanvasPosition(srcX, srcY);
    }

    public Point2D absoluteToRelativeCanvasPosition(final int canvasX, final int canvasY)
    {
        return partialImage.absoluteToRelativeCanvasPosition(canvasX, canvasY);
    }

    public Point2D absoluteCanvasToSourcePosition(final int canvasX, final int canvasY)
    {
        return partialImage.absoluteCanvasToSourcePosition(canvasX, canvasY);
    }

    public Rectangle2D getVisibleSourceRect()
    {
        return partialImage.getVisibleSourceRect();
    }

    private void onZoom(final double deltaY)
    {
        final Bounds viewportBounds = getViewportBounds();
        final double viewportWidth = viewportBounds.getWidth();
        final double viewportHeight = viewportBounds.getHeight();
        final double viewportEffectiveWidth = viewportWidth / zoomFactor;
        final double viewportEffectiveHeight = viewportHeight / zoomFactor;

        final Bounds imageViewBounds = partialImage.getBoundsInLocal();
        final double imageViewWidth = imageViewBounds.getWidth();
        final double imageViewHeight = imageViewBounds.getHeight();

        final double hMax = getHmax();
        final double vMax = getVmax();

        final double hRel = getHvalue() / hMax;
        final double vRel = getVvalue() / vMax;

        final double xLeft = hRel * (imageViewWidth - viewportWidth) / zoomFactor;
        final double yTop = vRel * (imageViewHeight - viewportHeight) / zoomFactor;

        if (deltaY > 0)
        {
            setZoomFactor(zoomFactor * 1.1);
        }
        else if (deltaY < 0)
        {
            setZoomFactor(zoomFactor * 0.9);
        }

        final BufferedImage image = partialImage.getSourceImage();
        final double newImageViewWidth = image.getWidth() * zoomFactor;
        final double newImageViewHeight = image.getHeight() * zoomFactor;
        final double newViewportEffectiveWidth = viewportWidth / zoomFactor;
        final double newViewportEffectiveHeight = viewportHeight / zoomFactor;

        partialImage.setZoomFactor(zoomFactor);
        layout();

        final double targetCenterX = xLeft + (viewportEffectiveWidth / 2);
        final double targetCenterY = yTop + (viewportEffectiveHeight / 2);

        final double targetXLeft = targetCenterX - (newViewportEffectiveWidth / 2);
        final double targetYLeft = targetCenterY - (newViewportEffectiveHeight / 2);

        final double newHRel = targetXLeft * zoomFactor / (newImageViewWidth - viewportWidth);
        final double newVRel = targetYLeft * zoomFactor / (newImageViewHeight - viewportHeight);

        final double newHVal = newHRel * hMax;
        final double newVVal = newVRel * vMax;

        setHvalue(newHVal);
        setVvalue(newVVal);
    }

    public void setMouseDraggedListener(final Consumer<MouseEvent> mouseDraggedListener)
    {
        this.mouseDraggedListener = mouseDraggedListener;
    }

    public void setMouseClickedListener(final Consumer<MouseEvent> mouseDraggedListener)
    {
        this.mouseClickedListener = mouseDraggedListener;
    }

    public void setMousePressedListener(final Consumer<MouseEvent> mousePressedListener)
    {
        this.mousePressedListener = mousePressedListener;
    }

    public void setMouseReleasedListener(final Consumer<MouseEvent> mouseReleasedListener)
    {
        this.mouseReleasedListener = mouseReleasedListener;
    }

    public void requestDraw()
    {
        partialImage.requestLayout();
    }
}
