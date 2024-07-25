
package com.github.bubb13.infinityareas.gui.editor.renderable;

import com.github.bubb13.infinityareas.game.resource.Area;
import com.github.bubb13.infinityareas.gui.editor.Editor;
import com.github.bubb13.infinityareas.misc.DoubleCorners;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

public class RenderableActor extends AbstractRenderable
{
    ///////////////////////////
    // Private Static Fields //
    ///////////////////////////

    public final static double RADIUS = 15;
    public final static double LINE_WIDTH = 2;
    private final static double SOURCE_WIDTH = RADIUS + LINE_WIDTH / 2;

    ////////////////////
    // Private Fields //
    ////////////////////

    private final Editor editor;
    private final Area.Actor actor;
    private final RenderableActorOrientationHandle renderableActorOrientationHandle;
    private final DoubleCorners corners = new DoubleCorners();

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public RenderableActor(final Editor editor, final Area.Actor actor)
    {
        this.editor = editor;
        this.actor = actor;
        recalculateCorners();
        renderableActorOrientationHandle = new RenderableActorOrientationHandle(editor, this);
        renderableActorOrientationHandle.recalculateLine();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public Area.Actor getActor()
    {
        return actor;
    }

    @Override
    public void onRender(final GraphicsContext canvasContext)
    {
        final Point2D actorPoint1 = editor.sourceToAbsoluteCanvasDoublePosition(
            actor.getX() - RADIUS, actor.getY() - RADIUS
        );
        final double actorPoint1X = actorPoint1.getX();
        final double actorPoint1Y = actorPoint1.getY();

        final Point2D actorPoint2 = editor.sourceToAbsoluteCanvasDoublePosition(
            actor.getX() + RADIUS, actor.getY() + RADIUS
        );
        final double actorPoint2X = actorPoint2.getX();
        final double actorPoint2Y = actorPoint2.getY();

        canvasContext.setStroke(Color.WHITE);
        canvasContext.setLineWidth(LINE_WIDTH * editor.getZoomFactor());
        canvasContext.strokeOval(actorPoint1X, actorPoint1Y,
            actorPoint2X - actorPoint1X,
            actorPoint2Y - actorPoint1Y);
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
    public void onClicked(final MouseEvent mouseEvent)
    {

    }

    @Override
    public void onDragged(final MouseEvent event)
    {

    }

    @Override
    public void onSelected()
    {

    }

    @Override
    public void onUnselected()
    {

    }

    @Override
    public void delete()
    {

    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void recalculateCorners()
    {
        corners.setTopLeftX(actor.getX() - SOURCE_WIDTH);
        corners.setTopLeftY(actor.getY() - SOURCE_WIDTH);
        corners.setBottomRightExclusiveX(actor.getX() + SOURCE_WIDTH);
        corners.setBottomRightExclusiveY(actor.getY() + SOURCE_WIDTH);
        editor.addRenderable(this);
    }
}
