
package com.github.bubb13.infinityareas.gui.pane;

import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.game.resource.PVRZ;
import com.github.bubb13.infinityareas.game.resource.ResourceDataCache;
import com.github.bubb13.infinityareas.game.resource.TIS;
import com.github.bubb13.infinityareas.gui.control.ShrinkableImageView;
import com.github.bubb13.infinityareas.misc.SimpleCache;
import com.github.bubb13.infinityareas.util.JavaFXUtil;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.nio.IntBuffer;

public class TISPane extends StackPane
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final ImageView imageView;
    private final ScrollPane scrollPane;
    private final ResourceDataCache resourceDataCache = new ResourceDataCache();
    private final SimpleCache<String, PVRZ> pvrzCache = new SimpleCache<>();
    private final Slider slider;
    private double zoomFactor = 1;
    private TIS tis;
    private final Label previewTileWidthLabel;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public TISPane()
    {
        super();
        imageView = new ShrinkableImageView();
        scrollPane = new ScrollPane();
        slider = new Slider();
        previewTileWidthLabel = new Label();
        init();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public JavaFXUtil.TaskManager.ManagedTask<Void> setSourceTask(final Game.ResourceSource source)
    {
        return new SetTISTask(source);
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init()
    {
        imageView.setPreserveRatio(true);
        imageView.setSmooth(false);

        //////////
        // VBox //
        //////////

        final VBox vbox = new VBox();
        vbox.setFocusTraversable(false);
        vbox.setPadding(new Insets(5, 0, 0, 10));

            //////////
            // HBox //
            //////////

            final HBox hbox = new HBox();
            hbox.setFocusTraversable(false);

                //////////
                // Text //
                //////////

                previewTileWidthLabel.setFont(Font.font(16));

                /////////////////
                // Slider VBox //
                /////////////////

                final VBox sliderVBox = new VBox();
                sliderVBox.setAlignment(Pos.CENTER);

                    ////////////
                    // Slider //
                    ////////////

                    slider.setPadding(new Insets(0, 0, 0, 5));
                    slider.setMin(1);
                    slider.setBlockIncrement(1);
                    slider.valueProperty().addListener((observable, oldValue, newValue) -> onSliderChange(newValue));

                    sliderVBox.getChildren().add(slider);


            HBox.setHgrow(sliderVBox, Priority.ALWAYS);
            hbox.getChildren().addAll(previewTileWidthLabel, sliderVBox);

            ////////////////
            // ScrollPane //
            ////////////////

            scrollPane.setContent(imageView);
            scrollPane.setPannable(true);
            scrollPane.setFocusTraversable(false);
            scrollPane.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

            scrollPane.addEventFilter(ScrollEvent.SCROLL, event ->
            {
                if (event.isControlDown())
                {
                    event.consume();
                    onZoom(event.getDeltaY());
                }
            });


        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        vbox.getChildren().addAll(hbox, scrollPane);
        getChildren().add(vbox);
    }

    private void onSliderChange(final Number number)
    {
        final int newNumTilesX = number.intValue();
        slider.setValue(newNumTilesX);
        renderPreview(newNumTilesX);
    }

    private void onZoom(final double deltaY)
    {
        final Bounds viewportBounds = scrollPane.getViewportBounds();
        final double viewportWidth = viewportBounds.getWidth();
        final double viewportHeight = viewportBounds.getHeight();
        final double viewportEffectiveWidth = viewportWidth / zoomFactor;
        final double viewportEffectiveHeight = viewportHeight / zoomFactor;

        final Bounds imageViewBounds = imageView.getBoundsInLocal();
        final double imageViewWidth = imageViewBounds.getWidth();
        final double imageViewHeight = imageViewBounds.getHeight();

        final double hMax = scrollPane.getHmax();
        final double vMax = scrollPane.getVmax();

        final double hRel = scrollPane.getHvalue() / hMax;
        final double vRel = scrollPane.getVvalue() / vMax;

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

        scrollPane.setHvalue(newHVal);
        scrollPane.setVvalue(newVVal);
    }

//    private WritableImage scaleImage(final Image inputImage, final double scaleFactor)
//    {
//        final int width = (int)(inputImage.getWidth() * scaleFactor);
//        final int height = (int)(inputImage.getHeight() * scaleFactor);
//
//        System.out.println("new width: " + width);
//        System.out.println("new height: " + height);
//
//        final WritableImage outputImage = new WritableImage(width, height);
//        final PixelWriter pixelWriter = outputImage.getPixelWriter();
//
//        for (int y = 0; y < height; ++y)
//        {
//            for (int x = 0; x < width; ++x)
//            {
//                final int argb = inputImage.getPixelReader().getArgb((int)(x / scaleFactor), (int)(y / scaleFactor));
//                pixelWriter.setArgb(x, y, argb);
//            }
//        }
//
//        return outputImage;
//    }

    private void renderPreview(final int previewNumTilesX)
    {
        final int numTiles = tis.getNumTiles();

        final int tileSideLength = tis.getTileSideLength();
        final int previewNumTilesY = (numTiles + (previewNumTilesX - 1)) / previewNumTilesX;

        final int previewWidth = previewNumTilesX * tileSideLength;
        final int previewHeight = previewNumTilesY * tileSideLength;

        final WritableImage writableImage = new WritableImage(previewWidth, previewHeight);
        final PixelWriter pixelWriter = writableImage.getPixelWriter();

        for (int yPos = 0, i = 0; yPos < previewHeight; yPos += tileSideLength)
        {
            for (int xPos = 0; xPos < previewWidth; xPos += tileSideLength, ++i)
            {
                final IntBuffer tileData = tis.getPreRenderedTileData(i);
                pixelWriter.setPixels(xPos, yPos,
                    tileSideLength, tileSideLength,
                    PixelFormat.getIntArgbInstance(),
                    tileData.array(), 0, tileSideLength);
            }
        }

        JavaFXUtil.waitForGuiThreadToExecute(() ->
        {
            previewTileWidthLabel.setText("Preview Tile Width: " + previewNumTilesX);
            slider.setValue(previewNumTilesX);
            imageView.setImage(writableImage);
        });
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private class SetTISTask extends JavaFXUtil.TaskManager.ManagedTask<Void>
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        final Game.ResourceSource source;

        ///////////////////////
        // Protected Methods //
        ///////////////////////

        public SetTISTask(final Game.ResourceSource source)
        {
            this.source = source;
        }

        @Override
        protected Void call() throws Exception
        {
            final TIS tis = new TIS(source, resourceDataCache, pvrzCache);
            subtask(tis.loadTISTask());
            TISPane.this.tis = tis;

            final int numTiles = tis.getNumTiles();
            slider.setMax(numTiles);

            final int previewNumTilesPerSide = (int)Math.ceil(Math.sqrt(numTiles));
            renderPreview(previewNumTilesPerSide);
            return null;
        }

        /////////////////////
        // Private Methods //
        /////////////////////


    }
}
