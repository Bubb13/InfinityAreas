
package com.github.bubb13.infinityareas.gui.stage;

import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.game.resource.KeyFile;
import com.github.bubb13.infinityareas.game.resource.ResourceDataCache;
import com.github.bubb13.infinityareas.game.resource.ResourceIdentifier;
import com.github.bubb13.infinityareas.game.resource.TIS;
import com.github.bubb13.infinityareas.game.resource.WED;
import com.github.bubb13.infinityareas.gui.control.DynamicListView;
import com.github.bubb13.infinityareas.misc.SimpleCache;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

public class ReplaceOverlayTilesetStage extends Stage
{
    ////////////////////
    // Private Fields //
    ////////////////////

    // Data
    private final WED wed;

    // GUI

    private final ComboBox<Integer> overlayDropdown = new ComboBox<>();

    private final ObservableList<TilesetEntry> tilesetEntries = FXCollections
        .synchronizedObservableList(FXCollections.observableArrayList());

    private final DynamicListView<TilesetEntry> tilesetList = new DynamicListView<>(
        tilesetEntries);

    private final Button doneButton = new Button("Done");

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public ReplaceOverlayTilesetStage(final WED wed)
    {
        this.wed = wed;
        init();
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init()
    {
        initStyle(StageStyle.DECORATED);
        initModality(Modality.APPLICATION_MODAL);

        ////////////////////
        // Main StackPane //
        ////////////////////

        final StackPane stackPane = new StackPane();
        stackPane.setBackground(Background.fill(Color.WHITE));

            ///////////////
            // Main VBox //
            ///////////////

            final VBox mainVBox = new VBox();
            mainVBox.setPadding(new Insets(10, 10, 10, 10));

                ////////////////
                // Title HBox //
                ////////////////

                    final HBox titleHBox = new HBox();
                    titleHBox.setPadding(new Insets(0, 5, 5, 5));
                    titleHBox.setAlignment(Pos.CENTER);

                    ///////////
                    // Title //
                    ///////////

                        final Label title;
                        title = new Label("Replace Overlay Tileset");
                        title.setFont(Font.font(16));

                    titleHBox.getChildren().addAll(title);

                ///////////////////////////
                // Overlay Dropdown HBox //
                ///////////////////////////

                final HBox overlayDropdownHBox = new HBox();
                overlayDropdownHBox.setPadding(new Insets(0, 0, 5, 0));

                    //////////////////////
                    // Overlay Dropdown //
                    //////////////////////

                    overlayDropdown.setPromptText("Choose Overlay");
                    overlayDropdown.getSelectionModel().selectedItemProperty().addListener(
                        (observable, oldValue, newValue) -> onOverlaySelected(newValue));
                    final ObservableList<Integer> overlayDropdownItems = overlayDropdown.getItems();

                    for (int i = 0; i < wed.getOverlays().size(); ++i)
                    {
                        overlayDropdownItems.add(i);
                    }

                overlayDropdownHBox.getChildren().addAll(overlayDropdown);

                ////////////////////////////
                // Tileset selection list //
                ////////////////////////////

                tilesetList.setDisable(true);
                tilesetList.setMaxHeight(200);
                tilesetList.getSelectionModel().selectedItemProperty().addListener(
                    (observable, oldValue, newValue) -> onTilesetSelected(newValue));

                for (final Game.Resource resource : GlobalState.getGame()
                    .getResourcesOfType(KeyFile.NumericResourceType.TIS))
                {
                    tilesetEntries.add(new TilesetEntry(resource.getIdentifier().resref()));
                }

                ////////////////////////
                // Cancel / Done HBox //
                ////////////////////////

                final HBox cancelDoneHBox = new HBox();
                cancelDoneHBox.setAlignment(Pos.CENTER_RIGHT);
                cancelDoneHBox.setPadding(new Insets(10, 0, 0, 0));

                    ///////////////////
                    // Cancel Button //
                    ///////////////////

                    final Button cancelButton = new Button("Cancel");
                    cancelButton.setOnAction((ignored) -> onCancel());

                    /////////////
                    // Padding //
                    /////////////

                    final Region padding1 = new Region();
                    padding1.setPadding(new Insets(0, 0, 0, 5));

                    /////////////////
                    // Done Button //
                    /////////////////

                    doneButton.setDisable(true);
                    doneButton.setOnAction((ignored) -> onDone());

                cancelDoneHBox.getChildren().addAll(cancelButton, padding1, doneButton);

            mainVBox.getChildren().addAll(titleHBox, overlayDropdownHBox, tilesetList, cancelDoneHBox);

        stackPane.getChildren().addAll(mainVBox);

        setScene(new Scene(stackPane));
        tilesetList.settle();
        sizeToScene();
        setResizable(false);

        // Force the window to the top of the window stack
        // Note: Stage::toFront() doesn't work for an unknown reason
        this.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<>()
        {
            @Override
            public void handle(WindowEvent window)
            {
                ReplaceOverlayTilesetStage.this.removeEventHandler(WindowEvent.WINDOW_SHOWN, this);
                ReplaceOverlayTilesetStage.this.setAlwaysOnTop(true);
                ReplaceOverlayTilesetStage.this.setAlwaysOnTop(false);
            }
        });
    }

    private void onOverlaySelected(final Integer newValue)
    {
        if (newValue == null)
        {
            tilesetList.setDisable(true);
            doneButton.setDisable(true);
            return;
        }

        final WED.Overlay overlay = wed.getOverlays().get(newValue);
        final String tilesetResref = overlay.getTilesetResref();

        int i = -1;
        for (final TilesetEntry listEntry : tilesetEntries)
        {
            ++i;
            if (listEntry.getText().equals(tilesetResref))
            {
                tilesetList.getSelectionModel().selectIndices(i);
                tilesetList.scrollTo(i);
            }
        }

        tilesetList.setDisable(false);
    }

    private void onTilesetSelected(final TilesetEntry newValue)
    {
        doneButton.setDisable(newValue == null);
    }

    private void onCancel()
    {
        close();
    }

    private void onDone()
    {
        final int overlayIndex = overlayDropdown.getSelectionModel().getSelectedItem();
        final TilesetEntry entry = tilesetList.getSelectionModel().getSelectedItem();
        final WED.Overlay overlay = wed.getOverlays().get(overlayIndex);
        overlay.setTilesetResref(entry.getText());

        final TIS tis = new TIS(
            GlobalState.getGame().getResource(new ResourceIdentifier(overlay.getTilesetResref(), KeyFile.NumericResourceType.TIS)).getPrimarySource(),
            new ResourceDataCache(),
            new SimpleCache<>()
        );
        tis.loadTISTask().run();

        short[] temp = new short[tis.getNumTiles()];
        for (short i = 0; i < tis.getNumTiles(); ++i)
        {
            temp[i] = i;
        }

        for (WED.TilemapEntry tilemapEntry : overlay.getTilemapEntries())
        {
            tilemapEntry.setTisTileIndexArray(temp);
        }

        close();
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private class TilesetEntry implements DynamicListView.Entry
    {
        private final String text;

        public TilesetEntry(String text)
        {
            this.text = text;
        }

        @Override
        public Color getRowColor(boolean isEven)
        {
            return Color.WHITE;
        }

        @Override
        public Color getTextColor(boolean isEven)
        {
            return Color.BLACK;
        }

        @Override
        public String getText()
        {
            return text;
        }
    }
}
