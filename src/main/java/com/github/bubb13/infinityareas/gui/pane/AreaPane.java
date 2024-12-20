
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
import com.github.bubb13.infinityareas.misc.ReadableDoublePoint;
import com.github.bubb13.infinityareas.misc.tasktracking.LoadingStageTracker;
import com.github.bubb13.infinityareas.misc.tasktracking.TaskTrackerI;
import com.github.bubb13.infinityareas.misc.tasktracking.TrackedTask;
import com.github.bubb13.infinityareas.misc.undoredo.IUndoHandle;
import com.github.bubb13.infinityareas.util.FileUtil;
import com.github.bubb13.infinityareas.util.ImageUtil;
import com.github.bubb13.infinityareas.util.MiscUtil;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
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
import javafx.scene.transform.Affine;
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

    //------//
    // Data //
    //------//

    private Area area;
    private AreaSearchMap searchMap;

    //-----//
    // GUI //
    //-----//

    //++++++++
    // Core ++
    //++++++++

    private final ZoomPane zoomPane = new ZoomPane();
    private final Editor editor = new Editor(zoomPane, this);
    {
        final Comparator<AbstractRenderable> renderingComparator = Comparator.comparingInt(AbstractRenderable::sortWeight);
        editor.setRenderingComparator(renderingComparator);
        editor.setInteractionComparator(renderingComparator.reversed());
    }
    private SearchMapImage searchMapImage;

    //+++++++++++
    // Toolbar ++
    //+++++++++++

    //----------------------------
    // State tracking variables --
    //----------------------------

    private final StackPane toolbarParent = new StackPane();
    private Parent curToolbarNode;

    //---------------
    // GUI Objects --
    //---------------

    // Default toolbar
    private final HBox defaultToolbarNode = new HBox(5);

    // Search Map Edit Mode toolbar
    private final ArrayList<ColorButton> searchMapEditModeToolbarColorButtons = new ArrayList<>();
    private final FlowPane searchMapEditModeToolbarButtonsFlow = new FlowPane(Orientation.HORIZONTAL, 5, 5);

    //++++++++++++++++++++
    // Right-Side Panel ++
    //++++++++++++++++++++

    //----------------------------
    // State tracking variables --
    //----------------------------

    private final StackPane rightNodeParent = new StackPane();
    private Parent curRightNode;

    //---------------
    // GUI Objects --
    //---------------

    // Default Panel
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
    private Slider searchMapOpacitySlider;

    // Snapshots panel (View Visible Objects)
    private final SnapshotsPane snapshotsPane = new SnapshotsPane(editor, editor.getSnapshots());

    // Fields panel (object selected)
    private final FieldPane fieldPane = new FieldPane();
    private Object fieldPaneOwner = null;

    // Search Map Edit Mode panel
    private final VBox searchMapEditModeRightPane = new VBox(10)
    {
        @Override
        protected double computeMinWidth(double height)
        {
            return computePrefWidth(height);
        }
    };
    private Slider searchMapEditModeOpacitySlider;
    private Slider searchMapEditModeBrushSizeSlider;

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

                    //////////////////////////////
                    // Default Toolbar FlowPane //
                    //////////////////////////////

                    final FlowPane defaultToolbarFlowPane = new FlowPane(Orientation.HORIZONTAL, 5, 5);
                    defaultToolbarFlowPane.prefWrapLengthProperty().bind(zoomPane.widthProperty());

                        //////////////////////////////////////
                        // Default Toolbar FlowPane Buttons //
                        //////////////////////////////////////

                        final Button saveButton = new Button("Save");
                        saveButton.setOnAction((ignored) -> this.onSave());

                        final Button viewVisibleObjectsButton = new UnderlinedButton("View Visible Objects");
                        viewVisibleObjectsButton.setOnAction((ignored) -> this.onViewVisibleObjects());

                        final Button drawPolygonButton = new UnderlinedButton("Draw Region Polygon");
                        drawPolygonButton.setOnAction((ignored) -> editor.enterEditModeUndoable(DrawPolygonEditMode.class));

                        final Button bisectLine = new UnderlinedButton("Bisect Segment");
                        bisectLine.setOnAction((ignored) -> EditorCommons.onBisectLine(editor));

                        final Button quickSelect = new UnderlinedButton("Quick Select Vertices");
                        quickSelect.setOnAction((ignored) -> editor.enterEditModeUndoable(QuickSelectEditMode.class));

                        final Button editSearchMap = new Button("Edit Search Map");
                        editSearchMap.setOnAction((ignored) -> editor.enterEditModeUndoable(SearchMapEditMode.class));

                    defaultToolbarFlowPane.getChildren().addAll(saveButton, viewVisibleObjectsButton,
                        drawPolygonButton, bisectLine, quickSelect, editSearchMap);

                defaultToolbarNode.getChildren().addAll(defaultToolbarFlowPane);

                ///////////////////////////////////////
                // Edit Search Map Mode Toolbar HBox //
                ///////////////////////////////////////

                searchMapEditModeToolbarButtonsFlow.setPadding(new Insets(0, 0, 5, 0));
                searchMapEditModeToolbarButtonsFlow.prefWrapLengthProperty().bind(zoomPane.widthProperty());

                    ////////////////
                    // End Button //
                    ////////////////

                    final Button searchMapEditModeEndButton = new Button("End");
                    searchMapEditModeEndButton.setOnAction((ignored) -> editor.cancelEditModeUndoable()); // TODO Undoable
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

                searchMapEditModeRightPane.setPadding(new Insets(0, 10, 10, 10));

                ////////////////////
                // Opacity Slider //
                ////////////////////

                final LabeledNode<Slider> labeledSearchMapEditModeOpacitySlider
                    = createNewSearchMapOpacitySlider(0);

                searchMapEditModeOpacitySlider = labeledSearchMapEditModeOpacitySlider.getNode();

                ///////////////////////
                // Brush Size Slider //
                ///////////////////////

                final LabeledNode<Slider> labeledSearchMapEditModeBrushSizeSlider
                    = createNewSearchMapBrushSizeSlider(0);

                searchMapEditModeBrushSizeSlider = labeledSearchMapEditModeBrushSizeSlider.getNode();

            searchMapEditModeRightPane.getChildren().addAll(labeledSearchMapEditModeOpacitySlider,
                labeledSearchMapEditModeBrushSizeSlider);

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
        editor.registerEditMode(SearchMapEditMode.class, SearchMapEditMode::new);
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

    private LabeledNode<Slider> createNewSearchMapBrushSizeSlider(final double insetLeft)
    {
        final Slider searchMapBrushSizeSlider = new Slider(1, 10, 1)
        {
            @Override
            protected double computePrefWidth(double height)
            {
                return 150;
            }
        };

        searchMapBrushSizeSlider.setShowTickLabels(true);
        searchMapBrushSizeSlider.setShowTickMarks(true);
        searchMapBrushSizeSlider.setMajorTickUnit(1);
        searchMapBrushSizeSlider.setMinorTickCount(0);
        searchMapBrushSizeSlider.setBlockIncrement(1);
        // Slider::setSnapToTicks() does nothing(?!)
        searchMapBrushSizeSlider.valueProperty().addListener((observable, oldValue, newValue)
            -> searchMapBrushSizeSlider.setValue(MiscUtil.snapToNearest(newValue.doubleValue(), 1)));

        final LabeledNode<Slider> labeledSearchMapBrushSizeSlider = new LabeledNode<>(
            "Brush Size", searchMapBrushSizeSlider, Pos.TOP_LEFT
        );
        labeledSearchMapBrushSizeSlider.setPadding(new Insets(0, 5, 0, insetLeft));

        return labeledSearchMapBrushSizeSlider;
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

    //------------//
    // Edit Modes //
    //------------//

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
                    editor.enterEditModeUndoable(DrawPolygonEditMode.class);
                }
                case Q ->
                {
                    event.consume();
                    editor.enterEditModeUndoable(QuickSelectEditMode.class);
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
        public void onModeExit()
        {
            super.onModeExit();
            regionEditorObject = null;
        }

        @Override
        public void reset()
        {
            super.reset();
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
        protected void removeDrawingPolygonRenderObjects()
        {
            if (regionEditorObject != null)
            {
                regionEditorObject.removeRenderable(false);
            }
        }
    }

    private class SearchMapEditMode extends LabeledEditMode
    {
        ///////////////////////////
        // Private Static Fields //
        ///////////////////////////

        private static final Color OUTLINE_COLOR = Color.rgb(0, 255, 0);

        ////////////////////
        // Private Fields //
        ////////////////////

        private final BrushOutlineState brushOutlineState = new BrushOutlineState();
        private Parent savedToolbarNode;
        private Parent savedRightNode;
        private int drawingPaletteIndex;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public SearchMapEditMode()
        {
            super(AreaPane.this.editor, "Edit Search Map Mode");
            init();
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        @Override
        public void onDraw(GraphicsContext canvasContext)
        {
            super.onDraw(canvasContext);

            final Canvas canvas = canvasContext.getCanvas();
            final Point2D mousePositionScreen = GlobalState.getMousePosition();
            final Point2D mousePositionRelativeCanvas = canvas.screenToLocal(mousePositionScreen);

            if (!canvas.contains(mousePositionRelativeCanvas))
            {
                return;
            }

            final Point2D mousePositionAbsoluteCanvas = canvas.localToParent(mousePositionRelativeCanvas);

            final Point2D imagePoint = searchMapImage.absoluteCanvasToSourceImagePoint(
                mousePositionAbsoluteCanvas.getX(), mousePositionAbsoluteCanvas.getY());

            final Point2D relativeCanvasPoint = searchMapImage.sourceImageToRelativeCanvasPoint(
                (int)imagePoint.getX(), (int)imagePoint.getY());

            final Affine savedTransform = canvasContext.getTransform();
            canvasContext.translate(relativeCanvasPoint.getX(), relativeCanvasPoint.getY());
            canvasContext.setStroke(OUTLINE_COLOR);

            final ArrayList<Point2D> brushOutlinePoints = brushOutlineState.points;
            final int limit = brushOutlinePoints.size();
            for (int i = 0; i < limit; i += 2)
            {
                final Point2D p1 = brushOutlinePoints.get(i);
                final Point2D p2 = brushOutlinePoints.get(i + 1);
                canvasContext.strokeLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
            }

            canvasContext.setTransform(savedTransform);
        }

        @Override
        public void onModeStart(final IUndoHandle ownedUndo)
        {
            pushPanes();
            super.onModeStart(ownedUndo); // After pushPanes() so that it doesn't consume a draw request
            searchMapImage.setOpacity(searchMapEditModeOpacitySlider.getValue() / 100);
            calculateOutlinePoints(brushOutlineState, searchMapImage);
        }

        @Override
        public void onModeExit()
        {
            popPanes();
            super.onModeExit(); // After popPanes() so that it doesn't consume a draw request
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
        public void onMouseMoved(final MouseEvent event)
        {
            editor.requestDraw();
        }

        @Override
        public void onZoomFactorChanged(final double newZoomFactor)
        {
            calculateOutlinePoints(brushOutlineState, searchMapImage);
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
        public void onMouseExited(final MouseEvent event)
        {
            editor.requestDraw();
        }

        @Override
        public void onKeyPressed(final KeyEvent event)
        {
            final KeyCode code = event.getCode();

            if (code == KeyCode.ESCAPE)
            {
                event.consume();
                editor.endEditModeUndoable(); // TODO Undoable
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

            searchMapEditModeBrushSizeSlider.valueProperty().addListener((observable, oldValue, newValue) ->
            {
                calculateOutlinePoints(brushOutlineState, searchMapImage);
                editor.requestDraw();
            });
        }

        public void setPixelPaletteIndex(final MouseEvent mouseEvent, final SearchMapImage specificSearchMapImage)
        {
            final double clickX = mouseEvent.getX();
            final double clickY = mouseEvent.getY();

            final Point2D srcPoint = specificSearchMapImage.absoluteCanvasToSourceImagePoint(clickX, clickY);

            final int sourceImageX = (int)srcPoint.getX();
            final int sourceImageY = (int)srcPoint.getY();

            final int brushSize = (int)Math.round(searchMapEditModeBrushSizeSlider.getValue());
            final int brushSizeSquared = brushSize * brushSize;

            final int xLimit = sourceImageX + brushSize;
            final int yLimit = sourceImageY + brushSize;

            final BufferedImage searchMapSourceImage = searchMap.getImage();
            final int searchMapWidth = searchMapSourceImage.getWidth();
            final int searchMapHeight = searchMapSourceImage.getHeight();

            for (int pixelY = sourceImageY - brushSize; pixelY <= yLimit; ++pixelY)
            {
                if (pixelY < 0 || pixelY >= searchMapHeight) continue;

                for (int pixelX = sourceImageX - brushSize; pixelX <= xLimit; ++pixelX)
                {
                    if (pixelX < 0 || pixelX >= searchMapWidth) continue;

                    final int offsetX = pixelX - sourceImageX;
                    final int offsetY = pixelY - sourceImageY;

                    if (offsetX * offsetX + offsetY * offsetY < brushSizeSquared)
                    {
                        searchMap.setPixelPaletteIndex(pixelX, pixelY, drawingPaletteIndex);
                    }
                }
            }
        }

        public void calculateOutlinePoints(
            final BrushOutlineState brushOutlineState, final SearchMapImage specificSearchMapImage)
        {
            final ArrayList<Point2D> points = brushOutlineState.points;
            points.clear();

            final int brushSize = (int)Math.round(searchMapEditModeBrushSizeSlider.getValue());
            final int brushSizeSquared = brushSize * brushSize;

            final int xLimit = brushSize * 2;
            final int yLimit = brushSize * 2;

            final double stretchFactorX = specificSearchMapImage.getStretchFactorX();
            final double stretchFactorY = specificSearchMapImage.getStretchFactorY();

            double minX = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;

            // Scan Left
            for (int pixelY = 0; pixelY <= yLimit; ++pixelY)
            {
                for (int pixelX = 0; pixelX <= xLimit; ++pixelX)
                {
                    final int offsetX = pixelX - brushSize;
                    final int offsetY = pixelY - brushSize;

                    if (offsetX * offsetX + offsetY * offsetY < brushSizeSquared)
                    {
                        final double topY = offsetY * stretchFactorY * editor.getZoomFactor();
                        final double bottomY = (offsetY + 1) * stretchFactorY * editor.getZoomFactor();
                        final double leftLineX = offsetX * stretchFactorX * editor.getZoomFactor();
                        if (leftLineX < minX) minX = leftLineX;
                        points.add(new Point2D(leftLineX, topY));
                        points.add(new Point2D(leftLineX, bottomY));
                        break;
                    }
                }
            }

            // Scan Right
            for (int pixelY = 0; pixelY <= yLimit; ++pixelY)
            {
                for (int pixelX = xLimit; pixelX >= 0; --pixelX)
                {
                    final int offsetX = pixelX - brushSize;
                    final int offsetY = pixelY - brushSize;

                    if (offsetX * offsetX + offsetY * offsetY < brushSizeSquared)
                    {
                        final double topY = offsetY * stretchFactorY * editor.getZoomFactor();
                        final double bottomY = (offsetY + 1) * stretchFactorY * editor.getZoomFactor();
                        final double rightLineX = (offsetX + 1) * stretchFactorX * editor.getZoomFactor();
                        if (rightLineX > maxX) maxX = rightLineX;
                        points.add(new Point2D(rightLineX, topY));
                        points.add(new Point2D(rightLineX, bottomY));
                        break;
                    }
                }
            }

            // Scan Top
            for (int pixelX = 0; pixelX <= xLimit; ++pixelX)
            {
                for (int pixelY = 0; pixelY <= yLimit; ++pixelY)
                {
                    final int offsetX = pixelX - brushSize;
                    final int offsetY = pixelY - brushSize;

                    if (offsetX * offsetX + offsetY * offsetY < brushSizeSquared)
                    {
                        final double leftX = offsetX * stretchFactorX * editor.getZoomFactor();
                        final double rightX = (offsetX + 1) * stretchFactorX * editor.getZoomFactor();
                        final double topLineY = offsetY * stretchFactorY * editor.getZoomFactor();
                        if (topLineY < minY) minY = topLineY;
                        points.add(new Point2D(leftX, topLineY));
                        points.add(new Point2D(rightX, topLineY));
                        break;
                    }
                }
            }

            // Scan Bottom
            for (int pixelX = 0; pixelX <= xLimit; ++pixelX)
            {
                for (int pixelY = yLimit; pixelY >= 0; --pixelY)
                {
                    final int offsetX = pixelX - brushSize;
                    final int offsetY = pixelY - brushSize;

                    if (offsetX * offsetX + offsetY * offsetY < brushSizeSquared)
                    {
                        final double leftX = offsetX * stretchFactorX * editor.getZoomFactor();
                        final double rightX = (offsetX + 1) * stretchFactorX * editor.getZoomFactor();
                        final double bottomLineY = (offsetY + 1) * stretchFactorY * editor.getZoomFactor();
                        if (bottomLineY > maxY) maxY = bottomLineY;
                        points.add(new Point2D(leftX, bottomLineY));
                        points.add(new Point2D(rightX, bottomLineY));
                        break;
                    }
                }
            }

            brushOutlineState.centerOffsetX = stretchFactorX * editor.getZoomFactor() / -2;
            brushOutlineState.centerOffsetY = stretchFactorY * editor.getZoomFactor() / -2;
        }

        private void pushPanes()
        {
            savedToolbarNode = curToolbarNode;
            savedRightNode = curRightNode;
            changeToolbarNode(searchMapEditModeToolbarButtonsFlow);
            changeRightNode(searchMapEditModeRightPane);
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

        public static class BrushOutlineState
        {
            public ArrayList<Point2D> points = new ArrayList<>();
            public double centerOffsetX;
            public double centerOffsetY;
        }
    }

    //--------------------//
    // Renderable Classes //
    //--------------------//

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
            super(AreaPane.this.editor, searchMap.getImage(), x, y, width, height, searchMapOpacitySlider.getValue() / 100);
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
            return editor.getCurrentEditMode().getClass() == SearchMapEditMode.class;
        }

        @Override
        public void onBeforeRemoved(final boolean undoable)
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
            super(AreaPane.this.editor, actor);
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

        public void removeRenderable(final boolean undoable)
        {
            regionEditorObjectPolygon.removeRenderable(undoable);
            removeConnectedRenderables(undoable);
        }

        ///////////////////////
        // Private Functions //
        ///////////////////////

        public void removeConnectedRenderables(final boolean undoable)
        {
            regionEditorObjectLaunchPosition.removeRenderable(undoable);
            regionEditorObjectActivationPosition.removeRenderable(undoable);
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
                super(AreaPane.this.editor, region.getPolygon());
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
            public void onBeforeRemoved(final boolean undoable)
            {
                super.onBeforeRemoved(undoable);
                RegionEditorObject.this.removeConnectedRenderables(undoable);
            }

            @Override
            public void softDelete()
            {
                super.softDelete();
                region.softDelete();
                editor.pushUndo("RegionEditorObjectPolygon::softDelete", region::restore);
            }

            @Override
            public void delete()
            {
                super.delete();
                RegionEditorObject.this.removeRenderable(false);
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

            public void removeRenderable(final boolean undoable)
            {
                editor.removeRenderable(this, undoable);
                editor.removeRenderable(launchPositionLine, undoable);
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

    //-------//
    // Tasks //
    //-------//

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
        protected Void doTask(final TaskTrackerI tracker) throws Exception
        {
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
        protected Void doTask(final TaskTrackerI tracker) throws Exception
        {
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
