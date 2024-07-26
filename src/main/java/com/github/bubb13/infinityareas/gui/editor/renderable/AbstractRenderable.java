
package com.github.bubb13.infinityareas.gui.editor.renderable;

import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;

public abstract class AbstractRenderable implements Renderable
{
    @Override
    public boolean contains(final Point2D point)
    {
        return getCorners().contains(point);
    }

    @Override public void onClicked(final MouseEvent mouseEvent) {}
    @Override public void onSelected() {}
    @Override public void onUnselected() {}
    @Override public void delete() {}
    @Override public void onDragged(final MouseEvent event) {}
    @Override public boolean listensToZoomFactorChanges() { return false; }
    @Override public void onZoomFactorChanged(final double zoomFactor) {}
}
