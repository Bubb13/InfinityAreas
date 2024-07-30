
package com.github.bubb13.infinityareas.gui.editor;

import com.github.bubb13.infinityareas.gui.editor.editmode.EditMode;
import com.github.bubb13.infinityareas.gui.editor.renderable.AbstractRenderable;
import com.github.bubb13.infinityareas.gui.pane.ZoomPane;
import com.github.bubb13.infinityareas.misc.DoubleCorners;
import com.github.bubb13.infinityareas.misc.DoubleQuadTree;
import com.github.bubb13.infinityareas.misc.OrderedInstanceSet;
import com.github.bubb13.infinityareas.util.MiscUtil;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.awt.Point;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.function.Supplier;

public class Editor
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final ZoomPane zoomPane;
    private final HashMap<Class<? extends EditMode>, EditMode> cachedEditModes = new HashMap<>();
    private final Stack<EditMode> previousEditModesStack = new Stack<>();
    private final OrderedInstanceSet<AbstractRenderable> selectedObjects = new OrderedInstanceSet<>();
    private final OrderedInstanceSet<AbstractRenderable> zoomFactorListenerObjects = new OrderedInstanceSet<>();

    private DoubleQuadTree<AbstractRenderable> quadTree = null;
    private Comparator<AbstractRenderable> renderingComparator;
    private Comparator<AbstractRenderable> interactionComparator;

    private EditMode editMode = null;

    private MouseButton pressButton = null;
    private AbstractRenderable pressObject = null;
    private boolean dragOccurred = false;
    private AbstractRenderable dragObject = null;

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

    public Iterable<AbstractRenderable> selectedObjects()
    {
        return MiscUtil.readOnlyIterable(selectedObjects);
    }

    public void doOperationMaintainViewportCenter(final Supplier<Boolean> operation)
    {
        zoomPane.doOperationMaintainViewportCenter(operation);
    }

    public void doOperationMaintainViewportLeft(final Supplier<Boolean> operation)
    {
        zoomPane.doOperationMaintainViewportLeft(operation);
    }

    public Point2D sourceToAbsoluteCanvasPosition(final int srcX, final int srcY)
    {
        return zoomPane.sourceToAbsoluteCanvasPosition(srcX, srcY);
    }

    public Point2D sourceToAbsoluteCanvasDoublePosition(final double srcX, final double srcY)
    {
        return zoomPane.sourceToAbsoluteCanvasDoublePosition(srcX, srcY);
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

    public Bounds getCanvasBounds()
    {
        return zoomPane.getCanvasBounds();
    }

    public Rectangle2D cornersToAbsoluteCanvasRectangle(final DoubleCorners corners)
    {
        final Point2D canvasPointTopLeft = sourceToAbsoluteCanvasDoublePosition(
            corners.topLeftX(), corners.topLeftY());

        final Point2D canvasPointBottomRight = sourceToAbsoluteCanvasDoublePosition(
            corners.bottomRightExclusiveX(), corners.bottomRightExclusiveY());

        return new Rectangle2D(canvasPointTopLeft.getX(), canvasPointTopLeft.getY(),
            canvasPointBottomRight.getX() - canvasPointTopLeft.getX(),
            canvasPointBottomRight.getY() - canvasPointTopLeft.getY());
    }

    public double getZoomFactor()
    {
        return zoomPane.getZoomFactor();
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

    public boolean objectInArea(final AbstractRenderable renderable, final DoubleCorners corners)
    {
        return (editMode.forceEnableObject(renderable) || renderable.isEnabled())
            && renderable.getCorners().intersect(corners) != null;
    }

    public boolean pointInObjectCornersFudge(
        final Point2D point, final AbstractRenderable renderable, final double fudgeAmount)
    {
        return (editMode.forceEnableObject(renderable) || renderable.isEnabled())
            && renderable.getCorners().contains(point, fudgeAmount);
    }

    public boolean pointInObjectExact(final Point2D point, final AbstractRenderable renderable)
    {
        return (editMode.forceEnableObject(renderable) || renderable.isEnabled()) && renderable.contains(point);
    }

    public MouseButton getPressButton()
    {
        return pressButton;
    }

    public void setPressButton(final MouseButton button)
    {
        pressButton = button;
    }

    public Iterable<AbstractRenderable> iterableNear(final DoubleCorners corners)
    {
        return quadTree.iterableNear(corners);
    }

    public EditMode getPreviousEditMode()
    {
        return previousEditModesStack.isEmpty() ? null : previousEditModesStack.peek();
    }

    public EditMode getEditMode()
    {
        return editMode;
    }

    public <T extends EditMode> void registerEditMode(final Class<T> clazz, final Supplier<T> supplier)
    {
        if (cachedEditModes.containsKey(clazz)) return;
        cachedEditModes.put(clazz, supplier.get());
    }

    public <T extends EditMode> T getEditMode(final Class<T> clazz)
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
            editMode.onExitMode();
        }
        editMode = nextEditMode;
        editMode.onEnterMode();
    }

    public void exitEditMode()
    {
        editMode.onExitMode();
        editMode = previousEditModesStack.pop();
        editMode.onEnterMode();
    }

    public void debugRenderCorners(final GraphicsContext canvasContext, final DoubleCorners corners)
    {
        canvasContext.setStroke(Color.rgb(0, 255, 0));
        canvasContext.setLineWidth(1);

        final Point2D t1 = sourceToAbsoluteCanvasDoublePosition(corners.topLeftX(), corners.topLeftY());
        final Point2D t2 = sourceToAbsoluteCanvasDoublePosition(
            corners.bottomRightExclusiveX(), corners.bottomRightExclusiveY());

        canvasContext.strokeRect(t1.getX(), t1.getY(),
            t2.getX() - t1.getX(),
            t2.getY() - t1.getY());
    }

    public void setRenderingComparator(final Comparator<AbstractRenderable> comparator)
    {
        this.renderingComparator = comparator;
    }

    public void setInteractionComparator(final Comparator<AbstractRenderable> comparator)
    {
        this.interactionComparator = comparator;
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void onDraw(final GraphicsContext canvasContext)
    {
        final DoubleCorners visibleSourceCorners = zoomPane.getVisibleSourceDoubleCorners();
        for (final AbstractRenderable renderable : quadTree.listNear(visibleSourceCorners, renderingComparator,
            (renderable) -> objectInArea(renderable, visibleSourceCorners)))
        {
            renderable.onRender(canvasContext);
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
}
