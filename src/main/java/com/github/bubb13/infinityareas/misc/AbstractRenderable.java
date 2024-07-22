
package com.github.bubb13.infinityareas.misc;

import javafx.scene.input.MouseEvent;

public abstract class AbstractRenderable implements Renderable
{
    @Override public void clicked(final MouseEvent mouseEvent) {}
    @Override public void selected() {}
    @Override public void unselected() {}
    @Override public void delete() {}
}
