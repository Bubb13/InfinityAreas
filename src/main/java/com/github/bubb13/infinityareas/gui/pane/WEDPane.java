
package com.github.bubb13.infinityareas.gui.pane;

import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.game.resource.WED;
import com.github.bubb13.infinityareas.gui.dialog.ErrorAlert;
import com.github.bubb13.infinityareas.gui.stage.ReplaceOverlayTilesetStage;
import com.github.bubb13.infinityareas.misc.Corners;
import com.github.bubb13.infinityareas.misc.LoadingStageTracker;
import com.github.bubb13.infinityareas.misc.QuadTree;
import com.github.bubb13.infinityareas.misc.Renderable;
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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.awt.Point;
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

    private DragMode dragMode = DragMode.NONE;
    private Object dragObject = null;

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
            zoomPane.setDrawCallback(this::onDraw);
            zoomPane.setMouseDraggedListener(this::onMouseDragged);
            zoomPane.setMousePressedListener(this::onMousePressed);
            zoomPane.setMouseReleasedListener(this::onMouseReleased);
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
    }

    final void onMouseDragged(final MouseEvent event)
    {
        if (event.isMiddleButtonDown() || dragMode == DragMode.NONE)
        {
            return;
        }

        final int absoluteCanvasX = (int)event.getX();
        final int absoluteCanvasY = (int)event.getY();

        switch (dragMode)
        {
            case VERTEX_HANDLE ->
            {
                final RenderablePolygonVertexHandle vertexHandle = (RenderablePolygonVertexHandle)dragObject;
                final Point sourcePos = zoomPane.absoluteCanvasToSourcePosition(absoluteCanvasX, absoluteCanvasY);
                vertexHandle.move((short)sourcePos.x, (short)sourcePos.y);
                zoomPane.requestDraw();
            }
        }

        event.consume();
    }

    final void onMousePressed(final MouseEvent event)
    {
        final int absoluteCanvasX = (int)event.getX();
        final int absoluteCanvasY = (int)event.getY();

        final Corners visibleSourceCorners = zoomPane.getVisibleSourceCorners();
        final Point sourcePressPos = zoomPane.absoluteCanvasToSourcePosition(absoluteCanvasX, absoluteCanvasY);

        final var iterateResult = quadTree.iterableNear(visibleSourceCorners);

        for (final Renderable renderable : iterateResult)
        {
            if (!renderable.isEnabled() || !renderable.getCorners().contains(sourcePressPos))
            {
                continue;
            }

            if (renderable instanceof RenderablePolygonVertexHandle vertexHandle)
            {
                dragMode = DragMode.VERTEX_HANDLE;
                dragObject = vertexHandle;
                break;
            }
        }
    }

    private void onMouseReleased(final MouseEvent event)
    {
        dragMode = DragMode.NONE;
        dragObject = null;
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

            for (final WED.Polygon polygon : wed.getPolygons())
            {
                final RenderablePolygon renderablePolygon = new RenderablePolygon(polygon);
                addRenderable(renderablePolygon);

                for (final WED.Vertex vertex : polygon.getVertices())
                {
                    addRenderable(new RenderablePolygonVertexHandle(renderablePolygon, vertex));
                }
            }

            waitForFxThreadToExecute(() -> zoomPane.setImage(image));
            return null;
        }
    }

    private void addRenderable(final Renderable renderable)
    {
        quadTree.add(renderable, renderable.getCorners());
    }

    private class RenderablePolygon implements Renderable
    {
        private final WED.Polygon polygon;
        private final Corners corners = new Corners();

        public RenderablePolygon(final WED.Polygon polygon)
        {
            this.polygon = polygon;
            calculateCorners();
        }

        public void recalculateCorners()
        {
            calculateCorners();
            quadTree.add(this, corners);
        }

        public WED.Polygon getPolygon()
        {
            return polygon;
        }

        @Override
        public boolean isEnabled()
        {
            return renderPolygonsCheckbox.isSelected();
        }

        @Override
        public void render()
        {
            final ArrayList<WED.Vertex> vertices = polygon.getVertices();
            final int limit = vertices.size() - 1;

            canvasGraphics.setLineWidth(1D);
            canvasGraphics.setStroke(Color.WHITE);

            for (int i = 0; i < limit; ++i)
            {
                final WED.Vertex v1 = vertices.get(i);
                final WED.Vertex v2 = vertices.get(i + 1);
                renderLine(v1, v2);
            }

            final WED.Vertex vFirst = vertices.get(0);
            final WED.Vertex vLast = vertices.get(limit);
            renderLine(vFirst, vLast);
        }

        @Override
        public Corners getCorners()
        {
            return corners;
        }

        /////////////////////
        // Private Methods //
        /////////////////////

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

    private class RenderablePolygonVertexHandle implements Renderable
    {
        private final RenderablePolygon renderablePolygon;
        private final WED.Vertex vertex;
        private final Corners corners = new Corners();

        public RenderablePolygonVertexHandle(final RenderablePolygon renderablePolygon, final WED.Vertex vertex)
        {
            this.renderablePolygon = renderablePolygon;
            this.vertex = vertex;
            calculateCorners();
        }

        public void move(final short x, final short y)
        {
            vertex.setX(x);
            vertex.setY(y);
            calculateCorners();
            quadTree.add(this, corners);

            final WED.Polygon polygon = renderablePolygon.getPolygon();
            if (x < polygon.getBoundingBoxLeft()) polygon.setBoundingBoxLeft(x);
            if (y < polygon.getBoundingBoxTop()) polygon.setBoundingBoxTop(y);
            if (x > polygon.getBoundingBoxRight()) polygon.setBoundingBoxRight(x);
            if (y > polygon.getBoundingBoxBottom()) polygon.setBoundingBoxBottom(y);

            renderablePolygon.recalculateCorners();
        }

        @Override
        public boolean isEnabled()
        {
            return renderPolygonsCheckbox.isSelected();
        }

        @Override
        public void render()
        {
            canvasGraphics.setStroke(Color.TEAL);
            final Point2D p1 = zoomPane.sourceToAbsoluteCanvasPosition(vertex.x() - 1, vertex.y() - 1);
            final Point2D p2 = zoomPane.sourceToAbsoluteCanvasPosition(vertex.x() + 1, vertex.y() + 1);
            canvasGraphics.strokeRect(p1.getX(), p1.getY(), p2.getX() - p1.getX(), p2.getY() - p1.getY());
        }

        @Override
        public Corners getCorners()
        {
            return corners;
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

    private enum DragMode
    {
        NONE, VERTEX_HANDLE
    }
}
