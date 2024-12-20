
package com.github.bubb13.infinityareas.gui.editor.editmode;

import com.github.bubb13.infinityareas.gui.editor.Editor;
import com.github.bubb13.infinityareas.gui.editor.renderable.AbstractRenderable;
import com.github.bubb13.infinityareas.gui.editor.renderable.RenderableVertex;
import com.github.bubb13.infinityareas.misc.DoubleCorners;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.awt.Point;

public class QuickSelectEditMode extends LabeledEditMode
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private QuickSelectRectangle quickSelectRectangle;
    private boolean quickSelectRender;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public QuickSelectEditMode(final Editor editor)
    {
        super(editor, "Quick Select Mode");
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    @Override
    public void reset()
    {
        super.reset();
        quickSelectRectangle = new QuickSelectRectangle();
        quickSelectRender = false;
    }

    @Override
    public EditModeForceEnableState forceObjectEnableState(final AbstractRenderable renderable)
    {
        final EditMode previousEditMode = editor.getPreviousEditMode();
        return previousEditMode != null ? previousEditMode.forceObjectEnableState(renderable) : EditModeForceEnableState.NO;
    }

    @Override
    public MouseButton customOnMousePressed(final MouseEvent event)
    {
        final MouseButton button = event.getButton();
        if (!quickSelectRender && button == MouseButton.PRIMARY)
        {
            final Point sourcePoint = editor.getEventSourcePosition(event);
            int sourcePressX = sourcePoint.x;
            int sourcePressY = sourcePoint.y;

            quickSelectRectangle.setOrigin(sourcePressX, sourcePressY);
            quickSelectRectangle.setBoundingCorner(sourcePressX, sourcePressY);
            quickSelectRender = true;

            return button;
        }
        return null;
    }

    @Override
    public boolean shouldCaptureBackgroundPress(
        final MouseEvent event, final double sourcePressX, final double sourcePressY)
    {
        return event.getButton() == MouseButton.PRIMARY;
    }

    @Override
    public boolean customOnMouseDragged(final MouseEvent event)
    {
        if (event.getButton() == editor.getPressButton())
        {
            event.consume();
            final Point sourcePos = editor.getEventSourcePosition(event);
            quickSelectRectangle.setBoundingCorner(sourcePos.x, sourcePos.y);
        }
        return true;
    }

    @Override
    public boolean customOnMouseReleased(final MouseEvent event)
    {
        if (event.getButton() != editor.getPressButton())
        {
            return true;
        }

        editor.performAsTransaction(() ->
        {
            if (!event.isShiftDown() && !event.isControlDown())
            {
                editor.unselectAll();
            }

            final DoubleCorners selectionCorners = quickSelectRectangle.getCorners();
            for (final AbstractRenderable renderable : editor.iterableNear(selectionCorners))
            {
                if (!editor.objectInArea(renderable, selectionCorners))
                {
                    continue;
                }

                if (renderable instanceof RenderableVertex renderableVertex)
                {
                    if (event.isControlDown())
                    {
                        if (editor.isSelected(renderableVertex))
                        {
                            editor.unselect(renderableVertex);
                        }
                        else
                        {
                            editor.select(renderableVertex);
                        }
                    }
                    else
                    {
                        editor.select(renderableVertex);
                    }
                }
            }
        });

        editor.setPressButton(null);
        quickSelectRender = false;
        editor.endEditMode();
        return true;
    }

    @Override
    public void onKeyPressed(final KeyEvent event)
    {
        if (event.getCode() == KeyCode.ESCAPE)
        {
            event.consume();
            editor.endEditMode();
        }
    }

    @Override
    public void onModeExit()
    {
        ownedUndo.delete();
        super.onModeExit();
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private class QuickSelectRectangle extends AbstractRenderable
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final DoubleCorners corners = new DoubleCorners();
        private int originX;
        private int originY;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public QuickSelectRectangle()
        {
            super(QuickSelectEditMode.this.editor);
            editor.addRenderable(this);
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        public void setOrigin(final int x, final int y)
        {
            originX = x;
            originY = y;
        }

        public void setBoundingCorner(final int x, final int y)
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

            editor.addRenderable(this);
        }

        @Override
        public int sortWeight()
        {
            return 9999;
        }

        @Override
        public boolean isEnabled()
        {
            return quickSelectRender;
        }

        @Override
        public void onRender(final GraphicsContext canvasContext, final double scaleCorrection)
        {
            canvasContext.setLineWidth(1D);
            canvasContext.setStroke(Color.rgb(0, 255, 0));

            final Point2D absoluteTopLeft = editor.sourceToCanvasPosition(
                (int)corners.topLeftX(), (int)corners.topLeftY());

            final Point2D absoluteBottomRightExclusive = editor.sourceToCanvasPosition(
                (int)corners.bottomRightExclusiveX(), (int)corners.bottomRightExclusiveY());

            canvasContext.strokeRect(absoluteTopLeft.getX(), absoluteTopLeft.getY(),
                absoluteBottomRightExclusive.getX() - absoluteTopLeft.getX(),
                absoluteBottomRightExclusive.getY() - absoluteTopLeft.getY());
        }

        @Override
        public DoubleCorners getCorners()
        {
            return corners;
        }
    }
}
