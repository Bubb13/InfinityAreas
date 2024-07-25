
package com.github.bubb13.infinityareas.gui.editor.editmode;

import com.github.bubb13.infinityareas.gui.editor.Editor;
import com.github.bubb13.infinityareas.gui.editor.EditorCommons;
import com.github.bubb13.infinityareas.gui.editor.renderable.Renderable;
import com.github.bubb13.infinityareas.gui.editor.renderable.RenderableVertex;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

public class NormalEditMode extends AbstractEditMode
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final Editor editor;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public NormalEditMode(Editor editor)
    {
        this.editor = editor;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    @Override
    public boolean shouldCaptureObjectPress(final MouseEvent event, final Renderable renderable)
    {
        return renderable instanceof RenderableVertex;
    }

    @Override
    public boolean shouldCaptureObjectDrag(final MouseEvent event, final Renderable renderable)
    {
        return renderable instanceof RenderableVertex;
    }

    @Override
    public void onObjectDragged(final MouseEvent event, final Renderable renderable)
    {
        renderable.onDragged(event);
    }

    @Override
    public void onKeyPressed(final KeyEvent event)
    {
        final KeyCode key = event.getCode();

        switch (key)
        {
            case ESCAPE ->
            {
                if (editor.selectedCount() > 0)
                {
                    event.consume();
                    editor.unselectAll();
                }
            }
            case B ->
            {
                event.consume();
                EditorCommons.onBisectLine(editor);
            }
            case D ->
            {
                event.consume();
                editor.enterEditMode(DrawPolygonEditMode.class);
            }
            case Q ->
            {
                event.consume();
                editor.enterEditMode(QuickSelectEditMode.class);
            }
            case DELETE ->
            {
                event.consume();
                EditorCommons.deleteSelected(editor);
            }
        }
    }
}
