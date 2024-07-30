
package com.github.bubb13.infinityareas.gui.pane;

import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.game.resource.Area;
import com.github.bubb13.infinityareas.game.resource.WED;
import com.github.bubb13.infinityareas.gui.control.UnderlinedButton;
import com.github.bubb13.infinityareas.gui.dialog.ErrorAlert;
import com.github.bubb13.infinityareas.gui.editor.Editor;
import com.github.bubb13.infinityareas.gui.editor.EditorCommons;
import com.github.bubb13.infinityareas.gui.editor.GenericPolygon;
import com.github.bubb13.infinityareas.gui.editor.connector.RegionConnector;
import com.github.bubb13.infinityareas.gui.editor.editmode.DrawPolygonEditMode;
import com.github.bubb13.infinityareas.gui.editor.editmode.QuickSelectEditMode;
import com.github.bubb13.infinityareas.gui.editor.editmode.areapane.AreaPaneNormalEditMode;
import com.github.bubb13.infinityareas.gui.editor.field.RegionFields;
import com.github.bubb13.infinityareas.gui.editor.gui.FieldPane;
import com.github.bubb13.infinityareas.gui.editor.gui.StandardStructureDefinitions;
import com.github.bubb13.infinityareas.gui.editor.renderable.AbstractRenderable;
import com.github.bubb13.infinityareas.gui.editor.renderable.RenderableActor;
import com.github.bubb13.infinityareas.gui.editor.renderable.RenderableClippedLine;
import com.github.bubb13.infinityareas.gui.editor.renderable.RenderablePoint;
import com.github.bubb13.infinityareas.gui.editor.renderable.RenderablePolygon;
import com.github.bubb13.infinityareas.misc.DoubleCorners;
import com.github.bubb13.infinityareas.misc.IntPoint;
import com.github.bubb13.infinityareas.misc.LoadingStageTracker;
import com.github.bubb13.infinityareas.misc.ReadableDoublePoint;
import com.github.bubb13.infinityareas.misc.TaskTrackerI;
import com.github.bubb13.infinityareas.misc.TrackedTask;
import com.github.bubb13.infinityareas.util.ImageUtil;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

public class AreaPane extends StackPane
{
    ////////////////////
    // Private Fields //
    ////////////////////

    // GUI
    private final CheckBox renderRegionsCheckbox = new CheckBox("Render Regions");

    private final ZoomPane zoomPane = new ZoomPane();
    private final Editor editor = new Editor(zoomPane, this);
    {
        final Comparator<AbstractRenderable> renderingComparator = Comparator.comparingInt(AbstractRenderable::sortWeight);
        editor.setRenderingComparator(renderingComparator);
        editor.setInteractionComparator(renderingComparator.reversed());
    }

    private final StackPane rightPane = new StackPane();
    private Node curRightNode;

    private VBox defaultRightNode;
    private final FieldPane fieldPane = new FieldPane();
    private Object fieldPaneOwner = null;

    private Area area;

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

    private void reset()
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

            final Button drawPolygonButton = new UnderlinedButton("Draw Region Polygon");
            drawPolygonButton.setOnAction((ignored) -> editor.enterEditMode(DrawPolygonEditMode.class));

            final Button bisectLine = new UnderlinedButton("Bisect Line");
            bisectLine.setOnAction((ignored) -> EditorCommons.onBisectLine(editor));

            final Button quickSelect = new UnderlinedButton("Quick Select");
            quickSelect.setOnAction((ignored) -> editor.enterEditMode(QuickSelectEditMode.class));

            toolbar.getChildren().addAll(saveButton, drawPolygonButton, bisectLine, quickSelect);

            ////////////////////
            // Side Pane VBox //
            ////////////////////

            defaultRightNode = new VBox();
            defaultRightNode.setMinWidth(150);
            defaultRightNode.setPadding(new Insets(0, 10, 10, 10));

                //////////////////////////////
                // Render Polygons Checkbox //
                //////////////////////////////

                renderRegionsCheckbox.selectedProperty().addListener((observable, oldValue, newValue) ->
                    onRenderRegionsChanged(newValue));

            defaultRightNode.getChildren().addAll(renderRegionsCheckbox);

            ///////////////////////////////
            // ZoomPane + Side Pane HBox //
            ///////////////////////////////

            final HBox zoomPaneSidePaneHBox = new HBox();
            zoomPaneSidePaneHBox.getChildren().addAll(zoomPane, rightPane);

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

