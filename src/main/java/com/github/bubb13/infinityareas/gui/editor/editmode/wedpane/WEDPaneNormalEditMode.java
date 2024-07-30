
package com.github.bubb13.infinityareas.gui.editor.editmode.wedpane;

import com.github.bubb13.infinityareas.gui.editor.Editor;
import com.github.bubb13.infinityareas.gui.editor.EditorCommons;
import com.github.bubb13.infinityareas.gui.editor.editmode.AbstractEditMode;
import com.github.bubb13.infinityareas.gui.editor.editmode.DrawPolygonEditMode;
import com.github.bubb13.infinityareas.gui.editor.editmode.QuickSelectEditMode;
import com.github.bubb13.infinityareas.gui.editor.renderable.AbstractRenderable;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

public class WEDPaneNormalEditMode extends AbstractEditMode
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final Editor editor;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public WEDPaneNormalEditMode(Editor editor)
    {
        this.editor = editor;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    @Override
    public boolean shouldCaptureObjectPress(final MouseEvent event, final AbstractRenderable renderable)
    {
        return true;
    }

    @Override
    public boolean shouldCaptureObjectDrag(final MouseEvent event, final AbstractRenderable renderable)
    {
        return true;
    }

    @Override
    public void onObjectDragged(final MouseEvent event, final AbstractRenderable renderable)
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
