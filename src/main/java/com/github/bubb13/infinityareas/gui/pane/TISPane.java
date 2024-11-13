
package com.github.bubb13.infinityareas.gui.pane;

import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.game.resource.PVRZ;
import com.github.bubb13.infinityareas.game.resource.ResourceDataCache;
import com.github.bubb13.infinityareas.game.resource.TIS;
import com.github.bubb13.infinityareas.misc.SimpleCache;
import com.github.bubb13.infinityareas.misc.tasktracking.TaskTrackerI;
import com.github.bubb13.infinityareas.misc.tasktracking.TrackedTask;
import com.github.bubb13.infinityareas.util.JavaFXUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.nio.IntBuffer;

public class TISPane extends StackPane
{
    ////////////////////
    // Private Fields //
    ////////////////////

    // Data
    private final ResourceDataCache resourceDataCache = new ResourceDataCache();
    private final SimpleCache<String, PVRZ> pvrzCache = new SimpleCache<>();
    private TIS tis;

    // GUI
    private final Label previewTileWidthLabel = new Label();
    private final Slider slider = new Slider();
    private final ZoomPane zoomPane = new ZoomPane();

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public TISPane()
    {
        super();
        init();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public TrackedTask<Void> setSourceTask(final Game.ResourceSource source)
    {
        return new SetTISTask(source);
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init()
    {
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

            //////////////
            // ZoomPane //
            //////////////




        VBox.setVgrow(zoomPane, Priority.ALWAYS);
        vbox.getChildren().addAll(hbox, zoomPane);
        getChildren().add(vbox);
    }

    private void onSliderChange(final Number number)
    {
        final int newNumTilesX = number.intValue();
        slider.setValue(newNumTilesX);
        renderPreview(newNumTilesX);
    }

    private void renderPreview(final int previewNumTilesX)
    {
        final int numTiles = tis.getNumTiles();

        final int tileSideLength = tis.getTileSideLength();
        final int previewNumTilesY = (numTiles + (previewNumTilesX - 1)) / previewNumTilesX;

        final int previewWidth = previewNumTilesX * tileSideLength;
        final int previewHeight = previewNumTilesY * tileSideLength;

        final BufferedImage image = new BufferedImage(previewWidth, previewHeight, BufferedImage.TYPE_INT_ARGB);
        final WritableRaster raster = image.getRaster();

        for (int yPos = 0, i = 0; yPos < previewHeight; yPos += tileSideLength)
        {
            for (int xPos = 0; xPos < previewWidth; xPos += tileSideLength, ++i)
            {
                final IntBuffer tileData = tis.getPreRenderedTileData(i);
                raster.setDataElements(xPos, yPos, tileSideLength, tileSideLength, tileData.array());
            }
        }

        JavaFXUtil.waitForFxThreadToExecute(() ->
        {
            previewTileWidthLabel.setText("Preview Tile Width: " + previewNumTilesX);
            slider.setValue(previewNumTilesX);
            zoomPane.setImage(image, false);
        });
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private class SetTISTask extends TrackedTask<Void>
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
        protected Void doTask(final TaskTrackerI tracker) throws Exception
        {
            tracker.updateMessage("Processing TIS ...");
            tracker.updateProgress(0, 1);

            final TIS tis = new TIS(source, resourceDataCache, pvrzCache);
            tis.load(getTracker());
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
