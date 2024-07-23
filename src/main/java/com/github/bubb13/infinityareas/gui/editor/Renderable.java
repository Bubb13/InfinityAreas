
package com.github.bubb13.infinityareas.gui.editor;

import com.github.bubb13.infinityareas.misc.Corners;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;

public interface Renderable
{
    void render(GraphicsContext canvasContext);
    Corners getCorners();
    boolean isEnabled();
    void clicked(MouseEvent mouseEvent);
    void onDragged(MouseEvent event);
    void selected();
    void unselected();
    void delete();
}
