
package com.github.bubb13.infinityareas.gui.editor.renderable;

import com.github.bubb13.infinityareas.game.resource.Area;
import com.github.bubb13.infinityareas.gui.editor.Editor;
import com.github.bubb13.infinityareas.misc.DoubleCorners;
import com.github.bubb13.infinityareas.util.MathUtil;
import com.github.bubb13.infinityareas.util.MiscUtil;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.awt.Point;

public class RenderableActorOrientationHandle extends AbstractRenderable
{
    ///////////////////////////
    // Private Static Fields //
    ///////////////////////////

    private static final double LINE_LENGTH = 10;
    private static final double LINE_WIDTH = 5;

    ////////////////////
    // Private Fields //
    ////////////////////

    private final RenderableActor renderableActor;
    private final DoubleCorners corners = new DoubleCorners();

    private double lineDegrees;
    private double lineStartPointX;
    private double lineStartPointY;
    private double lineEndPointX;
    private double lineEndPointY;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public RenderableActorOrientationHandle(final Editor editor, final RenderableActor renderableActor)
    {
        super(editor);
        this.renderableActor = renderableActor;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void recalculateLine()
    {
        final Area.Actor actor = renderableActor.getActor();

        lineDegrees = actor.getOrientationDegree();

        final double radians = Math.toRadians(lineDegrees);

        final double offsetLength = RenderableActor.RADIUS - LINE_LENGTH / 2;
        lineStartPointX = actor.getX() + offsetLength * Math.cos(radians);
        lineStartPointY = actor.getY() - offsetLength * Math.sin(radians);

        final double endLength = offsetLength + LINE_LENGTH;
        lineEndPointX = actor.getX() + endLength * Math.cos(radians);
        lineEndPointY = actor.getY() - endLength * Math.sin(radians);

        recalculateCorners();
    }

    @Override
    public int sortWeight()
    {
        return renderableActor.sortWeight() + 1;
    }

    @Override
    public void onRender(final GraphicsContext canvasContext, final double scaleCorrection)
    {
        canvasContext.setStroke(Color.MAGENTA);
        canvasContext.setLineWidth(LINE_WIDTH * editor.getZoomFactor());

        final Point2D startPoint = editor.sourceToCanvasDoublePosition(lineStartPointX, lineStartPointY);
        final Point2D endPoint = editor.sourceToCanvasDoublePosition(lineEndPointX, lineEndPointY);

        canvasContext.strokeLine(startPoint.getX(), startPoint.getY(), endPoint.getX(), endPoint.getY());
    }

    @Override
    public DoubleCorners getCorners()
    {
        return corners;
    }

    @Override
    public boolean isEnabled()
    {
        return true;
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

    @Override
    public void onDragged(final MouseEvent event)
    {
        final Area.Actor actor = renderableActor.getActor();
        final Point sourcePos = editor.getEventSourcePosition(event);
        final double angle = MiscUtil.calculateAngle(actor.getX(), actor.getY(), sourcePos.getX(), sourcePos.getY());
        actor.setOrientation(angleToOrientation(angle));
        recalculateLine();
    }

    @Override
    public boolean listensToZoomFactorChanges()
    {
        return true;
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void recalculateCorners()
    {
        final double topLeftX = Math.min(lineStartPointX, lineEndPointX);
        final double topLeftY = Math.min(lineStartPointY, lineEndPointY);
        final double bottomRightExclusiveX = Math.max(lineStartPointX, lineEndPointX);
        final double bottomRightExclusiveY = Math.max(lineStartPointY, lineEndPointY);

        final double halfLineWidth = LINE_WIDTH / 2;

        corners.setTopLeftX(MathUtil.extendCornerLeft(topLeftX, halfLineWidth, lineDegrees));
        corners.setBottomRightExclusiveX(MathUtil.extendCornerRight(bottomRightExclusiveX, halfLineWidth, lineDegrees));

        corners.setTopLeftY(MathUtil.extendCornerUp(topLeftY, halfLineWidth, lineDegrees));
        corners.setBottomRightExclusiveY(MathUtil.extendCornerDown(bottomRightExclusiveY, halfLineWidth, lineDegrees));

        editor.addRenderable(this);
    }

    // TODO - Improve
    private short angleToOrientation(final double angle)
    {
        return switch ((int)MiscUtil.snapToNearest(angle, 15))
        {
            case 0, 360 -> 12; // E
            case 15 -> 13;     // SEE
            case 30 -> angleToOrientation(MiscUtil.snapToNearest(angle, 15, 45));
            case 45 -> 14;     // SE
            case 60 -> angleToOrientation(MiscUtil.snapToNearest(angle, 45, 75));
            case 75 -> 15;     // SSE
            case 90 -> 0;      // S
            case 105 -> 1;     // SSW
            case 120 -> angleToOrientation(MiscUtil.snapToNearest(angle, 105, 135));
            case 135 -> 2;     // SW
            case 150 -> angleToOrientation(MiscUtil.snapToNearest(angle, 135, 165));
            case 165 -> 3;     // SWW
            case 180 -> 4;     // W
            case 195 -> 5;     // NWW
            case 210 -> angleToOrientation(MiscUtil.snapToNearest(angle, 195, 225));
            case 225 -> 6;     // NW
            case 240 -> angleToOrientation(MiscUtil.snapToNearest(angle, 225, 255));
            case 255 -> 7;     // NNW
            case 270 -> 8;     // N
            case 285 -> 9;     // NNE
            case 300 -> angleToOrientation(MiscUtil.snapToNearest(angle, 285, 315));
            case 315 -> 10;    // NE
            case 330 -> angleToOrientation(MiscUtil.snapToNearest(angle, 315, 345));
            case 345 -> 11;    // NEE
            default -> throw new IllegalStateException();
        };
    }
}
