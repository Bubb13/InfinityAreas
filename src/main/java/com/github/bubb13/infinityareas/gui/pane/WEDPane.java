
package com.github.bubb13.infinityareas.gui.pane;

import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.game.resource.WED;
import com.github.bubb13.infinityareas.gui.control.UnderlinedButton;
import com.github.bubb13.infinityareas.gui.dialog.ErrorAlert;
import com.github.bubb13.infinityareas.gui.editor.Editor;
import com.github.bubb13.infinityareas.gui.editor.EditorCommons;
import com.github.bubb13.infinityareas.gui.editor.connector.WEDWallPolygonConnector;
import com.github.bubb13.infinityareas.gui.editor.editmode.AbstractEditMode;
import com.github.bubb13.infinityareas.gui.editor.editmode.DrawPolygonEditMode;
import com.github.bubb13.infinityareas.gui.editor.editmode.QuickSelectEditMode;
import com.github.bubb13.infinityareas.gui.editor.field.StandardStructureDefinitions;
import com.github.bubb13.infinityareas.gui.editor.renderable.AbstractRenderable;
import com.github.bubb13.infinityareas.gui.editor.renderable.RenderablePolygon;
import com.github.bubb13.infinityareas.gui.stage.ReplaceOverlayTilesetStage;
import com.github.bubb13.infinityareas.misc.DoubleCorners;
import com.github.bubb13.infinityareas.misc.LoadingStageTracker;
import com.github.bubb13.infinityareas.misc.Reference;
import com.github.bubb13.infinityareas.misc.TaskTrackerI;
import com.github.bubb13.infinityareas.misc.TrackedTask;
import com.github.bubb13.infinityareas.util.FileUtil;
import com.github.bubb13.infinityareas.util.ImageUtil;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class WEDPane extends StackPane
{
    ////////////////////
    // Private Fields //
    ////////////////////

    // Data
    private final Reference<WED> wedRef = new Reference<>();

    // GUI
    private final ZoomPane zoomPane = new ZoomPane();
    private final Editor editor = new Editor(zoomPane, this);
    {
        final Comparator<AbstractRenderable> renderingComparator = Comparator.comparingInt(AbstractRenderable::sortWeight);
        editor.setRenderingComparator(renderingComparator);
        editor.setInteractionComparator(renderingComparator.reversed());
    }

    private final StackPane rightNodeParent = new StackPane();
    private Node curRightNode;

    private final VBox defaultRightNode = new VBox()
    {
        @Override
        protected double computeMinWidth(double height)
        {
            return computePrefWidth(height);
        }
    };
    private final CheckBox renderPolygonsCheckbox = new CheckBox("Render Wall Polygons");

    private final SnapshotsPane snapshotsPane = new SnapshotsPane(editor, editor.getSnapshots());
    private final FieldPane fieldPane = new FieldPane();
    private Object fieldPaneOwner = null;

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
        editor.enterEditMode(WEDPaneNormalEditMode.class);

        for (final WED.Polygon polygon : wedRef.get().getPolygons())
        {
            new WallPolygon(polygon);
        }
    }

    private void resetFX()
    {
        changeRightNode(defaultRightNode);
    }

    private void init()
    {
        ///////////////
        // Main VBox //
        ///////////////

        final VBox mainVBox = new VBox();
        mainVBox.setFocusTraversable(false);
        mainVBox.setPadding(new Insets(5, 0, 0, 10));

            //////////////////
            // Toolbar HBox //
            //////////////////

            final HBox toolbar = new HBox(5);
            toolbar.setPadding(new Insets(0, 0, 5, 0));

            final Button saveButton = new Button("Save");
            saveButton.setOnAction((ignored) -> this.onSave());

            final Button viewVisibleObjectsButton = new UnderlinedButton("View Visible Objects");
            viewVisibleObjectsButton.setOnAction((ignored) -> this.onViewVisibleObjects());

            final Button drawPolygonButton = new UnderlinedButton("Draw Wall Polygon");
            drawPolygonButton.setOnAction((ignored) -> editor.enterEditMode(DrawPolygonEditMode.class));

            final Button bisectLine = new UnderlinedButton("Bisect Segment");
            bisectLine.setOnAction((ignored) -> EditorCommons.onBisectLine(editor));

            final Button quickSelect = new UnderlinedButton("Quick Select Vertices");
            quickSelect.setOnAction((ignored) -> editor.enterEditMode(QuickSelectEditMode.class));

            toolbar.getChildren().addAll(saveButton, viewVisibleObjectsButton,
                drawPolygonButton, bisectLine, quickSelect);

            ////////////////////
            // Side Pane VBox //
            ////////////////////

            defaultRightNode.setPadding(new Insets(0, 10, 10, 10));

                //////////////////////////////
                // Render Polygons Checkbox //
                //////////////////////////////

                renderPolygonsCheckbox.selectedProperty().addListener((observable, oldValue, newValue) ->
                    onRenderPolygonsChanged(newValue));

            defaultRightNode.getChildren().addAll(renderPolygonsCheckbox);

            ///////////////////////////////
            // ZoomPane + Side Pane HBox //
            ///////////////////////////////

            final HBox zoomPaneSidePaneHBox = new HBox();
            zoomPaneSidePaneHBox.getChildren().addAll(zoomPane, rightNodeParent);

                //////////////
                // ZoomPane //
                //////////////

                VBox.setVgrow(zoomPane, Priority.ALWAYS);

                ///////////////
                // Side Pane //
                ///////////////

                changeRightNode(defaultRightNode);

        mainVBox.getChildren().addAll(toolbar, zoomPaneSidePaneHBox);
        getChildren().add(mainVBox);

        editor.registerEditMode(WEDPaneNormalEditMode.class, () -> new WEDPaneNormalEditMode(editor));
        editor.registerEditMode(DrawPolygonEditMode.class, DrawWallPolygonEditMode::new);
        editor.registerEditMode(QuickSelectEditMode.class, WEDPaneQuickSelectEditMode::new);
    }

    private void changeRightNode(final Parent newNode)
    {
        if (newNode != curRightNode)
        {
            editor.doOperationMaintainViewportLeft(() ->
            {
                curRightNode = newNode;
                final ObservableList<Node> children = rightNodeParent.getChildren();
                children.clear();
                children.add(newNode);

                // Ensure child Control classes have assigned their skins for proper layout calculations
                newNode.applyCss();
                //newNode.requestLayout();
                // Layout the entire panel so that the underlying ZoomPane calculates its new viewport width
                layout();

                return false;
            });
        }
    }

    private void onSave()
    {
        final Path overridePath = FileUtil.resolveCaseInsensitiveDefault(
            GlobalState.getGame().getRoot(), "override");

        try
        {
            Files.createDirectories(overridePath);
        }
        catch (final Exception e)
        {
            ErrorAlert.openAndWait("Failed to save WED", e);
        }

        final WED wed = wedRef.get();

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

    private void onViewVisibleObjects()
    {
        if (fieldPaneOwner != null)
        {
            editor.unselectAll();
        }

        changeRightNode(snapshotsPane);
    }

    private void onSelectReplaceOverlayTileset()
    {
        final WED wed = wedRef.get();
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

    public class WEDPaneNormalEditMode extends AbstractEditMode
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final Editor editor;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public WEDPaneNormalEditMode(Editor editor)
        {
            this.editor = editor;
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        @Override
        public boolean shouldCaptureObjectPress(final MouseEvent event, final AbstractRenderable renderable)
        {
            return true;
        }

        @Override
        public boolean shouldCaptureObjectDrag(final MouseEvent event, final AbstractRenderable renderable)
        {
            return true;
        }

        @Override
        public void onObjectDragged(final MouseEvent event, final AbstractRenderable renderable)
        {
            renderable.onDragged(event);
        }

        @Override
        public void onBackgroundClicked(final MouseEvent event)
        {
            editor.unselectAll();
        }

        @Override
        public void onKeyPressed(final KeyEvent event)
        {
            final KeyCode key = event.getCode();

            switch (key)
            {
                case ESCAPE ->
                {
                    if (editor.selectedCount() > 0)
                    {
                        event.consume();
                        editor.unselectAll();
                    }
                    else if (rightNodeParent.getChildren().get(0) == snapshotsPane)
                    {
                        changeRightNode(defaultRightNode);
                    }
                }
                case B ->
                {
                    event.consume();
                    EditorCommons.onBisectLine(editor);
                }
                case D ->
                {
                    event.consume();
                    editor.enterEditMode(DrawPolygonEditMode.class);
                }
                case Q ->
                {
                    event.consume();
                    editor.enterEditMode(QuickSelectEditMode.class);
                }
                case DELETE ->
                {
                    event.consume();
                    EditorCommons.deleteSelected(editor);
                }
                case V ->
                {
                    event.consume();
                    onViewVisibleObjects();
                }
            }
        }
    }

    public class WEDPaneQuickSelectEditMode extends QuickSelectEditMode
    {
        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public WEDPaneQuickSelectEditMode()
        {
            super(WEDPane.this.editor);
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        @Override
        public void onKeyPressed(KeyEvent event)
        {
            final KeyCode key = event.getCode();

            switch (key)
            {
                case ESCAPE ->
                {
                    if (editor.selectedCount() == 0 && rightNodeParent.getChildren().get(0) == snapshotsPane)
                    {
                        event.consume();
                        changeRightNode(defaultRightNode);
                        return;
                    }
                }
                case V ->
                {
                    event.consume();
                    onViewVisibleObjects();
                    return;
                }
            }

            super.onKeyPressed(event);
        }
    }

    private class DrawWallPolygonEditMode extends DrawPolygonEditMode<WED.Polygon>
    {
        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public DrawWallPolygonEditMode()
        {
            super(WEDPane.this.editor);
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        @Override
        public void onKeyPressed(KeyEvent event)
        {
            final KeyCode key = event.getCode();

            switch (key)
            {
                case ESCAPE ->
                {
                    if (editor.selectedCount() == 0 && rightNodeParent.getChildren().get(0) == snapshotsPane)
                    {
                        event.consume();
                        changeRightNode(defaultRightNode);
                        return;
                    }
                }
                case V ->
                {
                    event.consume();
                    onViewVisibleObjects();
                    return;
                }
            }

            super.onKeyPressed(event);
        }

        ///////////////////////
        // Protected Methods //
        ///////////////////////

        @Override
        protected WED.Polygon createBackingPolygon()
        {
            return new WED.Polygon(
                (byte)0, (byte)0,
                (short)0, (short)0,
                (short)0, (short)0
            );
        }

        @Override
        protected RenderablePolygon<WED.Polygon> createRenderablePolygon(final WED.Polygon backingPolygon)
        {
            return new WallPolygon(backingPolygon);
        }

        @Override
        protected void saveBackingPolygon(final WED.Polygon polygon)
        {
            wedRef.get().addPolygon(polygon);
        }
    }

    private class WallPolygon extends RenderablePolygon<WED.Polygon>
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private boolean selected;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public WallPolygon(final WED.Polygon wedPolygon)
        {
            super(editor, wedPolygon);
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        @Override
        public boolean isEnabled()
        {
            return renderPolygonsCheckbox.isSelected();
        }

        @Override
        public boolean offerPressCapture(final MouseEvent event)
        {
            return !isDrawing() && super.offerPressCapture(event);
        }

        @Override
        public void onClicked(final MouseEvent mouseEvent)
        {
            editor.select(this);
        }

        @Override
        public void onBeforeSelected()
        {
            // Needs to be before the unselectAll() operation to prevent it from
            // reverting the side pane when deselecting another region
            fieldPaneOwner = this;
            fieldPane.setStructure(StandardStructureDefinitions.WED_WALL_POLYGON,
                new WEDWallPolygonConnector(getPolygon())
            );
            changeRightNode(fieldPane);

            selected = true;
            editor.unselectAll();
            editor.requestDraw();
        }

        @Override
        public void onBeforeAdditionalObjectSelected(final AbstractRenderable renderable)
        {
            editor.unselect(this);
        }

        @Override
        public void onReceiveKeyPress(final KeyEvent event)
        {
            if (event.getCode() == KeyCode.ESCAPE)
            {
                event.consume();
                editor.unselectAll();
            }
        }

        @Override
        public void onUnselected()
        {
            if (fieldPaneOwner == this)
            {
                changeRightNode(defaultRightNode);
            }
            selected = false;
            editor.requestDraw();
        }

        @Override
        public void onRender(final GraphicsContext canvasContext, final double scaleCorrection)
        {
            super.onRender(canvasContext, scaleCorrection);

            if (selected)
            {
                final DoubleCorners corners = getCorners();

                final Point2D canvasPointTopLeft = editor.sourceToCanvasDoublePosition(
                    corners.topLeftX(), corners.topLeftY());

                final Point2D canvasPointBottomRight = editor.sourceToCanvasDoublePosition(
                    corners.bottomRightExclusiveX(), corners.bottomRightExclusiveY());

                canvasContext.setLineWidth(1);
                canvasContext.setStroke(Color.rgb(0, 255, 0));
                canvasContext.strokeRect(
                    canvasPointTopLeft.getX(), canvasPointTopLeft.getY(),
                    canvasPointBottomRight.getX() - canvasPointTopLeft.getX(),
                    canvasPointBottomRight.getY() - canvasPointTopLeft.getY()
                );
            }
        }

        @Override
        public void delete()
        {
            super.delete();
            getPolygon().delete();
        }
    }

    private class SetWEDTask extends TrackedTask<Void>
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final Game.ResourceSource source;

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
            wedRef.set(wed);

            final WED.Graphics wedGraphics = wed.newGraphics();
            wedGraphics.renderOverlays(getTracker(), 0, 1, 2, 3, 4);
            final BufferedImage image = ImageUtil.copyArgb(wedGraphics.getImage());

            editor.reset(image.getWidth(), image.getHeight());
            reset();

            waitForFxThreadToExecute(() ->
            {
                resetFX();
                zoomPane.setImage(image);
            });

            return null;
        }
    }
}
