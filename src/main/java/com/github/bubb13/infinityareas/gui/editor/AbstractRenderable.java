
package com.github.bubb13.infinityareas.gui.editor;

import javafx.scene.input.MouseEvent;

public abstract class AbstractRenderable implements Renderable
{
    @Override public void clicked(final MouseEvent mouseEvent) {}
    @Override public void selected() {}
    @Override public void unselected() {}
    @Override public void delete() {}
    @Override public void onDragged(MouseEvent event) {}
}
