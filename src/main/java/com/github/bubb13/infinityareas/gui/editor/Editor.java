
package com.github.bubb13.infinityareas.gui.editor;

import com.github.bubb13.infinityareas.gui.editor.editmode.EditMode;
import com.github.bubb13.infinityareas.gui.editor.renderable.AbstractRenderable;
import com.github.bubb13.infinityareas.gui.pane.ZoomPane;
import com.github.bubb13.infinityareas.misc.CanvasCache;
import com.github.bubb13.infinityareas.misc.DoubleCorners;
import com.github.bubb13.infinityareas.misc.DoubleQuadTree;
import com.github.bubb13.infinityareas.misc.OrderedInstanceSet;
import com.github.bubb13.infinityareas.misc.TrackingOrderedInstanceSet;
import com.github.bubb13.infinityareas.util.MiscUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.function.Supplier;

public class Editor
{
    //////////////////////////
    // Public Static Fields //
    //////////////////////////

    public static final int MAX_SNAPSHOT_SIDE = 100;

    ////////////////////
    // Private Fields //
    ////////////////////

    private final ZoomPane zoomPane;
    private final HashMap<Class<? extends EditMode>, EditMode> cachedEditModes = new HashMap<>();
    private final Stack<EditMode> previousEditModesStack = new Stack<>();

    private final TrackingOrderedInstanceSet<AbstractRenderable> selectedObjects = new TrackingOrderedInstanceSet<>()
    {
        @Override
        public void referencedObjectDeleted(final AbstractRenderable reference)
        {
            super.referencedObjectDeleted(reference);
            reference.onUnselected();
        }
    };

    private final OrderedInstanceSet<AbstractRenderable> zoomFactorListenerObjects = new OrderedInstanceSet<>();

    private DoubleQuadTree<AbstractRenderable> quadTree = null;
    private Comparator<AbstractRenderable> renderingComparator;
    private Comparator<AbstractRenderable> interactionComparator;

    private EditMode editMode = null;

    private MouseButton pressButton = null;
    private AbstractRenderable pressObject = null;
    private boolean dragOccurred = false;
    private AbstractRenderable dragObject = null;

