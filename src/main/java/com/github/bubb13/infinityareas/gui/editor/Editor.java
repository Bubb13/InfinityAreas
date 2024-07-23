
package com.github.bubb13.infinityareas.gui.editor;

import com.github.bubb13.infinityareas.gui.pane.ZoomPane;
import com.github.bubb13.infinityareas.misc.Corners;
import com.github.bubb13.infinityareas.misc.OrderedInstanceSet;
import com.github.bubb13.infinityareas.misc.QuadTree;
import com.github.bubb13.infinityareas.util.MiscUtil;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

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

    private QuadTree<Renderable> quadTree = null;

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
        keyPressedNode.setOnKeyPressed(this::onKeyPressed);
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void reset(final int quadTreeWidth, final int quadTreeHeight)
    {
        previousEditModesStack.clear();
        selectedObjects.clear();

        quadTree = new QuadTree<>(0, 0, quadTreeWidth, quadTreeHeight, 10);

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
        quadTree.add(renderable, renderable.getCorners());
    }

    public void removeRenderable(final Renderable renderable)
    {
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
        renderable.selected();
    }

    public void unselect(final Renderable renderable)
    {
        renderable.unselected();
        selectedObjects.remove(renderable);
    }

    public void unselectAll()
    {
        for (final Renderable renderable : selectedObjects)
        {
            renderable.unselected();
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

    public Point2D absoluteToRelativeCanvasPosition(final int canvasX, final int canvasY)
    {
        return zoomPane.absoluteToRelativeCanvasPosition(canvasX, canvasY);
    }

    public Point absoluteCanvasToSourcePosition(final int canvasX, final int canvasY)
    {
        return zoomPane.absoluteCanvasToSourcePosition(canvasX, canvasY);
    }

    public Point getEventSourcePosition(final MouseEvent event)
    {
        final int absoluteCanvasX = (int)event.getX();
        final int absoluteCanvasY = (int)event.getY();
        return absoluteCanvasToSourcePosition(absoluteCanvasX, absoluteCanvasY);
    }

    public boolean objectInArea(final Renderable renderable, final Corners corners)
    {
        return (editMode.forceEnableObject(renderable) || renderable.isEnabled())
            && renderable.getCorners().intersect(corners) != null;
    }

    public boolean pointInObject(final Point point, final Renderable renderable, final int fudgeAmount)
    {
        return (editMode.forceEnableObject(renderable) || renderable.isEnabled())
            && renderable.getCorners().contains(point, fudgeAmount);
    }

    public MouseButton getPressButton()
    {
        return pressButton;
    }

    public void setPressButton(final MouseButton button)
    {
        pressButton = button;
    }

    public Iterable<Renderable> iterableNear(final Corners corners)
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

    /////////////////////
    // Private Methods //
    /////////////////////

    private void onDraw(final GraphicsContext canvasContext)
    {
        final Corners visibleSourceCorners = zoomPane.getVisibleSourceCorners();
        quadTree.iterateNear(visibleSourceCorners, (renderable) ->
        {
            if (objectInArea(renderable, visibleSourceCorners))
            {
                renderable.render(canvasContext);
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

        final int absoluteCanvasX = (int)event.getX();
        final int absoluteCanvasY = (int)event.getY();

        final Point sourcePressPos = zoomPane.absoluteCanvasToSourcePosition(absoluteCanvasX, absoluteCanvasY);
        final int sourcePressX = sourcePressPos.x;
        final int sourcePressY = sourcePressPos.y;

        final int fudgeAmount = (int)(10 / zoomPane.getZoomFactor());
        final Corners fudgeCorners = new Corners(
            sourcePressX - fudgeAmount, sourcePressY - fudgeAmount,
            sourcePressX + fudgeAmount + 1, sourcePressY + fudgeAmount + 1
        );

        boolean pressedSomething = false;

        for (final Renderable renderable : quadTree.iterableNear(fudgeCorners))
        {
            if (!pointInObject(sourcePressPos, renderable, fudgeAmount))
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
                final Point sourcePoint = zoomPane.absoluteCanvasToSourcePosition((int)event.getX(), (int)event.getY());
                if (pressObject.getCorners().contains(sourcePoint))
                {
                    pressObject.clicked(event);
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

    private void onKeyPressed(final KeyEvent event)
    {
        editMode.onKeyPressed(event);
    }
}
