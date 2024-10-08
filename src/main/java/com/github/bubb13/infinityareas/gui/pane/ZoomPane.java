
package com.github.bubb13.infinityareas.gui.pane;

import com.github.bubb13.infinityareas.gui.region.PartiallyRenderedImageRegion;
import com.github.bubb13.infinityareas.misc.Corners;
import com.github.bubb13.infinityareas.misc.DoubleCorners;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ZoomPane extends NotifyingScrollPane
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final PartiallyRenderedImageRegion partialImage = new PartiallyRenderedImageRegion();
    private double zoomFactor = 1;
    private Consumer<Double> zoomFactorListener;
    private Consumer<MouseEvent> mouseMovedListener;
    private Consumer<MouseEvent> mouseClickedListener;
    private Consumer<MouseEvent> mousePressedListener;
    private Consumer<MouseEvent> dragDetectedListener;
    private Consumer<MouseEvent> mouseDraggedListener;
    private Consumer<MouseEvent> mouseReleasedListener;
    private Consumer<MouseEvent> mouseExitedListener;
    private Consumer<KeyEvent> keyPressedListener;

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

    public void setZoomFactorListener(final Consumer<Double> zoomFactorListener)
    {
        this.zoomFactorListener = zoomFactorListener;
    }

    public double getZoomFactor()
    {
        return zoomFactor;
    }

    public GraphicsContext getGraphics()
    {
        return partialImage.getGraphics();
    }

    public WritableImage getLatestCanvasBackgroundImage()
    {
        return partialImage.getLatestCanvasBackgroundImage();
    }

    public void requestDraw()
    {
        partialImage.requestLayout();
    }

    //-----------//
    // Listeners //
    //-----------//

    public void setDrawCallback(final Consumer<GraphicsContext> drawCallback)
    {
        partialImage.setDrawCallback(drawCallback);
    }

    public void setMouseDraggedListener(final Consumer<MouseEvent> mouseDraggedListener)
    {
        this.mouseDraggedListener = mouseDraggedListener;
    }

    public void setMouseMovedListener(final Consumer<MouseEvent> mouseMovedListener)
    {
        this.mouseMovedListener = mouseMovedListener;
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

    public void setMouseExitedListener(final Consumer<MouseEvent> mouseExitedListener)
    {
        this.mouseExitedListener = mouseExitedListener;
    }

    public void setKeyPressedListener(final Consumer<KeyEvent> keyPressedListener)
    {
        this.keyPressedListener = keyPressedListener;
    }

    public void setDragDetectedListener(final Consumer<MouseEvent> dragDetectedListener)
    {
        this.dragDetectedListener = dragDetectedListener;
    }

    //------------------//
    // Position Helpers //
    //------------------//

    public Point2D sourceToCanvasPosition(final int srcX, final int srcY)
    {
        return partialImage.sourceToCanvasPosition(srcX, srcY);
    }

    public Point2D sourceToCanvasDoublePosition(final double srcX, final double srcY)
    {
        return partialImage.sourceToCanvasDoublePosition(srcX, srcY);
    }

    public Point canvasToSourcePosition(final int canvasX, final int canvasY)
    {
        return partialImage.canvasToSourcePosition(canvasX, canvasY);
    }

    public Point2D canvasToSourceDoublePosition(final double canvasX, final double canvasY)
    {
        return partialImage.canvasToSourceDoublePosition(canvasX, canvasY);
    }

    public int getSourceWidth()
    {
        return partialImage.getSourceImage().getWidth();
    }

    public int getSourceHeight()
    {
        return partialImage.getSourceImage().getHeight();
    }

    public Point2D absoluteToRelativeCanvasPosition(final int canvasX, final int canvasY)
    {
        return partialImage.absoluteToRelativeCanvasPosition(canvasX, canvasY);
    }

    public Point2D absoluteToRelativeCanvasDoublePosition(final double canvasX, final double canvasY)
    {
        return partialImage.absoluteToRelativeCanvasDoublePosition(canvasX, canvasY);
    }

    public Point absoluteCanvasToSourcePosition(final int canvasX, final int canvasY)
    {
        return partialImage.absoluteCanvasToSourcePosition(canvasX, canvasY);
    }

    public Point2D absoluteCanvasToSourceDoublePosition(final double canvasX, final double canvasY)
    {
        return partialImage.absoluteCanvasToSourceDoublePosition(canvasX, canvasY);
    }

    public Bounds getCanvasBounds()
    {
        return partialImage.getCanvasBounds();
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

    //-------//
    // Other //
    //-------//

    public void doOperationMaintainViewportCenter(final Supplier<Boolean> operation)
    {
        final BufferedImage image = partialImage.getSourceImage();

        if (image == null)
        {
            operation.get();
            return;
        }

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

        // Fixes a strange bug when selecting a region in the area editor before performing a zoom operation
        final double unsafeHVal = getHvalue();
        final double safeHVal = Double.isNaN(unsafeHVal) ? 0 : unsafeHVal;

        final double hRel = safeHVal / hMax;
        final double vRel = getVvalue() / vMax;

        final double xLeft = hRel * (imageViewWidth - viewportWidth) / zoomFactor;
        final double yTop = vRel * (imageViewHeight - viewportHeight) / zoomFactor;

        if (operation.get())
        {
            blockDraw(false);
            return;
        }

        //layout();

        final Bounds newViewportBounds = getViewportBounds();
        final double newViewportWidth = newViewportBounds.getWidth();
        final double newViewportHeight = newViewportBounds.getHeight();
        final double newViewportEffectiveWidth = newViewportWidth / zoomFactor;
        final double newViewportEffectiveHeight = newViewportHeight / zoomFactor;

        final double newImageViewWidth = image.getWidth() * zoomFactor;
        final double newImageViewHeight = image.getHeight() * zoomFactor;

        final double targetCenterX = xLeft + (viewportEffectiveWidth / 2);
        final double targetCenterY = yTop + (viewportEffectiveHeight / 2);

        final double targetXLeft = targetCenterX - (newViewportEffectiveWidth / 2);
        final double targetYLeft = targetCenterY - (newViewportEffectiveHeight / 2);

        final double newHRel = targetXLeft * zoomFactor / (newImageViewWidth - newViewportWidth);
        final double newVRel = targetYLeft * zoomFactor / (newImageViewHeight - newViewportHeight);

        final double newHVal = newHRel * hMax;
        final double newVVal = newVRel * vMax;

        setHvalue(newHVal);
        // Release the notify+draw block so that the `setVvalue()` call triggers those events
        blockDraw(false);
        setVvalue(newVVal);
    }

    public void doOperationMaintainViewportLeft(final Supplier<Boolean> operation)
    {
        final BufferedImage image = partialImage.getSourceImage();

        if (image == null)
        {
            operation.get();
            return;
        }

        // Block notify+draw events until positioning is done
        blockDraw(true);

        final Bounds viewportBounds = getViewportBounds();
        final double viewportWidth = viewportBounds.getWidth();
        final double viewportHeight = viewportBounds.getHeight();

        final Bounds imageViewBounds = partialImage.getBoundsInLocal();
        final double imageViewWidth = imageViewBounds.getWidth();
        final double imageViewHeight = imageViewBounds.getHeight();

        final double hMax = getHmax();
        final double vMax = getVmax();

        final double hRel = getHvalue() / hMax;
        final double vRel = getVvalue() / vMax;

        final double xLeft = hRel * (imageViewWidth - viewportWidth) / zoomFactor;
        final double yTop = vRel * (imageViewHeight - viewportHeight) / zoomFactor;

        if (operation.get())
        {
            blockDraw(false);
            return;
        }

        //layout();

        final Bounds newViewportBounds = getViewportBounds();
        final double newViewportWidth = newViewportBounds.getWidth();
        final double newViewportHeight = newViewportBounds.getHeight();

        final double newImageViewWidth = image.getWidth() * zoomFactor;
        final double newImageViewHeight = image.getHeight() * zoomFactor;

        final double newHRel = xLeft * zoomFactor / (newImageViewWidth - newViewportWidth);
        final double newVRel = yTop * zoomFactor / (newImageViewHeight - newViewportHeight);

        final double newHVal = newHRel * hMax;
        final double newVVal = newVRel * vMax;

        setHvalue(newHVal);
        // Release the notify+draw block so that the `setVvalue()` call triggers those events
        blockDraw(false);
        setVvalue(newVVal);
    }

    public void doOperationMaintainViewportBottom(final Supplier<Boolean> operation)
    {
        final BufferedImage image = partialImage.getSourceImage();

        if (image == null)
        {
            operation.get();
            return;
        }

        // Block notify+draw events until positioning is done
        blockDraw(true);

        final Bounds viewportBounds = getViewportBounds();
        final double viewportHeight = viewportBounds.getHeight();
        final double viewportEffectiveHeight = viewportHeight / zoomFactor;

        final Bounds imageViewBounds = partialImage.getBoundsInLocal();
        final double imageViewHeight = imageViewBounds.getHeight();

        final double vMax = getVmax();
        final double vRel = getVvalue() / vMax;
        final double yTop = vRel * (imageViewHeight - viewportHeight) / zoomFactor;
        final double yBottom = yTop + viewportEffectiveHeight;

        if (operation.get())
        {
            blockDraw(false);
            return;
        }

        //layout();

        final Bounds newViewportBounds = getViewportBounds();
        final double newViewportHeight = newViewportBounds.getHeight();
        final double newViewportEffectiveHeight = newViewportHeight / zoomFactor;

        final double newImageViewHeight = image.getHeight() * zoomFactor;

        final double targetYTop = yBottom - (newViewportEffectiveHeight);

        final double newVRel = targetYTop * zoomFactor / (newImageViewHeight - newViewportHeight);
        final double newVVal = newVRel * vMax;

        // Release the notify+draw block so that the `setVvalue()` call triggers those events
        blockDraw(false);
        setVvalue(newVVal);
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init()
    {
        final Pane pane = new Pane();
        partialImage.setClearBeforeDraw(true);
        pane.getChildren().add(partialImage);

        setContent(pane);
        setPannable(true);
        setFocusTraversable(false);
        setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        addEventFilter(ScrollEvent.SCROLL, this::onScrollFilter);
        partialImage.setOnMouseMoved(this::onMouseMoved);
        partialImage.setOnMouseClicked(this::onMouseClicked);
        partialImage.setOnMouseDragged(this::onMouseDragged);
        partialImage.setOnMousePressed(this::onMousePressed);
        partialImage.setOnMouseReleased(this::onMouseReleased);
        partialImage.setOnMouseExited(this::onMouseExited);
        partialImage.setOnKeyPressed(this::onKeyPressed);
        partialImage.setOnDragDetected(this::onDragDetected);
    }

    private double calculateFitZoomFactor()
    {
        final Bounds viewportBounds = getViewportBounds();
        final BufferedImage image = partialImage.getSourceImage();
        final double fitZoomFactorX = viewportBounds.getWidth() / image.getWidth();
        final double fitZoomFactorY = viewportBounds.getHeight() / image.getHeight();
        return Math.min(fitZoomFactorX, fitZoomFactorY);
    }

    private void blockDraw(final boolean newValue)
    {
        blockNotify(newValue);
        partialImage.blockDraw(newValue);
    }

    //-----------//
    // Listeners //
    //-----------//

    private void onZoom(final double deltaY)
    {
        doOperationMaintainViewportCenter(() ->
        {
            double newZoomFactor = zoomFactor;
            if (deltaY > 0)
            {
                newZoomFactor *= 1.1;
            }
            else if (deltaY < 0)
            {
                newZoomFactor *= 0.9;
            }

            newZoomFactor = Math.max(calculateFitZoomFactor(), newZoomFactor);

            if (newZoomFactor == zoomFactor)
            {
                return true;
            }

            setZoomFactor(partialImage.setZoomFactor(newZoomFactor));
            layout();
            return false;
        });
    }

    private void onScrollFilter(final ScrollEvent event)
    {
        if (event.isControlDown())
        {
            event.consume();
            onZoom(event.getDeltaY());
        }
    }

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

    private void onMouseMoved(final MouseEvent event)
    {
        if (mouseMovedListener != null)
        {
            mouseMovedListener.accept(event);
        }
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

    private void onMouseExited(final MouseEvent event)
    {
        if (mouseExitedListener != null)
        {
            mouseExitedListener.accept(event);
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
}
