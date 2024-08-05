
package com.github.bubb13.infinityareas.gui.pane;

import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.game.resource.Area;
import com.github.bubb13.infinityareas.game.resource.AreaSearchMap;
import com.github.bubb13.infinityareas.game.resource.KeyFile;
import com.github.bubb13.infinityareas.game.resource.ResourceIdentifier;
import com.github.bubb13.infinityareas.game.resource.WED;
import com.github.bubb13.infinityareas.gui.control.ColorButton;
import com.github.bubb13.infinityareas.gui.control.UnderlinedButton;
import com.github.bubb13.infinityareas.gui.dialog.ErrorAlert;
import com.github.bubb13.infinityareas.gui.editor.Editor;
import com.github.bubb13.infinityareas.gui.editor.EditorCommons;
import com.github.bubb13.infinityareas.gui.editor.GenericPolygon;
import com.github.bubb13.infinityareas.gui.editor.connector.RegionConnector;
import com.github.bubb13.infinityareas.gui.editor.editmode.AbstractEditMode;
import com.github.bubb13.infinityareas.gui.editor.editmode.DrawPolygonEditMode;
import com.github.bubb13.infinityareas.gui.editor.editmode.EditModeForceEnableState;
import com.github.bubb13.infinityareas.gui.editor.editmode.LabeledEditMode;
import com.github.bubb13.infinityareas.gui.editor.editmode.QuickSelectEditMode;
import com.github.bubb13.infinityareas.gui.editor.field.StandardStructureDefinitions;
import com.github.bubb13.infinityareas.gui.editor.field.enums.RegionFields;
import com.github.bubb13.infinityareas.gui.editor.renderable.AbstractRenderable;
import com.github.bubb13.infinityareas.gui.editor.renderable.RenderableActor;
import com.github.bubb13.infinityareas.gui.editor.renderable.RenderableAnchoredLine;
import com.github.bubb13.infinityareas.gui.editor.renderable.RenderableImage;
import com.github.bubb13.infinityareas.gui.editor.renderable.RenderablePoint;
import com.github.bubb13.infinityareas.gui.editor.renderable.RenderablePolygon;
import com.github.bubb13.infinityareas.misc.DoubleCorners;
import com.github.bubb13.infinityareas.misc.IntPoint;
import com.github.bubb13.infinityareas.misc.LoadingStageTracker;
import com.github.bubb13.infinityareas.misc.ReadableDoublePoint;
import com.github.bubb13.infinityareas.misc.TaskTrackerI;
import com.github.bubb13.infinityareas.misc.TrackedTask;
import com.github.bubb13.infinityareas.util.FileUtil;
import com.github.bubb13.infinityareas.util.ImageUtil;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;

public class AreaPane extends StackPane
{
    ////////////////////
    // Private Fields //
    ////////////////////

    // Data
    private Area area;
    private AreaSearchMap searchMap;

    // GUI
    private final ZoomPane zoomPane = new ZoomPane();
    private final Editor editor = new Editor(zoomPane, this);
    {
        final Comparator<AbstractRenderable> renderingComparator = Comparator.comparingInt(AbstractRenderable::sortWeight);
        editor.setRenderingComparator(renderingComparator);
        editor.setInteractionComparator(renderingComparator.reversed());
    }

    private final StackPane toolbarParent = new StackPane();
    private Parent curToolbarNode;

    private final HBox defaultToolbarNode = new HBox(5);
    private final FlowPane searchMapEditModeToolbarButtonsFlow = new FlowPane(Orientation.HORIZONTAL, 5, 5);
    private final ArrayList<ColorButton> searchMapEditModeToolbarColorButtons = new ArrayList<>();

    private final StackPane rightNodeParent = new StackPane();
    private Parent curRightNode;

    private final VBox defaultRightNode = new VBox(10)
    {
        @Override
        protected double computeMinWidth(double height)
        {
            return computePrefWidth(height);
        }
    };
    private final CheckBox renderRegionsCheckbox = new CheckBox("Render Regions");
    private final CheckBox renderSearchMapCheckbox = new CheckBox("Render Search Map");

