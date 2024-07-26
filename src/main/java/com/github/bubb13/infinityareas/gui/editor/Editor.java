
package com.github.bubb13.infinityareas.gui.editor;

import com.github.bubb13.infinityareas.gui.editor.editmode.EditMode;
import com.github.bubb13.infinityareas.gui.editor.renderable.Renderable;
import com.github.bubb13.infinityareas.gui.pane.ZoomPane;
import com.github.bubb13.infinityareas.misc.DoubleCorners;
import com.github.bubb13.infinityareas.misc.DoubleQuadTree;
import com.github.bubb13.infinityareas.misc.OrderedInstanceSet;
import com.github.bubb13.infinityareas.util.MiscUtil;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.awt.Point;
import java.util.HashMap;
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
    private final OrderedInstanceSet<Renderable> selectedObjects = new OrderedInstanceSet<>();
    private final OrderedInstanceSet<Renderable> zoomFactorListenerObjects = new OrderedInstanceSet<>();

    private DoubleQuadTree<Renderable> quadTree = null;

    private EditMode editMode = null;

    private MouseButton pressButton = null;
    private Renderable pressObject = null;
    private Renderable dragObject = null;

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

    public void addRenderable(final Renderable renderable)
    {
        if (quadTree.add(renderable, renderable.getCorners()))
        {
            // New renderable
            if (renderable.listensToZoomFactorChanges())
            {
                zoomFactorListenerObjects.addTail(renderable);
            }
        }
    }

    public void removeRenderable(final Renderable renderable)
    {
        zoomFactorListenerObjects.remove(renderable);
        selectedObjects.remove(renderable);
        quadTree.remove(renderable);
    }

    public void requestDraw()
    {
        zoomPane.requestDraw();
    }

    public boolean isSelected(final Renderable renderable)
    {
        return selectedObjects.contains(renderable);
    }

    public int selectedCount()
    {
        return selectedObjects.size();
    }

    public void select(final Renderable renderable)
    {
        selectedObjects.addTail(renderable);
        renderable.onSelected();
    }

    public void unselect(final Renderable renderable)
    {
        renderable.onUnselected();
        selectedObjects.remove(renderable);
    }

    public void unselectAll()
    {
        for (final Renderable renderable : selectedObjects)
        {
            renderable.onUnselected();
        }
        selectedObjects.clear();
    }

    public Iterable<Renderable> selectedObjects()
    {
        return MiscUtil.readOnlyIterable(selectedObjects);
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

    public Bounds getCanvasBounds()
    {
        return zoomPane.getCanvasBounds();
    }

    public double getZoomFactor()
    {
        return zoomPane.getZoomFactor();
    }

    public Point getEventSourcePosition(final MouseEvent event)
    {
        final int absoluteCanvasX = (int)event.getX();
        final int absoluteCanvasY = (int)event.getY();
        return absoluteCanvasToSourcePosition(absoluteCanvasX, absoluteCanvasY);
    }

    public boolean objectInArea(final Renderable renderable, final DoubleCorners corners)
    {
        return (editMode.forceEnableObject(renderable) || renderable.isEnabled())
            && renderable.getCorners().intersect(corners) != null;
    }

    public boolean pointInObjectFudge(final Point2D point, final Renderable renderable, final double fudgeAmount)
    {
        return (editMode.forceEnableObject(renderable) || renderable.isEnabled())
            && renderable.getCorners().contains(point, fudgeAmount);
    }

    public boolean pointInObjectExact(final Point2D point, final Renderable renderable)
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

    public Iterable<Renderable> iterableNear(final DoubleCorners corners)
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

    /////////////////////
    // Private Methods //
    /////////////////////

    private void onDraw(final GraphicsContext canvasContext)
    {
        final DoubleCorners visibleSourceCorners = zoomPane.getVisibleSourceDoubleCorners();
        quadTree.iterateNear(visibleSourceCorners, (renderable) ->
        {
            if (objectInArea(renderable, visibleSourceCorners))
            {
                renderable.onRender(canvasContext);
            }
        });

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

        for (final Renderable renderable : quadTree.iterableNear(fudgeCorners))
        {
            if (!pointInObjectFudge(sourcePressPos, renderable, fudgeAmount))
            {
                continue;
            }

            if (editMode.shouldCaptureObjectPress(event, renderable))
            {
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

        editMode.onBackgroundPressed(event, sourcePressX, sourcePressY);
    }

    private void onMouseDragged(final MouseEvent event)
    {
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

        if (dragObject == null && editMode.shouldCaptureObjectDrag(event, pressObject))
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
            return;
        }

        final MouseButton button = event.getButton();

        if (button == pressButton)
        {
            if (dragObject == null)
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
    }

    private void onMouseClicked(final MouseEvent event)
    {
        event.consume();
    }

    private void onZoomFactorChanged(final double zoomFactor)
    {
        for (final Renderable renderable : zoomFactorListenerObjects)
        {
            renderable.onZoomFactorChanged(zoomFactor);
        }
    }

    private void onKeyPressed(final KeyEvent event)
    {
        editMode.onKeyPressed(event);
    }
}
