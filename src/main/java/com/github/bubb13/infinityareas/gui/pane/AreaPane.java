
package com.github.bubb13.infinityareas.gui.pane;

import com.github.bubb13.infinityareas.game.resource.Area;
import com.github.bubb13.infinityareas.game.resource.WED;
import com.github.bubb13.infinityareas.misc.QuadTree;
import com.github.bubb13.infinityareas.misc.TaskTracker;
import com.github.bubb13.infinityareas.util.ImageUtil;
import com.github.bubb13.infinityareas.util.MiscUtil;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.awt.Point;
import java.awt.image.BufferedImage;

public class AreaPane extends StackPane
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final ZoomPane zoomPane = new ZoomPane();

    private Area area;
    private Area.AreaGraphics graphics;
    private BufferedImage cachedBackground;

    private DragMode dragMode = DragMode.NONE;
    private Area.Actor actorBeingDragged;

    private QuadTree<Object> objects;

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

    public void setArea(final Area area) throws Exception
    {
        this.area = area;
        graphics = area.newGraphics();
        cachedBackground = renderBackground();

        WED.Overlay baseOverlay = area.getWed().getOverlays().get(0);

        objects = new QuadTree<>(
            0, 0,
            baseOverlay.getWidthInTiles() * 64, baseOverlay.getHeightInTiles() * 64,
            10, 10);

        for (final Area.Actor actor : area.getActors())
        {
            objects.add(actor, actor.getX() - 100, actor.getY() - 100,
                actor.getX() + 200, actor.getY() + 200);
        }
    }

    public BufferedImage renderBackground() throws Exception
    {
        final WED.Graphics wedGraphics = graphics.getWedGraphics();
        wedGraphics.renderOverlays(TaskTracker.DUMMY, 0, 1, 2, 3, 4);
        return ImageUtil.copyArgb(graphics.getImage());
    }

    public BufferedImage render() throws Exception
    {
        graphics.drawImage(cachedBackground, 0, 0);
        //graphics.setZoomFactor(1 / zoomPane.getZoomFactor());
        return ImageUtil.copyArgb(graphics.getImage());
    }

    public void setImage(final BufferedImage image)
    {
        zoomPane.setImage(image);
    }

    private void onZoomFactorChanged(final double zoomFactor)
    {
        try
        {
            //zoomPane.setImage(render(), false);
        }
        catch (final Exception e)
        {

        }
    }

    private void onDrawViewport(final GraphicsContext graphics)
    {
        final Font font = new Font("Arial", 28 * zoomPane.getZoomFactor());

        graphics.setFill(Color.WHITE);
        graphics.setFont(font);

        final Rectangle2D sourceRect = zoomPane.getVisibleSourceRect();

        var iterateResult = objects.iterableNear((int)sourceRect.getMinX(), (int)sourceRect.getMinY(),
            (int)sourceRect.getMaxX(), (int)sourceRect.getMaxY());

        for (final Object obj : iterateResult)
        {
            if (obj instanceof Area.Actor actor)
            {
                final Point2D actorPoint1 = zoomPane.sourceToAbsoluteCanvasPosition(
                    actor.getX() - 50, actor.getY() - 50
                );
                final double actorPoint1X = actorPoint1.getX();
                final double actorPoint1Y = actorPoint1.getY();

                final Point2D actorPoint2 = zoomPane.sourceToAbsoluteCanvasPosition(
                    actor.getX() + 50, actor.getY() + 50
                );
                final double actorPoint2X = actorPoint2.getX();
                final double actorPoint2Y = actorPoint2.getY();

                graphics.setStroke(Color.WHITE);
                graphics.setLineWidth(2 * zoomPane.getZoomFactor());
                graphics.strokeOval(actorPoint1X, actorPoint1Y, actorPoint2X - actorPoint1X, actorPoint2Y - actorPoint1Y);

                final Line points = calculateOrientationHandlePoints(actor);
                graphics.setStroke(Color.MAGENTA);
                graphics.strokeLine(points.p1().getX(), points.p1().getY(), points.p2().getX(), points.p2().getY());
            }
        }

        graphics.setStroke(Color.BLACK);
        graphics.setFill(Color.WHITE);
        graphics.setLineWidth(0.5 * zoomPane.getZoomFactor());

        for (final Object obj : iterateResult)
        {
            if (obj instanceof Area.Actor actor)
            {
                final Point2D actorPoint = zoomPane.sourceToAbsoluteCanvasPosition(actor.getX(), actor.getY());
                final Text tempText = new Text(actor.getName());
                tempText.setFont(font);
                final double textWidth = tempText.getBoundsInLocal().getWidth();
                final double textHeight = tempText.getBoundsInLocal().getHeight();

                final double startX = actorPoint.getX() - textWidth / 2;
                final double startY = actorPoint.getY() + textHeight / 4;

                graphics.fillText(actor.getName(), startX, startY);
                graphics.strokeText(actor.getName(), startX, startY);
            }
        }
    }

    final void onMouseDragged(final MouseEvent event)
    {
        if (event.isMiddleButtonDown())
        {
            return;
        }

        final int absoluteCanvasX = (int)event.getX();
        final int absoluteCanvasY = (int)event.getY();

        switch (dragMode)
        {
            case ORIENTATION ->
            {
                final Point sourcePos = zoomPane.absoluteCanvasToSourcePosition(absoluteCanvasX, absoluteCanvasY);
                final double angle = MiscUtil.calculateAngle(
                    actorBeingDragged.getX(), actorBeingDragged.getY(), sourcePos.getX(), sourcePos.getY()
                );
                actorBeingDragged.setOrientation(angleToOrientation(angle));
                zoomPane.requestDraw();
            }
            case NONE ->
            {
                return;
            }
        }

        event.consume();
    }