    private final SnapshotsPane snapshotsPane = new SnapshotsPane(editor, editor.getSnapshots());
    private final FieldPane fieldPane = new FieldPane();
    private Object fieldPaneOwner = null;
    private final VBox editSearchMapPane = new VBox(10)
    {
        @Override
        protected double computeMinWidth(double height)
        {
            return computePrefWidth(height);
        }
    };

    private SearchMapImage searchMapImage;
    private Slider searchMapOpacitySlider;
    private Slider searchMapEditModeOpacitySlider;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public AreaPane()
    {
        super();
        init();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public TrackedTask<Void> setSourceTask(final Game.ResourceSource source)
    {
        return new SetAreaTask(source);
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void reset(final TaskTrackerI tracker) throws Exception
    {
        editor.enterEditMode(AreaPaneNormalEditMode.class);

        for (final Area.Actor actor : area.actors())
        {
            new AreaActor(actor);
        }

        for (final Area.Region region : area.regions())
        {
            new RegionEditorObject(region);
        }

        attemptLoadSearchMap(tracker);
    }

    private void attemptLoadSearchMap(final TaskTrackerI tracker) throws Exception
    {
        final Game game = GlobalState.getGame();
        final String areaSearchMapResref = area.getSource().getIdentifier().resref() + "SR";

        final Game.Resource searchMapResource = game.getResource(
            new ResourceIdentifier(areaSearchMapResref, KeyFile.NumericResourceType.BMP));

        if (searchMapResource == null)
        {
            disableSearchMap();
            return;
        }

        final AreaSearchMap searchMapTemp = new AreaSearchMap(searchMapResource.getPrimarySource().demandFileData());
        try
        {
            searchMapTemp.load(tracker);
        }
        catch (final Exception e)
        {
            disableSearchMap();
            ErrorAlert.openAndWait("Failed to load search map.", e);
            return;
        }

        searchMap = searchMapTemp;
        searchMapImage = new SearchMapImage(0, 0, editor.getSourceWidth(), editor.getSourceHeight());
        searchMapImage.setOpacity(searchMapOpacitySlider.getValue() / 100);
        renderSearchMapCheckbox.setDisable(false);
        searchMapOpacitySlider.setDisable(false);

        loadSearchMapColors();
    }

    private void disableSearchMap()
    {
        searchMap = null;
        searchMapImage = null;
        renderSearchMapCheckbox.setDisable(true);
        searchMapOpacitySlider.setDisable(true);
    }

    private void loadSearchMapColors()
    {
        for (int i = 0; i < 16; ++i)
        {
            final ColorButton button = searchMapEditModeToolbarColorButtons.get(i);
            final int paletteColor = searchMap.getPaletteIndexColor(i);
            button.setColor(paletteColor);
        }
    }

    private void resetFX()
    {
        changeToolbarNode(defaultToolbarNode);
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

            //////////////////////////
            // Default Toolbar HBox //
            //////////////////////////

            final HBox toolbarAndCornerHBox = new HBox();

                //////////////////////////
                // Default Toolbar HBox //
                //////////////////////////

                defaultToolbarNode.setPadding(new Insets(0, 0, 5, 0));

                final Button saveButton = new Button("Save");
                saveButton.setOnAction((ignored) -> this.onSave());

                final Button viewVisibleObjectsButton = new UnderlinedButton("View Visible Objects");
                viewVisibleObjectsButton.setOnAction((ignored) -> this.onViewVisibleObjects());

                final Button drawPolygonButton = new UnderlinedButton("Draw Region Polygon");
                drawPolygonButton.setOnAction((ignored) -> editor.enterEditMode(DrawPolygonEditMode.class));

                final Button bisectLine = new UnderlinedButton("Bisect Segment");
                bisectLine.setOnAction((ignored) -> EditorCommons.onBisectLine(editor));

                final Button quickSelect = new UnderlinedButton("Quick Select Vertices");
                quickSelect.setOnAction((ignored) -> editor.enterEditMode(QuickSelectEditMode.class));

                final Button editSearchMap = new Button("Edit Search Map");
                editSearchMap.setOnAction((ignored) -> editor.enterEditMode(EditSearchMapMode.class));

                defaultToolbarNode.getChildren().addAll(saveButton, viewVisibleObjectsButton,
                    drawPolygonButton, bisectLine, quickSelect, editSearchMap);

                ///////////////////////////////////////
                // Edit Search Map Mode Toolbar HBox //
                ///////////////////////////////////////

                searchMapEditModeToolbarButtonsFlow.setPadding(new Insets(0, 0, 5, 0));
                searchMapEditModeToolbarButtonsFlow.prefWrapLengthProperty().bind(zoomPane.widthProperty());

                    ////////////////
                    // End Button //
                    ////////////////

                    final Button searchMapEditModeEndButton = new Button("End");
                    searchMapEditModeEndButton.setOnAction((ignored) -> editor.exitEditMode());
                    searchMapEditModeToolbarButtonsFlow.getChildren().addAll(searchMapEditModeEndButton);

                    ///////////////////
                    // Color Buttons //
                    ///////////////////

                    searchMapEditModeToolbarColorButtons.add(new ColorButton("Impassable (+LOS & Flight)"));
                    searchMapEditModeToolbarColorButtons.add(new ColorButton("Sand"));
                    searchMapEditModeToolbarColorButtons.add(new ColorButton("Wood 1"));
                    searchMapEditModeToolbarColorButtons.add(new ColorButton("Wood 2"));
                    searchMapEditModeToolbarColorButtons.add(new ColorButton("Stone 1"));
                    searchMapEditModeToolbarColorButtons.add(new ColorButton("Grass 1"));
                    searchMapEditModeToolbarColorButtons.add(new ColorButton("Water 1"));
                    searchMapEditModeToolbarColorButtons.add(new ColorButton("Stone 2"));
                    searchMapEditModeToolbarColorButtons.add(new ColorButton("Impassable"));
                    searchMapEditModeToolbarColorButtons.add(new ColorButton("Wood 3"));
                    searchMapEditModeToolbarColorButtons.add(new ColorButton("Wall, Impassable (+LOS & Flight)"));
                    searchMapEditModeToolbarColorButtons.add(new ColorButton("Water 2"));
                    searchMapEditModeToolbarColorButtons.add(new ColorButton("Water, Impassable"));
                    searchMapEditModeToolbarColorButtons.add(new ColorButton("Roof, Impassable (+LOS & Flight)"));
                    searchMapEditModeToolbarColorButtons.add(new ColorButton("World Map Exit, Impassable"));
                    searchMapEditModeToolbarColorButtons.add(new ColorButton("Grass 2"));
                    searchMapEditModeToolbarButtonsFlow.getChildren().addAll(searchMapEditModeToolbarColorButtons);

                ///////////////////
                // Corner Region //
                ///////////////////

                final Region cornerRegion = new Region();
                cornerRegion.minWidthProperty().bind(rightNodeParent.widthProperty());

            toolbarAndCornerHBox.getChildren().addAll(toolbarParent, cornerRegion);

            ////////////////////////
            // Default Right Node //
            ////////////////////////

            defaultRightNode.setPadding(new Insets(0, 10, 10, 10));

                //////////////////////////////
                // Render Polygons Checkbox //
                //////////////////////////////

                renderRegionsCheckbox.selectedProperty().addListener((observable, oldValue, newValue) ->
                    onRenderRegionsChanged());

                ////////////////////////////////
                // Render Search Map Checkbox //
                ////////////////////////////////

                renderSearchMapCheckbox.selectedProperty().addListener((observable, oldValue, newValue) ->
                    onRenderSearchMapChanged());

                ///////////////////////////////
                // Search Map Opacity Slider //
                ///////////////////////////////

                final LabeledNode<Slider> labeledSearchMapOpacitySlider = createNewSearchMapOpacitySlider(15);
                searchMapOpacitySlider = labeledSearchMapOpacitySlider.getNode();

            defaultRightNode.getChildren().addAll(renderRegionsCheckbox, renderSearchMapCheckbox,
                labeledSearchMapOpacitySlider);

            /////////////////////////////////////
            // Search Map Edit Mode Right Node //
            /////////////////////////////////////

            editSearchMapPane.setPadding(new Insets(0, 10, 10, 10));

            final LabeledNode<Slider> labeledSearchMapEditModeOpacitySlider
                = createNewSearchMapOpacitySlider(0);

            editSearchMapPane.getChildren().add(labeledSearchMapEditModeOpacitySlider);
            searchMapEditModeOpacitySlider = labeledSearchMapEditModeOpacitySlider.getNode();

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

        mainVBox.getChildren().addAll(toolbarAndCornerHBox, zoomPaneSidePaneHBox);
        getChildren().add(mainVBox);

        editor.registerEditMode(AreaPaneNormalEditMode.class, AreaPaneNormalEditMode::new);
        editor.registerEditMode(DrawPolygonEditMode.class, DrawRegionPolygonEditMode::new);
        editor.registerEditMode(QuickSelectEditMode.class, AreaPaneQuickSelectEditMode::new);
        editor.registerEditMode(EditSearchMapMode.class, EditSearchMapMode::new);
    }

    private LabeledNode<Slider> createNewSearchMapOpacitySlider(final double insetLeft)
    {
        final Slider searchMapOpacitySlider = new Slider(0, 100, 50)
        {
            @Override
            protected double computePrefWidth(double height)
            {
                return 150;
            }
        };

        searchMapOpacitySlider.setShowTickLabels(true);
        searchMapOpacitySlider.setShowTickMarks(true);
        searchMapOpacitySlider.setMajorTickUnit(20);
        searchMapOpacitySlider.setMinorTickCount(5);
        searchMapOpacitySlider.setBlockIncrement(1);
        searchMapOpacitySlider.valueProperty().addListener(
            (observable, oldValue, newValue) -> onSearchMapOpacityChanged(newValue.doubleValue() / 100));

        final LabeledNode<Slider> labeledSearchMapOpacitySlider = new LabeledNode<>(
            "Opacity", searchMapOpacitySlider, Pos.TOP_LEFT
        );
        labeledSearchMapOpacitySlider.setPadding(new Insets(0, 5, 0, insetLeft));

        return labeledSearchMapOpacitySlider;
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
            ErrorAlert.openAndWait("Failed to save area.", e);
            return;
        }

        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Output Directory");
        directoryChooser.setInitialDirectory(overridePath.toFile());

        GlobalState.pushModalStage(null);
        final File selectedDirectory = directoryChooser.showDialog(null);
        GlobalState.popModalStage(null);

        if (selectedDirectory == null)
        {
            return;
        }

        new SaveTask(selectedDirectory.toPath())
            .trackWith(new LoadingStageTracker())
            .onFailedFx((e) -> ErrorAlert.openAndWait("Failed to save area.", e))
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

    private void changeToolbarNode(final Parent newNode)
    {
        if (newNode != curToolbarNode)
        {
            editor.doOperationMaintainViewportBottom(() ->
            {
                curToolbarNode = newNode;
                final ObservableList<Node> children = toolbarParent.getChildren();
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

    private void onRenderRegionsChanged()
    {
        editor.requestDraw();
    }

    private void onRenderSearchMapChanged()
    {
        editor.requestDraw();
    }

    private void onSearchMapOpacityChanged(final double opacity)
    {
        searchMapImage.setOpacity(opacity);
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private class AreaPaneNormalEditMode extends AbstractEditMode
    {
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

    private class AreaPaneQuickSelectEditMode extends QuickSelectEditMode
    {
        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public AreaPaneQuickSelectEditMode()
        {
            super(AreaPane.this.editor);
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

    private class DrawRegionPolygonEditMode extends DrawPolygonEditMode<GenericPolygon>
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private RegionEditorObject regionEditorObject;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public DrawRegionPolygonEditMode()
        {
            super(AreaPane.this.editor);
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        @Override
        public void onKeyPressed(final KeyEvent event)
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

        @Override
        public void onModeEnd()
        {
            super.onModeEnd();
            regionEditorObject = null;
        }

        ///////////////////////
        // Protected Methods //
        ///////////////////////

        @Override
        protected GenericPolygon createBackingPolygon()
        {
            final Area.Region region = new Area.Region();
            regionEditorObject = new RegionEditorObject(region);
            return region.getPolygon();
        }

        @Override
        protected RenderablePolygon<GenericPolygon> createRenderablePolygon(final GenericPolygon backingPolygon)
        {
            regionEditorObject.setRegionBeingDrawn(true);
            return regionEditorObject.getRegionEditorObjectPolygon();
        }

        @Override
        protected void saveBackingPolygon(final GenericPolygon polygon)
        {
            final RegionConnector regionConnector = regionEditorObject.getConnector();
            final short centerX = (short)((polygon.getBoundingBoxLeft() + polygon.getBoundingBoxRight()) / 2);
            final short centerY = (short)((polygon.getBoundingBoxTop() + polygon.getBoundingBoxBottom()) / 2);

            regionConnector.setShort(RegionFields.TRAP_LAUNCH_X, centerX);
            regionConnector.setShort(RegionFields.TRAP_LAUNCH_Y, centerY);

            regionConnector.setShort(RegionFields.ACTIVATION_X, centerX);
            regionConnector.setShort(RegionFields.ACTIVATION_Y, centerY);

            regionEditorObject.setRegionBeingDrawn(false);
            area.addRegion(regionEditorObject.getRegion());
        }

        @Override
        protected void cleanUpPolygonDrawingModeRenderObjects()
        {
            if (regionEditorObject != null)
            {
                regionEditorObject.removeRenderable();
            }
        }
    }

    private class EditSearchMapMode extends LabeledEditMode
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private Parent savedToolbarNode;
        private Parent savedRightNode;
        private int drawingPaletteIndex;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public EditSearchMapMode()
        {
            super(AreaPane.this.editor, "Edit Search Map Mode");
            init();
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        @Override
        public void onModeStart()
        {
            pushPanes();
            super.onModeStart(); // After pushPanes() so that it doesn't consume a draw request
            searchMapImage.setOpacity(searchMapEditModeOpacitySlider.getValue() / 100);
        }

        @Override
        public void onModeEnd()
        {
            popPanes();
            super.onModeEnd(); // After popPanes() so that it doesn't consume a draw request
            searchMapImage.setOpacity(searchMapOpacitySlider.getValue() / 100);
        }

        @Override
        public EditModeForceEnableState forceObjectEnableState(final AbstractRenderable renderable)
        {
            return renderable instanceof SearchMapImage
                ? EditModeForceEnableState.ENABLE
                : EditModeForceEnableState.DISABLE;
        }

        @Override
        public boolean shouldCaptureObjectPress(MouseEvent event, AbstractRenderable renderable)
        {
            return true;
        }

        @Override
        public void onDragDetected(final MouseEvent event)
        {
            if (event.getButton() == MouseButton.PRIMARY)
            {
                event.consume();
            }
        }

        @Override
        public boolean customOnObjectClicked(final MouseEvent mouseEvent, final AbstractRenderable renderable)
        {
            if (!(renderable instanceof SearchMapImage specificSearchMapImage))
            {
                return false;
            }

            setPixelPaletteIndex(mouseEvent, specificSearchMapImage);
            return true;
        }

        @Override
        public boolean customOnMouseDragged(final MouseEvent event)
        {
            if (event.getButton() != MouseButton.PRIMARY)
            {
                return false;
            }

            final Point2D sourcePoint = editor.absoluteCanvasToSourceDoublePosition(event.getX(), event.getY());
            if (searchMapImage.contains(sourcePoint))
            {
                setPixelPaletteIndex(event, searchMapImage);
            }

            event.consume();
            return true;
        }

        @Override
        public void onKeyPressed(final KeyEvent event)
        {
            final KeyCode code = event.getCode();

            if (code == KeyCode.ESCAPE)
            {
                event.consume();
                editor.exitEditMode();
            }
        }

        /////////////////////
        // Private Methods //
        /////////////////////

        private void init()
        {
            for (int i = 0; i < 16; ++i)
            {
                final ColorButton colorButton = searchMapEditModeToolbarColorButtons.get(i);
                colorButton.setOnAction(new ColorButtonAction(i));
            }
            setSelectedColorButton(searchMapEditModeToolbarColorButtons.get(0), 0);
        }

        private void setPixelPaletteIndex(final MouseEvent mouseEvent, final SearchMapImage specificSearchMapImage)
        {
            final double clickX = mouseEvent.getX();
            final double clickY = mouseEvent.getY();

            final Point2D srcPoint = specificSearchMapImage.absoluteCanvasToSourceImagePoint(clickX, clickY);

            final int sourceImageX = (int)srcPoint.getX();
            final int sourceImageY = (int)srcPoint.getY();

            searchMap.setPixelPaletteIndex(sourceImageX, sourceImageY, drawingPaletteIndex);
        }

        private void pushPanes()
        {
            savedToolbarNode = curToolbarNode;
            savedRightNode = curRightNode;
            changeToolbarNode(searchMapEditModeToolbarButtonsFlow);
            changeRightNode(editSearchMapPane);
            AreaPane.this.requestFocus(); // Prevent button press from causing focus loss
        }

        private void popPanes()
        {
            changeRightNode(savedRightNode);
            changeToolbarNode(savedToolbarNode);
            savedRightNode = null;
            savedToolbarNode = null;
        }

        private void setSelectedColorButton(final ColorButton colorButton, final int index)
        {
            searchMapEditModeToolbarColorButtons.get(drawingPaletteIndex).getStyleClass().remove("green-button");
            colorButton.getStyleClass().add("green-button");
            drawingPaletteIndex = index;
        }

        /////////////////////
        // Private Classes //
        /////////////////////

        private class ColorButtonAction implements EventHandler<ActionEvent>
        {
            ////////////////////
            // Private Fields //
            ////////////////////

            private final int paletteIndex;

            /////////////////////////
            // Public Constructors //
            /////////////////////////

            public ColorButtonAction(final int paletteIndex)
            {
                this.paletteIndex = paletteIndex;
            }

            ////////////////////
            // Public Methods //
            ////////////////////

            @Override
            public void handle(final ActionEvent event)
            {
                final ColorButton colorButton = (ColorButton)event.getTarget();
                setSelectedColorButton(colorButton, paletteIndex);
            }
        }
    }

    private class SearchMapImage extends RenderableImage
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private AreaSearchMap.PixelPaletteIndexChangedListener listener;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public SearchMapImage(final double x, final double y, final double width, final double height)
        {
            super(editor, searchMap.getImage(), x, y, width, height, searchMapOpacitySlider.getValue() / 100);
            listener = this::onPixelPaletteIndexChanged;
            searchMap.addOnPixelPaletteIndexChangedListener(listener);
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        @Override
        public boolean isEnabled()
        {
            return renderSearchMapCheckbox.isSelected();
        }

        @Override
        public boolean offerPressCapture(MouseEvent event)
        {
            return editor.getCurrentEditMode().getClass() == EditSearchMapMode.class;
        }

        @Override
        public void onBeforeRemoved()
        {
            searchMap.removeOnPixelPaletteIndexChangedListener(listener);
            listener = null;
        }

        /////////////////////
        // Private Methods //
        /////////////////////

        private void onPixelPaletteIndexChanged(final int x, final int y, final int paletteIndex)
        {
            setPixelARGB(x, y, searchMap.getPaletteIndexColor(paletteIndex));
            editor.requestDraw();
        }
    }

    private class AreaActor extends RenderableActor
    {
        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public AreaActor(final Area.Actor actor)
        {
            super(editor, actor);
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        @Override
        public int sortWeight()
        {
            return 3;
        }
    }

    private class RegionEditorObject
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final Area.Region region;
        private final RegionConnector regionConnector;
        private final RegionEditorObjectPolygon regionEditorObjectPolygon;
        private final RegionEditorObjectLaunchPosition regionEditorObjectLaunchPosition;
        private final RegionEditorObjectLaunchPosition regionEditorObjectActivationPosition;
        private boolean selected;
        private boolean regionBeingDrawn;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public RegionEditorObject(final Area.Region region)
        {
            this.region = region;
            this.regionConnector = new RegionConnector(region);
            regionEditorObjectPolygon = new RegionEditorObjectPolygon();
            regionEditorObjectLaunchPosition = new RegionEditorObjectLaunchPosition(
                RegionFields.TRAP_LAUNCH_X, RegionFields.TRAP_LAUNCH_Y, Color.MAGENTA
            );
            regionEditorObjectActivationPosition = new RegionEditorObjectActivationPosition(
                RegionFields.ACTIVATION_X, RegionFields.ACTIVATION_Y, Color.YELLOW
            );
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        public Area.Region getRegion()
        {
            return region;
        }

        public RegionConnector getConnector()
        {
            return regionConnector;
        }

        public RegionEditorObjectPolygon getRegionEditorObjectPolygon()
        {
            return regionEditorObjectPolygon;
        }

        public void setRegionBeingDrawn(final boolean regionBeingDrawn)
        {
            this.regionBeingDrawn = regionBeingDrawn;
        }

        public void removeRenderable()
        {
            regionEditorObjectPolygon.removeRenderable();
            regionEditorObjectLaunchPosition.removeRenderable();
            regionEditorObjectActivationPosition.removeRenderable();
        }

        ////////////////////
        // Public Classes //
        ////////////////////

        public class RegionEditorObjectPolygon extends RenderablePolygon<GenericPolygon>
        {
            /////////////////////////
            // Public Constructors //
            /////////////////////////

            public RegionEditorObjectPolygon()
            {
                super(editor, region.getPolygon());
                setRenderFill(true);
            }

            ////////////////////
            // Public Methods //
            ////////////////////

            @Override
            public boolean isEnabled()
            {
                return renderRegionsCheckbox.isSelected();
            }

            @Override
            public int sortWeight()
            {
                return 1;
            }

            @Override
            public boolean offerPressCapture(final MouseEvent event)
            {
                return !regionBeingDrawn && super.offerPressCapture(event);
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
                fieldPane.setStructure(StandardStructureDefinitions.REGION, regionConnector);
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
                RegionEditorObject.this.removeRenderable();
                region.delete();
            }

            ///////////////////////
            // Protected Methods //
            ///////////////////////

            @Override
            protected Color getLineColor()
            {
                return switch (region.getType())
                {
                    case 0 -> Color.RED;
                    case 1 -> Color.BLUE;
                    case 2 -> Color.WHITE;
                    default -> Color.MAGENTA;
                };
            }
        }

        /////////////////////
        // Private Classes //
        /////////////////////

        private class RegionEditorObjectActivationPosition extends RegionEditorObjectLaunchPosition
        {
            public RegionEditorObjectActivationPosition(
                final RegionFields xField, final RegionFields yField, final Color lineColor)
            {
                super(xField, yField, lineColor);
                regionConnector.addIntListener(RegionFields.FLAGS, (oldValue, newValue) ->
                {
                    if ((oldValue & 0x400) != (newValue & 0x400))
                    {
                        editor.requestDraw();
                    }
                });
            }

            @Override
            public boolean isEnabled()
            {
                return (region.getFlags() & 0x400) != 0 && super.isEnabled();
            }
        }

        private class RegionEditorObjectLaunchPosition extends RenderablePoint<IntPoint>
        {
            ////////////////////
            // Private Fields //
            ////////////////////

            private final RegionEditorObjectLaunchPositionLine launchPositionLine;
            private final RegionFields xField;
            private final RegionFields yField;
            private final Color lineColor;

            /////////////////////////
            // Public Constructors //
            /////////////////////////

            public RegionEditorObjectLaunchPosition(
                final RegionFields xField, final RegionFields yField, final Color lineColor)
            {
                super(AreaPane.this.editor);
                this.xField = xField;
                this.yField = yField;
                this.lineColor = lineColor;
                setBackingObject(new LaunchPointProxy());
                launchPositionLine = new RegionEditorObjectLaunchPositionLine();
                regionConnector.addShortListener(xField, (ignored1, ignored2) -> update());
                regionConnector.addShortListener(yField, (ignored1, ignored2) -> update());
            }

            ////////////////////
            // Public Methods //
            ////////////////////

            public void recalculateLineCorners()
            {
                launchPositionLine.recalculateCorners();
            }

            public void removeRenderable()
            {
                editor.removeRenderable(this);
                editor.removeRenderable(launchPositionLine);
            }

            @Override
            public boolean isEnabled()
            {
                return renderRegionsCheckbox.isSelected() && RegionEditorObject.this.selected;
            }

            @Override
            public int sortWeight()
            {
                return 5;
            }

            @Override
            public boolean offerPressCapture(final MouseEvent event)
            {
                return event.getButton() == MouseButton.PRIMARY;
            }

            @Override
            public boolean offerDragCapture(final MouseEvent event)
            {
                return true;
            }

            /////////////////////
            // Private Methods //
            /////////////////////

            private void update()
            {
                recalculateCorners();
                recalculateLineCorners();
            }

            /////////////////////
            // Private Classes //
            /////////////////////

            private class LaunchPointProxy implements IntPoint
            {
                @Override
                public int getX()
                {
                    return regionConnector.getShort(xField);
                }

                @Override
                public void setX(int x)
                {
                    regionConnector.setShort(xField, (short)x);
                }

                @Override
                public int getY()
                {
                    return regionConnector.getShort(yField);
                }

                @Override
                public void setY(int y)
                {
                    regionConnector.setShort(yField, (short)y);
                }
            }

            private class RegionEditorObjectLaunchPositionLine extends RenderableAnchoredLine<ReadableDoublePoint>
            {
                /////////////////////////
                // Public Constructors //
                /////////////////////////

                public RegionEditorObjectLaunchPositionLine()
                {
                    super(AreaPane.this.editor);
                    setBackingObjects(
                        new BackingObjectPointProxy(), new RegionEditorObjectPolygonCenterProxy(),
                        RegionEditorObjectLaunchPosition.this.getCorners(), regionEditorObjectPolygon.getCorners()
                    );
                }

                ////////////////////
                // Public Methods //
                ////////////////////

                @Override
                public boolean isEnabled()
                {
                    return RegionEditorObjectLaunchPosition.this.isEnabled();
                }

                @Override
                public int sortWeight()
                {
                    return RegionEditorObjectLaunchPosition.this.sortWeight();
                }

                ///////////////////////
                // Protected Methods //
                ///////////////////////

                @Override
                protected Color getLineColor()
                {
                    return lineColor;
                }

                /////////////////////
                // Private Classes //
                /////////////////////

                private class BackingObjectPointProxy implements ReadableDoublePoint
                {
                    @Override
                    public double getX()
                    {
                        return backingObject.getX();
                    }

                    @Override
                    public double getY()
                    {
                        return backingObject.getY();
                    }
                }

                private class RegionEditorObjectPolygonCenterProxy implements ReadableDoublePoint
                {
                    @Override
                    public double getX()
                    {
                        final DoubleCorners corners = regionEditorObjectPolygon.getCorners();
                        return (corners.topLeftX() + corners.bottomRightExclusiveX()) / 2;
                    }

                    @Override
                    public double getY()
                    {
                        final DoubleCorners corners = regionEditorObjectPolygon.getCorners();
                        return (corners.topLeftY() + corners.bottomRightExclusiveY()) / 2;
                    }
                }
            }
        }
    }

    private class SetAreaTask extends TrackedTask<Void>
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final Game.ResourceSource source;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public SetAreaTask(final Game.ResourceSource source)
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
            tracker.updateMessage("Processing area ...");
            tracker.updateProgress(0, 1);

            final Area areaTemp = new Area(source);
            areaTemp.load(getTracker());
            area = areaTemp;

            final WED.Graphics wedGraphics = area.newGraphics().getWedGraphics();
            wedGraphics.renderOverlays(getTracker(), 0, 1, 2, 3, 4);
            final BufferedImage image = ImageUtil.copyArgb(wedGraphics.getImage());

            editor.reset(image.getWidth(), image.getHeight());
            waitForFxThreadToExecute(() ->
            {
                resetFX();
                zoomPane.setImage(image);
            });
            reset(getTracker());

            return null;
        }
    }

    private class SaveTask extends TrackedTask<Void>
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final Path outputDirectory;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public SaveTask(final Path outputDirectory)
        {
            this.outputDirectory = outputDirectory;
        }

        ///////////////////////
        // Protected Methods //
        ///////////////////////

        @Override
        protected Void doTask() throws Exception
        {
            final TaskTrackerI tracker = getTracker();
            tracker.updateMessage("Saving ...");
            tracker.updateProgress(0, 1);

            final String areaResref = area.getSource().getIdentifier().resref();

            final Path outputAreaPath = outputDirectory.resolve(areaResref + ".ARE");
            final Path outputSearchMapPath = outputDirectory.resolve(areaResref + "SR.BMP");

            if (FileUtil.hasFileConflict(outputAreaPath)) return null;
            if (searchMap.hasUnsavedChanges())
            {
                if (FileUtil.hasFileConflict(outputSearchMapPath)) return null;
            }

            area.save(getTracker(), outputAreaPath);

            if (searchMap.hasUnsavedChanges())
            {
                ImageIO.write(searchMap.getImage(), "BMP", outputSearchMapPath.toFile());
                searchMap.clearHasUnsavedChanges();
            }

            tracker.updateProgress(1, 1);
            return null;
        }
    }
}
