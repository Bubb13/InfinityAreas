
package com.github.bubb13.infinityareas.gui.editor.editmode.areapane;

import com.github.bubb13.infinityareas.gui.editor.Editor;
import com.github.bubb13.infinityareas.gui.editor.editmode.AbstractEditMode;
import com.github.bubb13.infinityareas.gui.editor.renderable.Renderable;
import com.github.bubb13.infinityareas.gui.editor.renderable.RenderableActorOrientationHandle;
import com.github.bubb13.infinityareas.gui.editor.renderable.RenderablePolygon;
import com.github.bubb13.infinityareas.gui.editor.renderable.RenderableVertex;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

public class AreaPaneNormalEditMode extends AbstractEditMode
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final Editor editor;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public AreaPaneNormalEditMode(Editor editor)
    {
        this.editor = editor;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    @Override
    public boolean shouldCaptureObjectPress(final MouseEvent event, final Renderable renderable)
    {
        return renderable instanceof RenderableVertex
            || renderable instanceof RenderableActorOrientationHandle
            || renderable instanceof RenderablePolygon<?>;
    }

    @Override
    public boolean shouldCaptureObjectDrag(final MouseEvent event, final Renderable renderable)
    {
        return renderable instanceof RenderableVertex
            || renderable instanceof RenderableActorOrientationHandle;
    }

    @Override
    public void onObjectDragged(final MouseEvent event, final Renderable renderable)
    {
        renderable.onDragged(event);
    }

    @Override
    public void onKeyPressed(final KeyEvent event)
    {

    }
}
