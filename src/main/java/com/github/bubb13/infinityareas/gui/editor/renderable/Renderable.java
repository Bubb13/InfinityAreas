
package com.github.bubb13.infinityareas.gui.editor.renderable;

import com.github.bubb13.infinityareas.misc.DoubleCorners;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;

public interface Renderable
{
    int sortWeight();
    void onRender(GraphicsContext canvasContext);
    DoubleCorners getCorners();
    boolean contains(Point2D point);
    boolean isEnabled();
    void onClicked(MouseEvent mouseEvent);
    void onDragged(MouseEvent event);
    void onSelected();
    void onUnselected();
    void delete();
    boolean listensToZoomFactorChanges();
    void onZoomFactorChanged(double zoomFactor);
}
