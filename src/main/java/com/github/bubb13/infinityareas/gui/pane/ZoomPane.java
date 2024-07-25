
package com.github.bubb13.infinityareas.gui.pane;

import com.github.bubb13.infinityareas.gui.region.PartiallyRenderedImage;
import com.github.bubb13.infinityareas.misc.Corners;
import com.github.bubb13.infinityareas.misc.DoubleCorners;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;

import java.awt.Point;
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
    private Consumer<KeyEvent> keyPressedListener;
    private Consumer<MouseEvent> dragDetectedListener;

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

        final boolean sizeChanged = previousImage != null
            && (previousImage.getWidth() != image.getWidth() || previousImage.getHeight() != image.getHeight());

        if (resetZoomFactor || sizeChanged)
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

    public void setKeyPressedListener(final Consumer<KeyEvent> keyPressedListener)
    {
        this.keyPressedListener = keyPressedListener;
    }

    public void setDragDetectedListener(final Consumer<MouseEvent> dragDetectedListener)
    {
        this.dragDetectedListener = dragDetectedListener;
    }

    public void requestDraw()
    {
        partialImage.requestLayout();
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

        addEventFilter(ScrollEvent.SCROLL, this::onScrollFilter);
        partialImage.setOnMouseClicked(this::onMouseClicked);
        partialImage.setOnMouseDragged(this::onMouseDragged);
        partialImage.setOnMousePressed(this::onMousePressed);
        partialImage.setOnMouseReleased(this::onMouseReleased);
        partialImage.setOnKeyPressed(this::onKeyPressed);
        partialImage.setOnDragDetected(this::onDragDetected);
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

    private void onKeyPressed(final KeyEvent event)
    {
        if (keyPressedListener != null)
        {
            keyPressedListener.accept(event);
        }
    }

    private void onDragDetected(final MouseEvent event)
    {
        if (dragDetectedListener != null)
        {
            dragDetectedListener.accept(event);
        }
    }

    private void onScrollFilter(final ScrollEvent event)
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

    public Point2D sourceToAbsoluteCanvasDoublePosition(final double srcX, final double srcY)
    {
        return partialImage.sourceToAbsoluteCanvasDoublePosition(srcX, srcY);
    }

    public Point2D absoluteToRelativeCanvasPosition(final int canvasX, final int canvasY)
    {
        return partialImage.absoluteToRelativeCanvasPosition(canvasX, canvasY);
    }

    public Point absoluteCanvasToSourcePosition(final int canvasX, final int canvasY)
    {
        return partialImage.absoluteCanvasToSourcePosition(canvasX, canvasY);
    }

    public Point2D absoluteCanvasToSourceDoublePosition(final double canvasX, final double canvasY)
    {
        return partialImage.absoluteCanvasToSourceDoublePosition(canvasX, canvasY);
    }

    public Rectangle2D getVisibleSourceRect()
    {
        return partialImage.getVisibleSourceRect();
    }

    public Corners getVisibleSourceCorners()
    {
        return partialImage.getVisibleSourceCorners();
    }

    public DoubleCorners getVisibleSourceDoubleCorners()
    {
        return partialImage.getVisibleSourceDoubleCorners();
    }

    private void onZoom(final double deltaY)
    {
        // Block notify+draw events until positioning is done
        blockDraw(true);

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

        double newZoomFactor = zoomFactor;
        if (deltaY > 0)
        {
            newZoomFactor *= 1.1;
        }
        else if (deltaY < 0)
        {
            newZoomFactor *= 0.9;
        }

        final BufferedImage image = partialImage.getSourceImage();
        final double imageWidth = image.getWidth();
        final double imageHeight = image.getHeight();

        final double fitZoomFactorX = viewportWidth / imageWidth;
        final double fitZoomFactorY = viewportHeight / imageHeight;
        final double fitZoomFactor = Math.min(fitZoomFactorX, fitZoomFactorY);

        newZoomFactor = Math.max(fitZoomFactor, newZoomFactor);

        if (newZoomFactor == zoomFactor)
        {
            blockDraw(false);
            return;
        }

        setZoomFactor(partialImage.setZoomFactor(newZoomFactor));
        layout();

        final double newImageViewWidth = imageWidth * zoomFactor;
        final double newImageViewHeight = imageHeight * zoomFactor;
        final double newViewportEffectiveWidth = viewportWidth / zoomFactor;
        final double newViewportEffectiveHeight = viewportHeight / zoomFactor;

        final double targetCenterX = xLeft + (viewportEffectiveWidth / 2);
        final double targetCenterY = yTop + (viewportEffectiveHeight / 2);

        final double targetXLeft = targetCenterX - (newViewportEffectiveWidth / 2);
        final double targetYLeft = targetCenterY - (newViewportEffectiveHeight / 2);

        final double newHRel = targetXLeft * zoomFactor / (newImageViewWidth - viewportWidth);
        final double newVRel = targetYLeft * zoomFactor / (newImageViewHeight - viewportHeight);

        final double newHVal = newHRel * hMax;
        final double newVVal = newVRel * vMax;

        setHvalue(newHVal);
        // Release the notify+draw block so that the `setVvalue()` call triggers those events
        blockDraw(false);
        setVvalue(newVVal);
    }

    private void blockDraw(final boolean newValue)
    {
        blockNotify(newValue);
        partialImage.blockDraw(newValue);
    }
}
