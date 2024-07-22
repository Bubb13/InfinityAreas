
package com.github.bubb13.infinityareas.gui.pane;

import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.game.resource.WED;
import com.github.bubb13.infinityareas.gui.control.UnderlinedButton;
import com.github.bubb13.infinityareas.gui.dialog.ErrorAlert;
import com.github.bubb13.infinityareas.gui.dialog.WarningAlertTwoOptions;
import com.github.bubb13.infinityareas.gui.stage.ReplaceOverlayTilesetStage;
import com.github.bubb13.infinityareas.misc.AbstractRenderable;
import com.github.bubb13.infinityareas.misc.Corners;
import com.github.bubb13.infinityareas.misc.InstanceHashMap;
import com.github.bubb13.infinityareas.misc.LoadingStageTracker;
import com.github.bubb13.infinityareas.misc.OrderedInstanceSet;
import com.github.bubb13.infinityareas.misc.QuadTree;
import com.github.bubb13.infinityareas.misc.Renderable;
import com.github.bubb13.infinityareas.misc.SimpleLinkedList;
import com.github.bubb13.infinityareas.misc.TaskTrackerI;
import com.github.bubb13.infinityareas.misc.TrackedTask;
import com.github.bubb13.infinityareas.util.ImageUtil;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class WEDPane extends StackPane
{
    ////////////////////
    // Private Fields //
    ////////////////////

    // Data
    private WED wed;
    private WED.Graphics wedGraphics;
    private QuadTree<Renderable> quadTree;

    // GUI
    private final ZoomPane zoomPane = new ZoomPane();
    private final GraphicsContext canvasGraphics = zoomPane.getGraphics();
    private final CheckBox renderPolygonsCheckbox = new CheckBox("Render Polygons");

    private RenderablePolygon drawingPolygon = null;
    private EditMode editMode = EditMode.NORMAL;
    private DragMode dragMode = DragMode.NONE;
    private SelectMode selectMode = SelectMode.NONE;

    private MouseButton pressButton = null;
    private MouseButton dragButton;

    private Renderable pressObject = null;
    private Renderable dragObject = null;

    final private OrderedInstanceSet<Renderable> selectedObjects = new OrderedInstanceSet<>();

    private QuickSelectRectangle quickSelectRectangle;
    private boolean quickSelectMode = false;
    private boolean quickSelectRender = false;

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
        drawingPolygon = null;
        editMode = EditMode.NORMAL;
        dragMode = DragMode.NONE;
        selectMode = SelectMode.NONE;

        pressButton = null;
        dragButton = null;

        pressObject = null;
        dragObject = null;

        selectedObjects.clear();

        quickSelectRectangle = new QuickSelectRectangle();
        quickSelectMode = false;
        quickSelectRender = false;

        for (final WED.Polygon polygon : wed.getPolygons())
        {
            new RenderablePolygon(polygon);
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
                drawPolygonButton.setOnAction((ignored) -> this.onDrawPolygon());

                final Region padding2 = new Region();
                padding2.setPadding(new Insets(0, 0, 0, 5));

                final Button bisectLine = new UnderlinedButton("Bisect Line");
                bisectLine.setOnAction((ignored) -> this.onBisectLine());

                final Region padding3 = new Region();
                padding3.setPadding(new Insets(0, 0, 0, 5));

                final Button quickSelect = new UnderlinedButton("Quick Select");
                quickSelect.setOnAction((ignored) -> this.onQuickSelect());

                final Region padding4 = new Region();
                padding4.setPadding(new Insets(0, 0, 0, 5));

                final MenuButton overlaysDropdown = new MenuButton("Overlays");
                final MenuItem replaceOverlayTisButton = new MenuItem("Replace Overlay Tileset");
                replaceOverlayTisButton.setOnAction((ignored) -> this.onSelectReplaceOverlayTileset());
                overlaysDropdown.getItems().addAll(replaceOverlayTisButton);

                toolbar.getChildren().addAll(saveButton, padding1, drawPolygonButton,
                    padding2, bisectLine, padding3, quickSelect, padding4, overlaysDropdown);

            zoomPane.setZoomFactorListener(this::onZoomFactorChanged);
            zoomPane.setDrawCallback(this::onDraw);
            zoomPane.setMouseDraggedListener(this::onMouseDragged);
            zoomPane.setMousePressedListener(this::onMousePressed);
            zoomPane.setMouseReleasedListener(this::onMouseReleased);
            //zoomPane.setDragDetectedListener(this::onDragDetected);
            this.setOnKeyPressed(this::onKeyPressed);
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

    private void onDrawPolygon()
    {
        setEditMode(EditMode.DRAW_POLYGON);
    }

    private void onBisectLine()
    {
        if (selectedObjects.size() != 2)
        {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        final RenderablePolygonVertex[] selectedVertices = new RenderablePolygonVertex[2];
        int i = 0;

        for (final Renderable renderable : selectedObjects)
        {
            if (!(renderable instanceof RenderablePolygonVertex renderablePolygonVertex))
            {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            selectedVertices[i++] = renderablePolygonVertex;
        }

        final RenderablePolygonVertex renderableVertex1;
        final RenderablePolygonVertex renderableVertex2;

        if (selectedVertices[0].next() == selectedVertices[1])
        {
            renderableVertex1 = selectedVertices[0];
            renderableVertex2 = selectedVertices[1];
        }
        else if (selectedVertices[1].next() == selectedVertices[0])
        {
            renderableVertex1 = selectedVertices[1];
            renderableVertex2 = selectedVertices[0];
        }
        else
        {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        final WED.Vertex vertex1 = renderableVertex1.vertex;
        final WED.Vertex vertex2 = renderableVertex2.vertex;

        final short finalX = (short)(vertex2.x() - (vertex2.x() - vertex1.x()) / 2);
        final short finalY = (short)(vertex2.y() - (vertex2.y() - vertex1.y()) / 2);

        renderableVertex1.addNewVertexAfter(finalX, finalY);
        unselectAll();
        zoomPane.requestDraw();
    }

    private void onQuickSelect()
    {
        quickSelectMode = true;
        zoomPane.requestDraw();
    }

    private void setEditMode(final EditMode newMode)
    {
        editMode = newMode;
        zoomPane.requestDraw();
    }

    private void onDraw(final GraphicsContext context)
    {
        final Corners visibleSourceCorners = zoomPane.getVisibleSourceCorners();
        quadTree.iterateNear(visibleSourceCorners, (renderable) ->
        {
            if (!renderable.isEnabled() || renderable.getCorners().intersect(visibleSourceCorners) == null)
            {
                return;
            }
            renderable.render();
        });

        if (quickSelectMode)
        {
            drawModeText(context, "Quick Select Mode");
            return;
        }

        switch (editMode)
        {
            case DRAW_POLYGON -> drawModeText(context, "Draw Polygon Mode");
        }
    }

    private void drawModeText(final GraphicsContext context, final String text)
    {
        final Font font = new Font("Arial", 28);

        final Text tempText = new Text(text);
        tempText.setFont(font);
        final double textHeight = tempText.getBoundsInLocal().getHeight();

        context.setFill(Color.WHITE);
        context.setFont(font);
        context.setStroke(Color.BLACK);
        context.setLineWidth(1);

        context.fillText(text, 10, textHeight);
        context.strokeText(text, 10, textHeight);
    }

    private void onMousePressed(final MouseEvent event)
    {
        final int absoluteCanvasX = (int)event.getX();
        final int absoluteCanvasY = (int)event.getY();

        final Point sourcePressPos = zoomPane.absoluteCanvasToSourcePosition(absoluteCanvasX, absoluteCanvasY);
        final short sourcePressX = (short)sourcePressPos.x;
        final short sourcePressY = (short)sourcePressPos.y;

        if (quickSelectMode)
        {
            if (!quickSelectRender && event.getButton() == MouseButton.PRIMARY)
            {
                pressButton = event.getButton();
                quickSelectRectangle.setOrigin(sourcePressX, sourcePressY);
                quickSelectRectangle.setBoundingCorner(sourcePressX, sourcePressY);
                quickSelectRender = true;
                zoomPane.requestDraw();
            }
            return;
        }

        final int fudgeAmount = (int)(5 / zoomPane.getZoomFactor());
        final Corners fudgeCorners = new Corners(
            sourcePressX - fudgeAmount, sourcePressY - fudgeAmount,
            sourcePressX + fudgeAmount + 1, sourcePressY + fudgeAmount + 1
        );

        boolean pressedSomething = false;

        for (final Renderable renderable : quadTree.iterableNear(fudgeCorners))
        {
            if (!renderable.isEnabled() || !renderable.getCorners().contains(sourcePressPos, fudgeAmount))
            {
                continue;
            }

            if (renderable instanceof RenderablePolygonVertex)
            {
                if (event.getButton() != MouseButton.PRIMARY) break;
                pressObject = renderable;
                pressButton = event.getButton();
                pressedSomething = true;
                break;
            }
        }

        if (pressedSomething)
        {
            return;
        }

        switch (editMode)
        {
            case DRAW_POLYGON:
            {
                drawPolygonModeBackgroundPressed(event, sourcePressX, sourcePressY);
            }
        }
    }

    private void drawPolygonModeBackgroundPressed(
        final MouseEvent event, final short sourcePressX, final short sourcePressY)
    {
        if (event.getButton() != MouseButton.PRIMARY) return;

        if (drawingPolygon == null)
        {
            final WED.Polygon polygon = new WED.Polygon(
                (byte)0, (byte)0,
                sourcePressX, (short)(sourcePressX + 1),
                sourcePressY, (short)(sourcePressY + 1),
                new SimpleLinkedList<>()
            );

            drawingPolygon = new RenderablePolygon(polygon, false);
        }

        drawingPolygon.addNewVertex(sourcePressX, sourcePressY);
        zoomPane.requestDraw();
    }

    final void onMouseDragged(final MouseEvent event)
    {
        final MouseButton button = event.getButton();

        if (quickSelectMode)
        {
            if (button == pressButton)
            {
                event.consume();
                final int absoluteCanvasX = (int)event.getX();
                final int absoluteCanvasY = (int)event.getY();
                final Point sourcePos = zoomPane.absoluteCanvasToSourcePosition(absoluteCanvasX, absoluteCanvasY);
                quickSelectRectangle.setBoundingCorner((short)sourcePos.x, (short)sourcePos.y);
            }
            return;
        }

        if (editMode == EditMode.DRAW_POLYGON && button == MouseButton.PRIMARY && pressObject == null)
        {
            pressButton = button;
            pressObject = drawingPolygon.getRenderablePolygonVertices().getLast();
        }

        if (button != pressButton)
        {
            return;
        }

        if (dragMode == DragMode.NONE)
        {
            if (pressObject instanceof RenderablePolygonVertex)
            {
                dragMode = DragMode.VERTEX_HANDLE;
                dragObject = pressObject;
                dragButton = button;
            }
        }

        if (dragMode == DragMode.NONE)
        {
            return;
        }

        final int absoluteCanvasX = (int)event.getX();
        final int absoluteCanvasY = (int)event.getY();

        switch (dragMode)
        {
            case VERTEX_HANDLE ->
            {
                boolean wasNotSelected = false;
                if (!selectedObjects.contains(dragObject))
                {
                    wasNotSelected = true;
                    select(dragObject);
                }

                final WED.Vertex dragVertex = ((RenderablePolygonVertex)dragObject).vertex;

                final Point newSourcePos = zoomPane.absoluteCanvasToSourcePosition(absoluteCanvasX, absoluteCanvasY);
                final short deltaX = (short)(newSourcePos.x - dragVertex.x());
                final short deltaY = (short)(newSourcePos.y - dragVertex.y());

                for (final Renderable renderable : selectedObjects)
                {
                    if (!(renderable instanceof RenderablePolygonVertex movingRenderableVertex))
                    {
                        continue;
                    }

                    final WED.Vertex movingVertex = movingRenderableVertex.vertex;
                    final short newX = (short)(movingVertex.x() + deltaX);
                    final short newY = (short)(movingVertex.y() + deltaY);

                    movingRenderableVertex.move(newX, newY);
                    zoomPane.requestDraw();
                }

                if (wasNotSelected)
                {
                    unselect(dragObject);
                }
            }
        }

        event.consume();
    }

    private void onMouseReleased(final MouseEvent event)
    {
        final MouseButton button = event.getButton();

        if (quickSelectRender)
        {
            if (button == pressButton)
            {
                if (!event.isShiftDown() && !event.isControlDown())
                {
                    unselectAll();
                }

                final Corners selectionCorners = quickSelectRectangle.getCorners();
                for (final Renderable renderable : quadTree.iterableNear(selectionCorners))
                {
                    if (!renderable.isEnabled() || renderable.getCorners().intersect(selectionCorners) == null)
                    {
                        continue;
                    }

                    if (renderable instanceof RenderablePolygonVertex renderablePolygonVertex)
                    {
                        if (event.isControlDown())
                        {
                            if (renderablePolygonVertex.selected)
                            {
                                unselect(renderablePolygonVertex);
                            }
                            else
                            {
                                select(renderablePolygonVertex);
                            }
                        }
                        else
                        {
                            select(renderablePolygonVertex);
                        }
                    }
                }

                pressButton = null;
                quickSelectRender = false;
                quickSelectMode = false;
                zoomPane.requestDraw();
            }
            return;
        }

        if (button == pressButton)
        {
            if (dragMode == DragMode.NONE)
            {
                final Point sourcePoint = zoomPane.absoluteCanvasToSourcePosition((int)event.getX(), (int)event.getY());
                if (pressObject.getCorners().contains(sourcePoint))
                {
                    pressObject.clicked(event);
                }
            }
            pressButton = null;
            pressObject = null;
        }

        if (button == dragButton)
        {
            dragMode = DragMode.NONE;
            dragObject = null;
        }
    }

    private void onKeyPressed(final KeyEvent event)
    {
        final KeyCode key = event.getCode();

        switch (key)
        {
            case ESCAPE ->
            {
                if (selectedObjects.size() > 0)
                {
                    event.consume();
                    unselectAll();
                }
                else if (editMode == EditMode.DRAW_POLYGON)
                {
                    event.consume();
                    cancelDrawPolygonMode();
                }
            }
            case ENTER, SPACE ->
            {
                if (editMode == EditMode.DRAW_POLYGON)
                {
                    event.consume();
                    endDrawPolygonMode();
                }
            }
            case B ->
            {
                event.consume();
                onBisectLine();
            }
            case D ->
            {
                if (editMode == EditMode.NORMAL)
                {
                    event.consume();
                    setEditMode(EditMode.DRAW_POLYGON);
                }
            }
            case Q ->
            {
                event.consume();
                onQuickSelect();
            }
            case DELETE ->
            {
                event.consume();
                onDelete();
            }
        }
    }

    private void onDelete()
    {
        final InstanceHashMap<RenderablePolygon, VerticesDeleteInfo> verticesDeleteInfoMap = new InstanceHashMap<>();
        final ArrayList<Renderable> otherSelectedObjects = new ArrayList<>();

        for (final Renderable renderable : selectedObjects)
        {
            if (renderable instanceof RenderablePolygonVertex vertex)
            {
                final VerticesDeleteInfo deleteInfo = verticesDeleteInfoMap.computeIfAbsent(
                    vertex.getRenderablePolygon(), (ignored) -> new VerticesDeleteInfo());

                deleteInfo.toDelete.add(vertex);
            }
            else
            {
                otherSelectedObjects.add(renderable);
            }
        }

        final boolean[] noContinue = new boolean[] { false };

        for (final var entry : verticesDeleteInfoMap.entries())
        {
            final RenderablePolygon polygon = entry.getKey();
            final VerticesDeleteInfo deleteInfo = entry.getValue();
            final int numVertices = polygon.getRenderablePolygonVertices().size();
            final int numVerticesToDelete = deleteInfo.toDelete.size();
            final int remainingVertices = numVertices - numVerticesToDelete;

            if (remainingVertices > 0 && remainingVertices < 3)
            {
                noContinue[0] = true;

                WarningAlertTwoOptions.openAndWait(
                    "The delete operation will result in polygon(s) with less than 3 vertices. These " +
                        "polygons will be deleted.\n\n" +
                        "Do you still wish to perform the delete operation?",
                    "Yes", () -> noContinue[0] = false,
                    "Cancel", null);

                if (noContinue[0])
                {
                    return;
                }
                break;
            }
        }

        for (final var entry : verticesDeleteInfoMap.entries())
        {
            final RenderablePolygon polygon = entry.getKey();
            final VerticesDeleteInfo deleteInfo = entry.getValue();
            final int numVertices = polygon.getRenderablePolygonVertices().size();
            final int numVerticesToDelete = deleteInfo.toDelete.size();
            final int remainingVertices = numVertices - numVerticesToDelete;

            if (remainingVertices < 3)
            {
                polygon.delete();
            }
            else
            {
                for (final RenderablePolygonVertex vertex : deleteInfo.toDelete)
                {
                    vertex.delete();
                }
            }
        }

        for (final Renderable renderable : otherSelectedObjects)
        {
            renderable.delete();
        }
    }

    private void cancelDrawPolygonMode()
    {
        cleanUpPolygonDrawingModeRenderObjects();
        drawingPolygon = null;
        setEditMode(EditMode.NORMAL);
        zoomPane.requestDraw();
    }

    private void endDrawPolygonMode()
    {
        boolean endMode = true;

        if (drawingPolygon != null)
        {
            final WED.Polygon polygon = drawingPolygon.getPolygon();

            // Detect invalid polygon and warn that it will be deleted
            if (polygon.getVertices().size() < 3)
            {
                final boolean[] deletePolygon = new boolean[] { false };
                WarningAlertTwoOptions.openAndWait(
                    "The drawn polygon has less than 3 vertices.\n\n" +
                        "Ending polygon drawing mode will delete the drawn vertices.\n\n" +
                        "Do you still wish to end polygon drawing mode?",
                    "Yes", () -> deletePolygon[0] = true,
                    "Cancel", null);

                // Delete the drawn polygon if prompted to
                if (deletePolygon[0])
                {
                    cleanUpPolygonDrawingModeRenderObjects();
                    drawingPolygon = null;
                }
                else
                {
                    endMode = false;
                }
            }
            else
            {
                drawingPolygon.setRenderImpliedFinalLine(true);
                drawingPolygon = null;
                wed.addPolygon(polygon);
            }
        }

        if (endMode)
        {
            setEditMode(EditMode.NORMAL);
        }
    }

    private void cleanUpPolygonDrawingModeRenderObjects()
    {
        if (drawingPolygon == null) return;

        // Remove all renderable objects from the quadtree
        for (final RenderablePolygonVertex vertex : drawingPolygon.getRenderablePolygonVertices())
        {
            removeRenderable(vertex);
        }
        removeRenderable(drawingPolygon);
    }

    private void addRenderable(final Renderable renderable)
    {
        quadTree.add(renderable, renderable.getCorners());
    }

    private void removeRenderable(final Renderable renderable)
    {
        selectedObjects.remove(renderable);
        quadTree.remove(renderable);
    }

    private void select(final Renderable renderable)
    {
        selectedObjects.addTail(renderable);
        renderable.selected();
    }

    private void unselectAll()
    {
        for (final Renderable renderable : selectedObjects)
        {
            renderable.unselected();
        }
        selectedObjects.clear();
    }

    private void unselect(final Renderable renderable)
    {
        renderable.unselected();
        selectedObjects.remove(renderable);
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

            wedGraphics = wed.newGraphics();
            wedGraphics.renderOverlays(getTracker(), 0, 1, 2, 3, 4);
            final BufferedImage image = ImageUtil.copyArgb(wedGraphics.getImage());

            quadTree = new QuadTree<>(
                0, 0,
                image.getWidth(), image.getHeight(),
                10);

            reset();

            waitForFxThreadToExecute(() -> zoomPane.setImage(image));
            return null;
        }
    }

    private class RenderablePolygon extends AbstractRenderable
    {
        private final WED.Polygon polygon;
        private final SimpleLinkedList<RenderablePolygonVertex> renderableVertices = new SimpleLinkedList<>();
        private final Corners corners = new Corners();
        private boolean renderImpliedFinalLine;

        public RenderablePolygon(final WED.Polygon polygon, final boolean renderImpliedFinalLine)
        {
            this.polygon = polygon;

            for (final WED.Vertex vertex : polygon.getVertices())
            {
                addNewRenderableVertex(vertex);
            }

            this.renderImpliedFinalLine = renderImpliedFinalLine;
            calculateCorners();
            addRenderable(this);
        }

        public RenderablePolygon(final WED.Polygon polygon)
        {
            this(polygon, true);
        }

        public RenderablePolygonVertex addNewVertex(final short x, final short y)
        {
            return addNewRenderableVertex(polygon.addVertex(x, y));
        }

        public void recalculateCorners()
        {
            calculateCorners();
            addRenderable(this);
        }

        public WED.Polygon getPolygon()
        {
            return polygon;
        }

        public void setRenderImpliedFinalLine(boolean renderImpliedFinalLine)
        {
            this.renderImpliedFinalLine = renderImpliedFinalLine;
        }

        public SimpleLinkedList<RenderablePolygonVertex> getRenderablePolygonVertices()
        {
            return renderableVertices;
        }

        @Override
        public boolean isEnabled()
        {
            return editMode == EditMode.DRAW_POLYGON || renderPolygonsCheckbox.isSelected();
        }

        @Override
        public void render()
        {
            final SimpleLinkedList<WED.Vertex> vertices = polygon.getVertices();
            final int limit = vertices.size() - 1;

            canvasGraphics.setLineWidth(1D);
            canvasGraphics.setStroke(Color.WHITE);

            var curNode = vertices.getFirstNode();
            for (int i = 0; i < limit; ++i)
            {
                final var nextNode = curNode.next();
                renderLine(curNode.value(), nextNode.value());
                curNode = nextNode;
            }

            if (renderImpliedFinalLine)
            {
                final WED.Vertex vFirst = vertices.getFirst();
                final WED.Vertex vLast = vertices.getLast();
                renderLine(vFirst, vLast);
            }
        }

        @Override
        public Corners getCorners()
        {
            return corners;
        }

        @Override
        public void delete()
        {
            for (final RenderablePolygonVertex renderable : renderableVertices)
            {
                removeRenderable(renderable);
            }
            removeRenderable(this);
            polygon.delete();
            zoomPane.requestDraw();
        }

        /////////////////////
        // Private Methods //
        /////////////////////

        private RenderablePolygonVertex addNewRenderableVertex(final WED.Vertex vertex)
        {
            final RenderablePolygonVertex newRenderableVertex = renderableVertices.addTail((node)
                -> new RenderablePolygonVertex(this, vertex, node)).value();

            newRenderableVertex.recalculatePolygonCorners();
            return newRenderableVertex;
        }

        private void calculateCorners()
        {
            corners.setTopLeftX(polygon.getBoundingBoxLeft());
            corners.setTopLeftY(polygon.getBoundingBoxTop());
            corners.setBottomRightExclusiveX(polygon.getBoundingBoxRight());
            corners.setBottomRightExclusiveY(polygon.getBoundingBoxBottom());
        }

        private void renderLine(final WED.Vertex v1, final WED.Vertex v2)
        {
            final Point2D p1 = zoomPane.sourceToAbsoluteCanvasPosition(v1.x(), v1.y());
            final Point2D p2 = zoomPane.sourceToAbsoluteCanvasPosition(v2.x(), v2.y());
            canvasGraphics.strokeLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
        }
    }

    private class RenderablePolygonVertex extends AbstractRenderable
    {
        private final RenderablePolygon renderablePolygon;
        private final WED.Vertex vertex;
        private final SimpleLinkedList<RenderablePolygonVertex>.Node renderableVertexNode;
        private final Corners corners = new Corners();

        private boolean selected = false;

        public RenderablePolygonVertex(
            final RenderablePolygon renderablePolygon,
            final WED.Vertex vertex,
            final SimpleLinkedList<RenderablePolygonVertex>.Node renderableVertexNode)
        {
            this.renderablePolygon = renderablePolygon;
            this.vertex = vertex;
            this.renderableVertexNode = renderableVertexNode;
            calculateCorners();
            addRenderable(this);
        }

        public void move(final short x, final short y)
        {
            vertex.setX(x);
            vertex.setY(y);
            calculateCorners();
            addRenderable(this);
            recalculatePolygonCorners();
        }

        @Override
        public boolean isEnabled()
        {
            return editMode == EditMode.DRAW_POLYGON || renderPolygonsCheckbox.isSelected();
        }

        @Override
        public void render()
        {
            Color color = selected ? Color.BLUE : Color.TEAL;
            if (editMode == EditMode.DRAW_POLYGON && renderablePolygon == drawingPolygon
                && renderableVertexNode.next() == null)
            {
                color = selected
                    ? Color.rgb(0, 255, 0)
                    : Color.rgb(0, 127, 0);
            }

            canvasGraphics.setStroke(color);
            final Point2D p1 = zoomPane.sourceToAbsoluteCanvasPosition(vertex.x() - 1, vertex.y() - 1);
            final Point2D p2 = zoomPane.sourceToAbsoluteCanvasPosition(vertex.x() + 1, vertex.y() + 1);
            canvasGraphics.strokeRect(p1.getX(), p1.getY(), p2.getX() - p1.getX(), p2.getY() - p1.getY());
        }

        @Override
        public Corners getCorners()
        {
            return corners;
        }

        public void recalculatePolygonCorners()
        {
            final short x = vertex.x();
            final short y = vertex.y();

            final WED.Polygon polygon = renderablePolygon.getPolygon();
            if (x < polygon.getBoundingBoxLeft()) polygon.setBoundingBoxLeft(x);
            if (y < polygon.getBoundingBoxTop()) polygon.setBoundingBoxTop(y);
            if (x > polygon.getBoundingBoxRight()) polygon.setBoundingBoxRight(x);
            if (y > polygon.getBoundingBoxBottom()) polygon.setBoundingBoxBottom(y);

            renderablePolygon.recalculateCorners();
        }

        @Override
        public void clicked(final MouseEvent event)
        {
            if (!event.isShiftDown() && !event.isControlDown())
            {
                unselectAll();
            }

            if (event.isControlDown())
            {
                if (selected)
                {
                    unselect(this);
                }
                else
                {
                    select(this);
                }
            }
            else
            {
                select(this);
            }
        }

        @Override
        public void selected()
        {
            selected = true;
            zoomPane.requestDraw();
        }

        @Override
        public void unselected()
        {
            selected = false;
            zoomPane.requestDraw();
        }

        @Override
        public void delete()
        {
            removeRenderable(this);
            renderableVertexNode.remove();
            vertex.getNode().remove();
            zoomPane.requestDraw();
        }

        public RenderablePolygonVertex addNewVertexAfter(final short x, final short y)
        {
            final WED.Vertex addedVertex = vertex.getNode().addAfter(
                (node)-> new WED.Vertex(node, x, y)).value();

            final RenderablePolygonVertex addedRenderableVertex = renderableVertexNode.addAfter(
                (node) -> new RenderablePolygonVertex(renderablePolygon, addedVertex, node)).value();

            addedRenderableVertex.recalculatePolygonCorners();
            return addedRenderableVertex;
        }

        public RenderablePolygonVertex previous()
        {
            final var previousNode = renderableVertexNode.previous();
            return previousNode == null ? renderablePolygon.renderableVertices.getLast() : previousNode.value();
        }

        public RenderablePolygonVertex next()
        {
            final var nextNode = renderableVertexNode.next();
            return nextNode == null ? renderablePolygon.renderableVertices.getFirst() : nextNode.value();
        }

        public RenderablePolygon getRenderablePolygon()
        {
            return renderablePolygon;
        }

        /////////////////////
        // Private Methods //
        /////////////////////

        private void calculateCorners()
        {
            final short x = vertex.x();
            final short y = vertex.y();
            corners.setTopLeftX(x - 1);
            corners.setTopLeftY(y - 1);
            corners.setBottomRightExclusiveX(x + 1);
            corners.setBottomRightExclusiveY(y + 1);
        }
    }

    private class QuickSelectRectangle extends AbstractRenderable
    {
        private final Corners corners = new Corners();
        private short originX;
        private short originY;

        public QuickSelectRectangle()
        {
            addRenderable(this);
        }

        @Override
        public boolean isEnabled()
        {
            return quickSelectRender;
        }

        @Override
        public void render()
        {
            canvasGraphics.setLineWidth(1D);
            canvasGraphics.setStroke(Color.rgb(0, 255, 0));

            final Point2D absoluteTopLeft = zoomPane.sourceToAbsoluteCanvasPosition(
                corners.topLeftX(), corners.topLeftY());

            final Point2D absoluteBottomRightExclusive = zoomPane.sourceToAbsoluteCanvasPosition(
                corners.bottomRightExclusiveX(), corners.bottomRightExclusiveY());

            canvasGraphics.strokeRect(absoluteTopLeft.getX(), absoluteTopLeft.getY(),
                absoluteBottomRightExclusive.getX() - absoluteTopLeft.getX(),
                absoluteBottomRightExclusive.getY() - absoluteTopLeft.getY());
        }

        @Override
        public Corners getCorners()
        {
            return corners;
        }

        public void setOrigin(final short x, final short y)
        {
            originX = x;
            originY = y;
        }

        public void setBoundingCorner(final short x, final short y)
        {
            if (x < originX)
            {
                corners.setTopLeftX(x);
                corners.setBottomRightExclusiveX(originX + 1);
            }
            else
            {
                corners.setTopLeftX(originX);
                corners.setBottomRightExclusiveX(x + 1);
            }

            if (y < originY)
            {
                corners.setTopLeftY(y);
                corners.setBottomRightExclusiveY(originY + 1);
            }
            else
            {
                corners.setTopLeftY(originY);
                corners.setBottomRightExclusiveY(y + 1);
            }

            addRenderable(this);
            zoomPane.requestDraw();
        }
    }

    private enum EditMode
    {
        NORMAL, DRAW_POLYGON
    }

    private enum DragMode
    {
        NONE, VERTEX_HANDLE
    }

    private enum SelectMode
    {
        NONE, ONLY_VERTICES
    }

    private static class VerticesDeleteInfo
    {
        public final ArrayList<RenderablePolygonVertex> toDelete = new ArrayList<>();
    }
}
