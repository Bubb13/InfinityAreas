
package com.github.bubb13.infinityareas.gui.pane;

import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.game.resource.WED;
import com.github.bubb13.infinityareas.gui.dialog.ErrorAlert;
import com.github.bubb13.infinityareas.gui.stage.ReplaceOverlayTilesetStage;
import com.github.bubb13.infinityareas.util.JavaFXUtil;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class WEDPane extends StackPane
{
    ////////////////////
    // Private Fields //
    ////////////////////

    // Data
    private WED wed;
    private WED.WEDGraphics graphics;

    // GUI
    private final ZoomPane zoomPane = new ZoomPane();
    private final CheckBox renderPolygonsCheckbox = new CheckBox("Render Polygons");

    private final Object zoomRenderLock = new Object();
    private JavaFXUtil.TaskManager.ManagedTask<Void> curZoomRenderTask;

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
        ///////////////
        // Main HBox //
        ///////////////

            final HBox mainHBox = new HBox();

            ///////////////
            // Main VBox //
            ///////////////

            final VBox mainVBox = new VBox();
            mainVBox.setFocusTraversable(false);
            mainVBox.setPadding(new Insets(5, 0, 0, 10));

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

            zoomPane.setZoomFactorListener(this::onZoomFactorChanged);
            VBox.setVgrow(zoomPane, Priority.ALWAYS);
            mainVBox.getChildren().addAll(toolbar, zoomPane);

            ////////////////////
            // Side Pane VBox //
            ////////////////////

            final VBox sidePaneVBox = new VBox();
            sidePaneVBox.setMinWidth(150);
            sidePaneVBox.setPadding(new Insets(5, 10, 10, 10));

                //////////////////////////////
                // Render Polygons Checkbox //
                //////////////////////////////

                renderPolygonsCheckbox.selectedProperty().addListener((observable, oldValue, newValue) ->
                    onRenderPolygonsChanged(newValue));

            sidePaneVBox.getChildren().addAll(renderPolygonsCheckbox);

        mainHBox.getChildren().addAll(mainVBox, sidePaneVBox);
        getChildren().add(mainHBox);
    }

    private void onZoomFactorChanged(final double zoomFactor)
    {
        if (renderPolygonsCheckbox.isSelected())
        {
            if (curZoomRenderTask != null)
            {
                synchronized (zoomRenderLock)
                {
                    curZoomRenderTask.cancel();
                }
            }

            final WED.WEDGraphics graphics = wed.newGraphics();

            curZoomRenderTask = new JavaFXUtil.TaskManager.ManagedTask<>()
            {
                @Override
                protected Void call() throws Exception
                {
                    graphics.renderOverlays(this, 0, 1, 2, 3, 4);
                    if (Thread.interrupted()) return null;
                    graphics.renderPolygons(calculatePolygonRenderWidth());
                    if (Thread.interrupted()) return null;
                    final BufferedImage image = graphics.getImage();
                    if (Thread.interrupted()) return null;
                    synchronized (zoomRenderLock)
                    {
                        JavaFXUtil.waitForGuiThreadToExecute(() -> zoomPane.setImage(image, false));
                    }
                    return null;
                }
            };
            JavaFXUtil.runTaskNoManager(curZoomRenderTask);
        }
    }

    private void onSave()
    {
        final Path overridePath = GlobalState.getGame().getRoot().resolve("override");
        try
        {
            Files.createDirectories(overridePath);
        }
        catch (final Exception e)
        {
            ErrorAlert.openAndWait("Failed to save WED", e);
        }

        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Output File");
        fileChooser.setInitialDirectory(overridePath.toFile());
        fileChooser.setInitialFileName(wed.getSource().getIdentifier().resref() + ".WED");

        final File selectedFile = fileChooser.showSaveDialog(null);
        if (selectedFile == null)
        {
            return;
        }

        JavaFXUtil.runTask(wed.saveWEDTask(selectedFile.toPath())
            .onFailed((e) -> ErrorAlert.openAndWait("Failed to save WED", e))
        );
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

    private float calculatePolygonRenderWidth()
    {
        return (float)(1.75 / zoomPane.getZoomFactor());
    }

    private void onRenderPolygonsChanged(final boolean newValue)
    {
        JavaFXUtil.runTask(new JavaFXUtil.TaskManager.ManagedTask<Void>()
        {
            @Override
            protected Void call() throws Exception
            {
                synchronized (zoomRenderLock)
                {
                    graphics.clear();
                    graphics.renderOverlays(this, 0, 1, 2, 3, 4);

                    if (newValue)
                    {
                        graphics.renderPolygons(calculatePolygonRenderWidth());
                    }

                    final BufferedImage image = graphics.getSnapshot();
                    JavaFXUtil.waitForGuiThreadToExecute(() -> zoomPane.setImage(image, false));
                    return null;
                }
            }
        });
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
            graphics = wed.newGraphics();

            graphics.renderOverlays(this, 0, 1, 2, 3, 4);
            final BufferedImage image = graphics.getSnapshot();

            JavaFXUtil.waitForGuiThreadToExecute(() -> zoomPane.setImage(image));
            return null;
        }
    }
}