        editor.registerEditMode(AreaPaneNormalEditMode.class, () -> new AreaPaneNormalEditMode(editor));
        editor.registerEditMode(DrawPolygonEditMode.class, DrawRegionPolygonEditMode::new);
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
            ErrorAlert.openAndWait("Failed to save area", e);
        }

        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Output File");
        fileChooser.setInitialDirectory(overridePath.toFile());
        fileChooser.setInitialFileName(area.getSource().getIdentifier().resref() + ".ARE");

        GlobalState.pushModalStage(null);
        final File selectedFile = fileChooser.showSaveDialog(null);
        GlobalState.popModalStage(null);

        if (selectedFile == null)
        {
            return;
        }

        area.saveTask(selectedFile.toPath())
            .trackWith(new LoadingStageTracker())
            .onFailed((e) -> ErrorAlert.openAndWait("Failed to save area", e))
            .start();
    }

    private void changeRightNode(final Parent newNode)
    {
        if (newNode != curRightNode)
        {
            editor.doOperationMaintainViewportLeft(() ->
            {
                curRightNode = newNode;
                final ObservableList<Node> children = rightPane.getChildren();
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

    private void onRenderRegionsChanged(final boolean newValue)
    {
        editor.requestDraw();
    }

    /////////////////////
    // Private Classes //
    /////////////////////

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
            if (event.getCode() == KeyCode.Q)
            {
                event.consume();
                editor.enterEditMode(QuickSelectEditMode.class);
            }
            else
            {
                super.onKeyPressed(event);
            }
        }

        @Override
        public void onExitMode()
        {
            super.onExitMode();
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
            final Area.Region region = regionEditorObject.getRegion();

            final int centerX = (polygon.getBoundingBoxLeft() + polygon.getBoundingBoxRight()) / 2;
            final int centerY = (polygon.getBoundingBoxTop() + polygon.getBoundingBoxBottom()) / 2;

            final IntPoint launchPoint = region.getTrapLaunchPoint();
            launchPoint.setX(centerX);
            launchPoint.setY(centerY);

            final RegionEditorObject.RegionEditorObjectLaunchPosition launchPosition
                = regionEditorObject.getRegionEditorObjectLaunchPosition();

            launchPosition.recalculateCorners();
            launchPosition.recalculateLineCorners();

            regionEditorObject.setRegionBeingDrawn(false);
            area.addRegion(region);
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
            return 2;
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
            regionEditorObjectLaunchPosition = new RegionEditorObjectLaunchPosition();
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        public Area.Region getRegion()
        {
            return region;
        }

        public RegionEditorObjectPolygon getRegionEditorObjectPolygon()
        {
            return regionEditorObjectPolygon;
        }

        public RegionEditorObjectLaunchPosition getRegionEditorObjectLaunchPosition()
        {
            return regionEditorObjectLaunchPosition;
        }

        public void setRegionBeingDrawn(final boolean regionBeingDrawn)
        {
            this.regionBeingDrawn = regionBeingDrawn;
        }

        public void removeRenderable()
        {
            regionEditorObjectPolygon.removeRenderable();
            regionEditorObjectLaunchPosition.removeRenderable();
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
            public void onRender(final GraphicsContext canvasContext)
            {
                super.onRender(canvasContext);

                if (selected)
                {
                    final DoubleCorners corners = getCorners();

                    final Point2D canvasPointTopLeft = editor.sourceToAbsoluteCanvasDoublePosition(
                        corners.topLeftX(), corners.topLeftY());

                    final Point2D canvasPointBottomRight = editor.sourceToAbsoluteCanvasDoublePosition(
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

        private class RegionEditorObjectLaunchPosition extends RenderablePoint<IntPoint>
        {
            ////////////////////
            // Private Fields //
            ////////////////////

            private final RegionEditorObjectLaunchPositionLine launchPositionLine;

            /////////////////////////
            // Public Constructors //
            /////////////////////////

            public RegionEditorObjectLaunchPosition()
            {
                super(AreaPane.this.editor);
                setBackingObject(new LaunchPointProxy());
                launchPositionLine = new RegionEditorObjectLaunchPositionLine();
                regionConnector.addShortListener(RegionFields.TRAP_LAUNCH_X, (ignored) -> update());
                regionConnector.addShortListener(RegionFields.TRAP_LAUNCH_Y, (ignored) -> update());
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
                return 3;
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
                    return regionConnector.getShort(RegionFields.TRAP_LAUNCH_X);
                }

                @Override
                public void setX(int x)
                {
                    regionConnector.setShort(RegionFields.TRAP_LAUNCH_X, (short)x);
                }

                @Override
                public int getY()
                {
                    return regionConnector.getShort(RegionFields.TRAP_LAUNCH_Y);
                }

                @Override
                public void setY(int y)
                {
                    regionConnector.setShort(RegionFields.TRAP_LAUNCH_Y, (short)y);
                }
            }

            private class RegionEditorObjectLaunchPositionLine extends RenderableClippedLine<ReadableDoublePoint>
            {
                ////////////////////
                // Private Fields //
                ////////////////////

                private final Collection<Rectangle2D> exclusionRectangles = new ArrayList<>();

                /////////////////////////
                // Public Constructors //
                /////////////////////////

                public RegionEditorObjectLaunchPositionLine()
                {
                    super(AreaPane.this.editor);
                    setBackingPoints(new BackingObjectPointProxy(), new RegionEditorObjectPolygonCenterProxy());
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
                protected Collection<Rectangle2D> getCanvasExclusionRects()
                {
                    exclusionRectangles.clear();
                    exclusionRectangles.add(editor.cornersToAbsoluteCanvasRectangle(
                        RegionEditorObjectLaunchPosition.this.getCorners())
                    );
                    exclusionRectangles.add(editor.cornersToAbsoluteCanvasRectangle(
                        regionEditorObjectPolygon.getCorners())
                    );
                    return exclusionRectangles;
                }

                @Override
                protected Color getLineColor()
                {
                    return Color.MAGENTA;
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

            final Area area = new Area(source);
            area.load(getTracker());
            AreaPane.this.area = area;

            final WED.Graphics wedGraphics = area.newGraphics().getWedGraphics();
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