    private final ArrayList<Snapshot> snapshots = new ArrayList<>();
    private final ObservableList<Snapshot> observableSnapshots = FXCollections.observableList(snapshots);
    private final CanvasCache snapshotCanvasCache = new CanvasCache(MAX_SNAPSHOT_SIDE, MAX_SNAPSHOT_SIDE);

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public Editor(final ZoomPane zoomPane, final Node keyPressedNode)
    {
        this.zoomPane = zoomPane;
        zoomPane.setDrawCallback(this::onDraw);
        zoomPane.setMouseDraggedListener(this::onMouseDragged);
        zoomPane.setMousePressedListener(this::onMousePressed);
        zoomPane.setMouseReleasedListener(this::onMouseReleased);
        zoomPane.setMouseClickedListener(this::onMouseClicked);
        zoomPane.setZoomFactorListener(this::onZoomFactorChanged);
        keyPressedNode.setOnKeyPressed(this::onKeyPressed);
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    //------------//
    // Renderable //
    //------------//

    public void reset(final double quadTreeWidth, final double quadTreeHeight)
    {
        previousEditModesStack.clear();
        selectedObjects.clear();
        zoomFactorListenerObjects.clear();

        quadTree = new DoubleQuadTree<>(0, 0, quadTreeWidth, quadTreeHeight, 10);

        editMode = null;

        pressButton = null;
        pressObject = null;
        dragObject = null;

        for (final EditMode editMode : cachedEditModes.values())
        {
            editMode.reset();
        }
    }

    public void addRenderable(final AbstractRenderable renderable)
    {
        if (quadTree.add(renderable, renderable.getCorners()))
        {
            // New renderable
            if (renderable.listensToZoomFactorChanges())
            {
                zoomFactorListenerObjects.addTail(renderable);
            }
        }
        requestDraw();
    }

    public void removeRenderable(final AbstractRenderable renderable)
    {
        zoomFactorListenerObjects.remove(renderable);
        selectedObjects.remove(renderable);
        quadTree.remove(renderable);
        requestDraw();
    }

    public void requestDraw()
    {
        zoomPane.requestDraw();
    }

    public ObservableList<Snapshot> getSnapshots()
    {
        return observableSnapshots;
    }

    //-----------//
    // Selection //
    //-----------//

    public Iterable<AbstractRenderable> selectedObjects()
    {
        return MiscUtil.readOnlyIterable(selectedObjects);
    }

    public boolean isSelected(final AbstractRenderable renderable)
    {
        return selectedObjects.contains(renderable);
    }

    public int selectedCount()
    {
        return selectedObjects.size();
    }

    public void select(final AbstractRenderable renderable)
    {
        if (selectedObjects.contains(renderable))
        {
            return;
        }

        renderable.onBeforeSelected();

        for (final AbstractRenderable selectedObject : selectedObjects)
        {
            selectedObject.onBeforeAdditionalObjectSelected(renderable);
        }

        selectedObjects.addTail(renderable);
    }

    public void unselect(final AbstractRenderable renderable)
    {
        if (!selectedObjects.contains(renderable))
        {
            return;
        }

        renderable.onUnselected();
        selectedObjects.remove(renderable);
    }

    public void unselectAll()
    {
        for (final AbstractRenderable renderable : selectedObjects)
        {
            renderable.onUnselected();
        }
        selectedObjects.clear();
    }

    //--------//
    // Canvas //
    //--------//

    public double getZoomFactor()
    {
        return zoomPane.getZoomFactor();
    }

    public Bounds getCanvasBounds()
    {
        return zoomPane.getCanvasBounds();
    }

    public WritableImage getLatestCanvasBackgroundImage()
    {
        return zoomPane.getLatestCanvasBackgroundImage();
    }

    public void doOperationMaintainViewportCenter(final Supplier<Boolean> operation)
    {
        zoomPane.doOperationMaintainViewportCenter(operation);
    }

    public void doOperationMaintainViewportLeft(final Supplier<Boolean> operation)
    {
        zoomPane.doOperationMaintainViewportLeft(operation);
    }

    //------------------//
    // Position Helpers //
    //------------------//

    public Point2D sourceToCanvasPosition(final int srcX, final int srcY)
    {
        return zoomPane.sourceToCanvasPosition(srcX, srcY);
    }

    public Point2D sourceToCanvasDoublePosition(final double srcX, final double srcY)
    {
        return zoomPane.sourceToCanvasDoublePosition(srcX, srcY);
    }

    public Point canvasToSourcePosition(final int canvasX, final int canvasY)
    {
        return zoomPane.canvasToSourcePosition(canvasX, canvasY);
    }

    public Point2D canvasToSourceDoublePosition(final double canvasX, final double canvasY)
    {
        return zoomPane.canvasToSourceDoublePosition(canvasX, canvasY);
    }

    public double getCanvasWidth()
    {
        return zoomPane.getWidth();
    }

    public double getCanvasHeight()
    {
        return zoomPane.getHeight();
    }

    public Point2D absoluteToRelativeCanvasPosition(final int canvasX, final int canvasY)
    {
        return zoomPane.absoluteToRelativeCanvasPosition(canvasX, canvasY);
    }

    public Point absoluteCanvasToSourcePosition(final int canvasX, final int canvasY)
    {
        return zoomPane.absoluteCanvasToSourcePosition(canvasX, canvasY);
    }

    public Point2D absoluteCanvasToSourceDoublePosition(final double canvasX, final double canvasY)
    {
        return zoomPane.absoluteCanvasToSourceDoublePosition(canvasX, canvasY);
    }

    public Rectangle2D cornersToAbsoluteCanvasRectangle(final DoubleCorners corners)
    {
        final Point2D canvasPointTopLeft = sourceToCanvasDoublePosition(
            corners.topLeftX(), corners.topLeftY());

        final Point2D canvasPointBottomRight = sourceToCanvasDoublePosition(
            corners.bottomRightExclusiveX(), corners.bottomRightExclusiveY());

        return new Rectangle2D(canvasPointTopLeft.getX(), canvasPointTopLeft.getY(),
            canvasPointBottomRight.getX() - canvasPointTopLeft.getX(),
            canvasPointBottomRight.getY() - canvasPointTopLeft.getY());
    }

    public Point getEventSourcePosition(final MouseEvent event)
    {
        final double x = event.getX();
        final double y = event.getY();
        final Point2D sourcePoint = absoluteCanvasToSourceDoublePosition(x, y);
        return new Point(
            (int)Math.round(sourcePoint.getX()),
            (int)Math.round(sourcePoint.getY())
        );
    }

    public Point2D getCornersCanvasCenter(final DoubleCorners corners)
    {
        final Point2D canvasTopLeft = sourceToCanvasDoublePosition(
            corners.topLeftX(), corners.topLeftY());

        final Point2D canvasBottomRightExclusive = sourceToCanvasDoublePosition(
            corners.bottomRightExclusiveX(), corners.bottomRightExclusiveY());

        final double centerX = (canvasTopLeft.getX() + canvasBottomRightExclusive.getX()) / 2;
        final double centerY = (canvasTopLeft.getY() + canvasBottomRightExclusive.getY()) / 2;
        return new Point2D(centerX, centerY);
    }

    //----------//
    // QuadTree //
    //----------//

    public void setRenderingComparator(final Comparator<AbstractRenderable> comparator)
    {
        this.renderingComparator = comparator;
    }

    public void setInteractionComparator(final Comparator<AbstractRenderable> comparator)
    {
        this.interactionComparator = comparator;
    }

    public boolean objectInArea(final AbstractRenderable renderable, final DoubleCorners corners)
    {
        return (editMode.forceEnableObject(renderable) || renderable.isEnabled())
            && renderable.getCorners().intersect(corners) != null;
    }

    public boolean pointInObjectCornersFudge(
        final Point2D point, final AbstractRenderable renderable, final double fudgeAmount)
    {
        return !renderable.isHidden() && (editMode.forceEnableObject(renderable) || renderable.isEnabled())
            && renderable.getCorners().contains(point, fudgeAmount);
    }

    public boolean pointInObjectExact(final Point2D point, final AbstractRenderable renderable)
    {
        return !renderable.isHidden() &&
            (editMode.forceEnableObject(renderable) || renderable.isEnabled()) && renderable.contains(point);
    }

    public Iterable<AbstractRenderable> iterableNear(final DoubleCorners corners)
    {
        return quadTree.iterableNear(corners);
    }

    public void debugRenderCorners(final GraphicsContext canvasContext, final DoubleCorners corners)
    {
        canvasContext.setStroke(Color.rgb(0, 255, 0));
        canvasContext.setLineWidth(1);

        final Point2D t1 = sourceToCanvasDoublePosition(corners.topLeftX(), corners.topLeftY());
        final Point2D t2 = sourceToCanvasDoublePosition(
            corners.bottomRightExclusiveX(), corners.bottomRightExclusiveY());

        canvasContext.strokeRect(t1.getX(), t1.getY(),
            t2.getX() - t1.getX(),
            t2.getY() - t1.getY());
    }

    //--------------//
    // Editor State //
    //--------------//

    public <T extends EditMode> void registerEditMode(final Class<T> clazz, final Supplier<T> supplier)
    {
        if (cachedEditModes.containsKey(clazz)) return;
        cachedEditModes.put(clazz, supplier.get());
    }

    public <T extends EditMode> T getRegisteredEditMode(final Class<T> clazz)
    {
        //noinspection unchecked
        return (T)cachedEditModes.get(clazz);
    }

    public void enterEditMode(final Class<? extends EditMode> nextEditModeClass)
    {
        final EditMode nextEditMode = cachedEditModes.get(nextEditModeClass);
        if (nextEditMode == null) throw new IllegalStateException();
        if (editMode != null)
        {
            previousEditModesStack.push(editMode);
            editMode.onModeSuspend();
        }
        editMode = nextEditMode;
        editMode.onModeStart();
    }

    public void exitEditMode()
    {
        editMode.onModeEnd();
        editMode = previousEditModesStack.pop();
        editMode.onModeResume();
    }

    public EditMode getCurrentEditMode()
    {
        return editMode;
    }

    public EditMode getPreviousEditMode()
    {
        return previousEditModesStack.isEmpty() ? null : previousEditModesStack.peek();
    }

    public MouseButton getPressButton()
    {
        return pressButton;
    }

    public void setPressButton(final MouseButton button)
    {
        pressButton = button;
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void onDraw(final GraphicsContext canvasContext)
    {
        final DoubleCorners visibleSourceCorners = zoomPane.getVisibleSourceDoubleCorners();
        final List<AbstractRenderable> renderables = quadTree.listNear(visibleSourceCorners, renderingComparator,
            (renderable) -> objectInArea(renderable, visibleSourceCorners));

        for (final AbstractRenderable renderable : renderables)
        {
            if (!renderable.isHidden())
            {
                renderable.onRender(canvasContext, 1);
            }
        }

        //-----------------------------------------------------------------------------------------------------//
        // Resort the renderables based on distance from viewport center in preparation for snapshot rendering //
        //-----------------------------------------------------------------------------------------------------//

        final Canvas canvas = canvasContext.getCanvas();
        final int canvasWidth = (int)canvas.getWidth();
        final int canvasHeight = (int)canvas.getHeight();

        final Point2D viewSourceCenter = canvasToSourceDoublePosition(
            (double)canvasWidth / 2, (double)canvasHeight / 2);

        renderables.sort((o1, o2) ->
        {
            final Point2D o1Center = o1.getCorners().getCenter();
            final Point2D o2Center = o2.getCorners().getCenter();

            final int distanceCompare = Double.compare(
                o1Center.distance(viewSourceCenter), o2Center.distance(viewSourceCenter));

            return distanceCompare == 0 ? interactionComparator.compare(o1, o2) : distanceCompare;
        });

        // Prepare the snapshot Canvas for rendering
        final WritableImage curBackgroundImage = zoomPane.getLatestCanvasBackgroundImage();
        observableSnapshots.clear();

        //----------------------//
        // Render the snapshots //
        //----------------------//

        for (final AbstractRenderable renderable : renderables)
        {
            if (!renderable.snapshotable())
            {
                continue;
            }

            final DoubleCorners corners = renderable.getCorners();

            final Point2D canvasTopLeft = sourceToCanvasDoublePosition(
                corners.topLeftX(), corners.topLeftY());

            final Point2D canvasBottomRightExclusive = sourceToCanvasDoublePosition(
                corners.bottomRightExclusiveX(), corners.bottomRightExclusiveY());

            final double bottomRightExclusiveX = Math.min(canvasBottomRightExclusive.getX(), canvasWidth);
            final double bottomRightExclusiveY = Math.min(canvasBottomRightExclusive.getY(), canvasHeight);

            final double renderX = Math.max(0, canvasTopLeft.getX());
            final double renderY = Math.max(0, canvasTopLeft.getY());

            final double srcW = bottomRightExclusiveX - renderX;
            final double srcH = bottomRightExclusiveY - renderY;

            final double scaleX = srcW > MAX_SNAPSHOT_SIDE ? MAX_SNAPSHOT_SIDE / srcW : 1;
            final double scaleY = srcH > MAX_SNAPSHOT_SIDE ? MAX_SNAPSHOT_SIDE / srcH : 1;
            final double scale = Math.min(scaleX, scaleY);

            final double renderW = scale * srcW;
            final double renderH = scale * srcH;

            final Canvas snapshotCanvas = snapshotCanvasCache.getCacheCanvas();
            final GraphicsContext snapshotContext = snapshotCanvas.getGraphicsContext2D();

            snapshotCanvas.setWidth(renderW);
            snapshotCanvas.setHeight(renderH);

            final Affine saved = canvasContext.getTransform();

            snapshotContext.drawImage(curBackgroundImage,
                renderX, renderY, srcW, srcH,
                0, 0, renderW, renderH);

            snapshotContext.scale(scale, scale);
            snapshotContext.translate(-renderX, -renderY);
            renderable.onRender(snapshotContext, scale);

            snapshotContext.setTransform(saved);

            observableSnapshots.add(new Snapshot(renderable, snapshotCanvas));
        }

        editMode.onDraw(canvasContext);
    }

    private void onMousePressed(final MouseEvent event)
    {
        final MouseButton customMousePressedButton = editMode.customOnMousePressed(event);
        if (customMousePressedButton != null)
        {
            pressButton = customMousePressedButton;
        }

        if (pressButton != null)
        {
            return;
        }

        final double absoluteCanvasX = event.getX();
        final double absoluteCanvasY = event.getY();

        final Point2D sourcePressPos = zoomPane.absoluteCanvasToSourceDoublePosition(absoluteCanvasX, absoluteCanvasY);
        final double sourcePressX = sourcePressPos.getX();
        final double sourcePressY = sourcePressPos.getY();

        final double fudgeAmount = 10 / zoomPane.getZoomFactor();
        final DoubleCorners fudgeCorners = new DoubleCorners(
            sourcePressX - fudgeAmount, sourcePressY - fudgeAmount,
            sourcePressX + fudgeAmount + 1, sourcePressY + fudgeAmount + 1
        );

        boolean pressedSomething = false;

        // Get all objects with corners within the fudge amount
        final List<AbstractRenderable> fudgedObjects = quadTree.listNear(fudgeCorners, interactionComparator,
            (renderable) -> pointInObjectCornersFudge(sourcePressPos, renderable, fudgeAmount));

        // Prioritize objects that actually contain the click
        for (final AbstractRenderable renderable : fudgedObjects)
        {
            if (pointInObjectExact(sourcePressPos, renderable)
                && editMode.shouldCaptureObjectPress(event, renderable)
                && renderable.offerPressCapture(event))
            {
                pressObject = renderable;
                pressButton = event.getButton();
                pressedSomething = true;
                break;
            }
        }

        // If no object that actually contained the click was captured, check if a fudged object can be captured
        if (!pressedSomething)
        {
            for (final AbstractRenderable renderable : fudgedObjects)
            {
                if (editMode.shouldCaptureObjectPress(event, renderable) && renderable.offerPressCapture(event))
                {
                    pressObject = renderable;
                    pressButton = event.getButton();
                    pressedSomething = true;
                    break;
                }
            }
        }

        if (pressedSomething)
        {
            return;
        }

        editMode.onBackgroundPressed(event, sourcePressX, sourcePressY);
    }

    private void onMouseDragged(final MouseEvent event)
    {
        dragOccurred = true;
        if (editMode.customOnMouseDragged(event))
        {
            return;
        }

        final MouseButton button = event.getButton();

        if (pressObject == null)
        {
            pressObject = editMode.directCaptureDraggedObject(event);
            if (pressObject != null)
            {
                pressButton = button;
            }
        }

        if (button != pressButton)
        {
            return;
        }

        if (dragObject == null && editMode.shouldCaptureObjectDrag(event, pressObject)
            && pressObject.offerDragCapture(event))
        {
            dragObject = pressObject;
        }

        if (dragObject == null)
        {
            return;
        }

        editMode.onObjectDragged(event, dragObject);
        event.consume();
    }

    private void onMouseReleased(final MouseEvent event)
    {
        if (editMode.customOnMouseReleased(event))
        {
            dragOccurred = false;
            return;
        }

        final MouseButton button = event.getButton();

        if (button == pressButton)
        {
            if (!dragOccurred)
            {
                final Point2D sourcePoint = zoomPane.absoluteCanvasToSourceDoublePosition(event.getX(), event.getY());
                if (pointInObjectExact(sourcePoint, pressObject))
                {
                    pressObject.onClicked(event);
                }
            }
            pressButton = null;
            pressObject = null;
            dragObject = null;
        }
        dragOccurred = false;
    }

    private void onMouseClicked(final MouseEvent event)
    {
        event.consume();
    }

    private void onZoomFactorChanged(final double zoomFactor)
    {
        for (final AbstractRenderable renderable : zoomFactorListenerObjects)
        {
            renderable.onZoomFactorChanged(zoomFactor);
        }
    }

    private void onKeyPressed(final KeyEvent event)
    {
        editMode.onKeyPressed(event);

        if (event.isConsumed())
        {
            return;
        }

        for (final AbstractRenderable renderable : selectedObjects)
        {
            renderable.onReceiveKeyPress(event);

            if (event.isConsumed())
            {
                break;
            }
        }
    }

    ////////////////////
    // Public Classes //
    ////////////////////

    public record Snapshot(AbstractRenderable renderable, Canvas canvas) {}
}
