
package com.github.bubb13.infinityareas.gui.pane;

import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.game.resource.WED;
import com.github.bubb13.infinityareas.gui.control.UnderlinedButton;
import com.github.bubb13.infinityareas.gui.dialog.ErrorAlert;
import com.github.bubb13.infinityareas.gui.editor.DrawPolygonEditMode;
import com.github.bubb13.infinityareas.gui.editor.Editor;
import com.github.bubb13.infinityareas.gui.editor.EditorCommons;
import com.github.bubb13.infinityareas.gui.editor.NormalEditMode;
import com.github.bubb13.infinityareas.gui.editor.QuickSelectEditMode;
import com.github.bubb13.infinityareas.gui.editor.RenderablePolygon;
import com.github.bubb13.infinityareas.gui.editor.WEDPolygonDelegator;
import com.github.bubb13.infinityareas.gui.stage.ReplaceOverlayTilesetStage;
import com.github.bubb13.infinityareas.misc.LoadingStageTracker;
import com.github.bubb13.infinityareas.misc.TaskTrackerI;
import com.github.bubb13.infinityareas.misc.TrackedTask;
import com.github.bubb13.infinityareas.util.ImageUtil;
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

    // GUI
    private final ZoomPane zoomPane = new ZoomPane();
    private final Editor editor = new Editor(zoomPane, this);
    private final CheckBox renderPolygonsCheckbox = new CheckBox("Render Polygons");

    // Data
    private final WEDPolygonDelegator wedPolygonDelegator = new WEDPolygonDelegator(renderPolygonsCheckbox::isSelected);
    private WED wed;

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

    public TrackedTask<Void> setSourceTask(final Game.ResourceSource source)
    {
        return new SetWEDTask(source);
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void reset()
    {
        editor.enterEditMode(NormalEditMode.class);

        for (final WED.Polygon polygon : wed.getPolygons())
        {
            new RenderablePolygon(editor, wedPolygonDelegator, polygon);
        }
    }

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

                final Button drawPolygonButton = new UnderlinedButton("Draw Polygon");
                drawPolygonButton.setOnAction((ignored) -> editor.enterEditMode(DrawPolygonEditMode.class));

                final Region padding2 = new Region();
                padding2.setPadding(new Insets(0, 0, 0, 5));

                final Button bisectLine = new UnderlinedButton("Bisect Line");
                bisectLine.setOnAction((ignored) -> EditorCommons.onBisectLine(editor));

                final Region padding3 = new Region();
                padding3.setPadding(new Insets(0, 0, 0, 5));

                final Button quickSelect = new UnderlinedButton("Quick Select");
                quickSelect.setOnAction((ignored) -> editor.enterEditMode(QuickSelectEditMode.class));

                final Region padding4 = new Region();
                padding4.setPadding(new Insets(0, 0, 0, 5));

                final MenuButton overlaysDropdown = new MenuButton("Overlays");
                final MenuItem replaceOverlayTisButton = new MenuItem("Replace Overlay Tileset");
                replaceOverlayTisButton.setOnAction((ignored) -> this.onSelectReplaceOverlayTileset());
                overlaysDropdown.getItems().addAll(replaceOverlayTisButton);

                toolbar.getChildren().addAll(saveButton, padding1, drawPolygonButton,
                    padding2, bisectLine, padding3, quickSelect, padding4, overlaysDropdown);

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

        editor.registerEditMode(NormalEditMode.class, () -> new NormalEditMode(editor));
        editor.registerEditMode(DrawPolygonEditMode.class, () -> new DrawPolygonEditMode(editor, wedPolygonDelegator));
        editor.registerEditMode(QuickSelectEditMode.class, () -> new QuickSelectEditMode(editor));
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

        GlobalState.pushModalStage(null);
        final File selectedFile = fileChooser.showSaveDialog(null);
        GlobalState.popModalStage(null);

        if (selectedFile == null)
        {
            return;
        }

        wed.saveWEDTask(selectedFile.toPath())
            .trackWith(new LoadingStageTracker())
            .onFailed((e) -> ErrorAlert.openAndWait("Failed to save WED", e))
            .start();
    }

    private void onSelectReplaceOverlayTileset()
    {
        final ReplaceOverlayTilesetStage stage = new ReplaceOverlayTilesetStage(wed);
        GlobalState.registerStage(stage);
        stage.showAndWait();
        GlobalState.deregisterStage(stage);

        if (wed.checkAndClearChanged())
        {
            wed.renderOverlaysTask(0, 1, 2, 3, 4)
                .trackWith(new LoadingStageTracker())
                .onSucceededFx(zoomPane::setImage)
                .onFailed((e) -> ErrorAlert.openAndWait("Failed to render WED", e))
                .start();
        }
    }

    private void onRenderPolygonsChanged(final boolean ignored)
    {
        zoomPane.requestDraw();
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private class SetWEDTask extends TrackedTask<Void>
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
        protected Void doTask() throws Exception
        {
            final TaskTrackerI tracker = getTracker();
            tracker.updateMessage("Processing WED ...");
            tracker.updateProgress(0, 1);

            final WED wed = new WED(source);
            wed.load(getTracker());
            WEDPane.this.wed = wed;

            final WED.Graphics wedGraphics = wed.newGraphics();
            wedGraphics.renderOverlays(getTracker(), 0, 1, 2, 3, 4);
            final BufferedImage image = ImageUtil.copyArgb(wedGraphics.getImage());

            editor.reset(image.getWidth(), image.getHeight());
            wedPolygonDelegator.setWED(wed);
            reset();

            waitForFxThreadToExecute(() -> zoomPane.setImage(image));
            return null;
        }
    }
}
