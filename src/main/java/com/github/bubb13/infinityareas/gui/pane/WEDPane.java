
package com.github.bubb13.infinityareas.gui.pane;

import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.game.resource.WED;
import com.github.bubb13.infinityareas.gui.dialog.ErrorAlert;
import com.github.bubb13.infinityareas.gui.stage.ReplaceOverlayTilesetStage;
import com.github.bubb13.infinityareas.util.JavaFXUtil;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.awt.image.BufferedImage;

public class WEDPane extends StackPane
{
    ////////////////////
    // Private Fields //
    ////////////////////

    // Data
    private WED wed;

    // GUI
    private final ZoomPane zoomPane = new ZoomPane();

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public WEDPane()
    {
        super();
        init();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public JavaFXUtil.TaskManager.ManagedTask<Void> setSourceTask(final Game.ResourceSource source)
    {
        return new SetWEDTask(source);
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

            //////////////////
            // Toolbar HBox //
            //////////////////

            final HBox toolbar = new HBox();
            toolbar.setPadding(new Insets(0, 0, 5, 0));

            final Button saveButton = new Button("Save");
            saveButton.setOnAction((ignored) -> this.onSave());

            final Region padding1 = new Region();
            padding1.setPadding(new Insets(0, 0, 0, 5));

            final MenuButton overlaysDropdown = new MenuButton("Overlays");
            final MenuItem replaceOverlayTisButton = new MenuItem("Replace Overlay Tileset");
            replaceOverlayTisButton.setOnAction((ignored) -> this.onSelectReplaceOverlayTileset());
            overlaysDropdown.getItems().addAll(replaceOverlayTisButton);

            toolbar.getChildren().addAll(saveButton, padding1, overlaysDropdown);


        VBox.setVgrow(zoomPane, Priority.ALWAYS);
        vbox.getChildren().addAll(toolbar, zoomPane);
        getChildren().add(vbox);
    }

    private void onSave()
    {
        System.out.println("onSave()");
    }

    private void onSelectReplaceOverlayTileset()
    {
        final ReplaceOverlayTilesetStage stage = new ReplaceOverlayTilesetStage(wed);
        stage.showAndWait();

        if (wed.checkAndClearChanged())
        {
            JavaFXUtil.runTask(wed.renderOverlaysTask(0, 1, 2, 3, 4)
                .onSucceeded(zoomPane::setImage)
                .onFailed((e) -> ErrorAlert.openAndWait("Failed to render WED", e)));
        }
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private class SetWEDTask extends JavaFXUtil.TaskManager.ManagedTask<Void>
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        final Game.ResourceSource source;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public SetWEDTask(final Game.ResourceSource source)
        {
            this.source = source;
        }

        ///////////////////////
        // Protected Methods //
        ///////////////////////

        @Override
        protected Void call() throws Exception
        {
            final WED wed = new WED(source);
            subtask(wed.loadWEDTask());
            WEDPane.this.wed = wed;

            final BufferedImage image = subtask(wed.renderOverlaysTask(0, 1, 2, 3, 4));
            JavaFXUtil.waitForGuiThreadToExecute(() -> zoomPane.setImage(image));
            return null;
        }
    }
}