//    final void onMouseClicked(final MouseEvent event)
//    {
//        final int canvasX = (int)event.getX();
//        final int canvasY = (int)event.getY();
//    }

    final void onMousePressed(final MouseEvent event)
    {
        final int canvasX = (int)event.getX();
        final int canvasY = (int)event.getY();
        final Point2D relativeCanvasPos = zoomPane.absoluteToRelativeCanvasPosition(canvasX, canvasY);

        //System.out.printf("Click (Relative Canvas): (%d,%d)\n", relativeCanvasPos.x, relativeCanvasPos.y);

        final Rectangle2D sourceRect = zoomPane.getVisibleSourceRect();

        final var iterateResult = objects.iterableNear(
            (int)sourceRect.getMinX(), (int)sourceRect.getMinY(),
            (int)sourceRect.getMaxX(), (int)sourceRect.getMaxY());

        for (final Object obj : iterateResult)
        {
            if (obj instanceof Area.Actor actor)
            {
                final Line points = calculateOrientationHandlePoints(actor);
                final Rectangle2D handleRect = MiscUtil.getRectangleFromCornerPointsExpand(points.p1(), points.p2(), 10);

                if (handleRect.contains(relativeCanvasPos))
                {
                    dragMode = DragMode.ORIENTATION;
                    actorBeingDragged = actor;
                    break;
                }
            }
        }
    }

    private void onMouseReleased(final MouseEvent event)
    {
        dragMode = DragMode.NONE;
        actorBeingDragged = null;
    }

    private Line calculateOrientationHandlePoints(final Area.Actor actor)
    {
        final int degrees = switch (actor.getOrientation())
        {
            case 0 -> 90;   // S
            case 1 -> 105;  // SSW
            case 2 -> 135;  // SW
            case 3 -> 165;  // SWW
            case 4 -> 180;  // W
            case 5 -> 195;  // NWW
            case 6 -> 225;  // NW
            case 7 -> 255;  // NNW
            case 8 -> 270;  // N
            case 9 -> 285;  // NNE
            case 10 -> 315; // NE
            case 11 -> 345; // NEE
            case 12 -> 0;   // E
            case 13 -> 15;  // SEE
            case 14 -> 45;  // SE
            case 15 -> 75;  // SSE
            default -> throw new IllegalStateException();
        };

        final double radians = Math.toRadians(degrees);

        final int offsetLength = 45;
        final int startLineX = (int)(actor.getX() + offsetLength * Math.cos(radians));
        final int startLineY = (int)(actor.getY() + offsetLength * Math.sin(radians));
        final Point2D offsetPoint = zoomPane.sourceToAbsoluteCanvasPosition(startLineX, startLineY);

        final int endLength = offsetLength + 10;
        final int newX = (int)(actor.getX() + endLength * Math.cos(radians));
        final int newY = (int)(actor.getY() + endLength * Math.sin(radians));
        final Point2D endDirPoint = zoomPane.sourceToAbsoluteCanvasPosition(newX, newY);

        return new Line(offsetPoint, endDirPoint);
    }

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

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init()
    {
        zoomPane.setZoomFactorListener(this::onZoomFactorChanged);
        zoomPane.setDrawCallback(this::onDrawViewport);
        zoomPane.setMouseDraggedListener(this::onMouseDragged);
        //zoomPane.setMouseClickedListener(this::onMouseClicked);
        zoomPane.setMousePressedListener(this::onMousePressed);
        zoomPane.setMouseReleasedListener(this::onMouseReleased);
        getChildren().add(zoomPane);
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private record Line(Point2D p1, Point2D p2) {}

    private enum DragMode
    {
        NONE, MOVE, ORIENTATION
    }
}
