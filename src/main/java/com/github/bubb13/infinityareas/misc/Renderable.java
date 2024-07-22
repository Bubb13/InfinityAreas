
package com.github.bubb13.infinityareas.misc;

import javafx.scene.input.MouseEvent;

public interface Renderable
{
    void render();
    Corners getCorners();
    boolean isEnabled();
    void clicked(final MouseEvent mouseEvent);
    void selected();
    void unselected();
    void delete();
}
