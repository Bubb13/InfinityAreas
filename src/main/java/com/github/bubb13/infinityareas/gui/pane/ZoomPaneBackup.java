
package com.github.bubb13.infinityareas.gui.pane;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Bounds;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

import java.awt.image.BufferedImage;

public class ZoomPaneBackup extends ScrollPane
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final ImageView imageView = new ImageView();
    private BufferedImage image;
    private double zoomFactor = 1;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public ZoomPaneBackup()
    {
        super();
        init();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void setImage(final BufferedImage image)
    {
        this.image = image;
        updateImageView();
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init()
    {
        imageView.setPreserveRatio(true);
        imageView.setSmooth(false);

        setContent(imageView);
        setPannable(true);
        setFocusTraversable(false);
        setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        addEventFilter(ScrollEvent.SCROLL, this::onScroll);
        imageView.setOnMouseClicked(this::onMouseClick);
    }

    private void updateImageView()
    {
        imageView.setImage(SwingFXUtils.toFXImage(image, null));
    }

    private void onMouseClick(final MouseEvent event)
    {
        final double imageViewX = event.getX();
        final double imageViewY = event.getY();

        final int imageX = (int)(imageViewX / zoomFactor);
        final int imageY = (int)(imageViewY / zoomFactor);

        System.out.printf("Click at (%d,%d)\n", imageX, imageY);
    }

    private void onScroll(final ScrollEvent event)
    {
        if (event.isControlDown())
        {
            event.consume();
            onZoom(event.getDeltaY());
        }
    }

    private void onZoom(final double deltaY)
    {
        final Bounds viewportBounds = getViewportBounds();
        final double viewportWidth = viewportBounds.getWidth();
        final double viewportHeight = viewportBounds.getHeight();
        final double viewportEffectiveWidth = viewportWidth / zoomFactor;
        final double viewportEffectiveHeight = viewportHeight / zoomFactor;

        final Bounds imageViewBounds = imageView.getBoundsInLocal();
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
            zoomFactor *= 1.1;
        }
        else if (deltaY < 0)
        {
            zoomFactor *= 0.9;
        }

        final Image image = imageView.getImage();
        final double newImageViewWidth = image.getWidth() * zoomFactor;
        final double newImageViewHeight = image.getHeight() * zoomFactor;
        final double newViewportEffectiveWidth = viewportWidth / zoomFactor;
        final double newViewportEffectiveHeight = viewportHeight / zoomFactor;

        imageView.setFitWidth(newImageViewWidth);
        imageView.setFitHeight(newImageViewHeight);

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

    private void scaleImage()
    {
        final Bounds viewportBounds = getViewportBounds();
        final double viewportWidth = viewportBounds.getWidth();
        final double viewportHeight = viewportBounds.getHeight();
        final double viewportEffectiveWidth = viewportWidth / zoomFactor;
        final double viewportEffectiveHeight = viewportHeight / zoomFactor;

        final Bounds imageViewBounds = imageView.getBoundsInLocal();
        final double imageViewWidth = imageViewBounds.getWidth();
        final double imageViewHeight = imageViewBounds.getHeight();

        final double hMax = getHmax();
        final double vMax = getVmax();

        final double hRel = getHvalue() / hMax;
        final double vRel = getVvalue() / vMax;

        final double xLeft = hRel * (imageViewWidth - viewportWidth) / zoomFactor;
        final double yTop = vRel * (imageViewHeight - viewportHeight) / zoomFactor;
        final double xRight = xLeft + viewportEffectiveWidth;
        final double yBottom = yTop + viewportEffectiveHeight;

        final int xCroppedLeft = (int)xLeft;
        final int yCroppedTop = (int)yTop;
        final int xCroppedRight = (int)Math.ceil(xRight);
        final int yCroppedBottom = (int)Math.ceil(yBottom);
    }
}
