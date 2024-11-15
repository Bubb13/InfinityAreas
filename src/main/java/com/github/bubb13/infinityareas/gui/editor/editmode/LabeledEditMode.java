
package com.github.bubb13.infinityareas.gui.editor.editmode;

import com.github.bubb13.infinityareas.gui.editor.Editor;
import com.github.bubb13.infinityareas.misc.undoredo.IUndoHandle;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class LabeledEditMode extends AbstractEditMode
{
    ///////////////////////////
    // Private Static Fields //
    ///////////////////////////

    private static final Font FONT = new Font("Arial", 28);

    //////////////////////
    // Protected Fields //
    //////////////////////

    protected final Editor editor;

    ////////////////////
    // Private Fields //
    ////////////////////

    private final String label;
    private final double textHeight;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public LabeledEditMode(final Editor editor, final String label)
    {
        this.editor = editor;
        this.label = label;

        final Text tempText = new Text(label);
        tempText.setFont(FONT);
        textHeight = tempText.getBoundsInLocal().getHeight();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    @Override
    public void onModeStart(final IUndoHandle ownedUndo)
    {
        super.onModeStart(ownedUndo);
        editor.requestDraw();
    }

    @Override
    public void onModeResume()
    {
        editor.requestDraw();
    }

    @Override
    public void onModeSuspend()
    {
        editor.requestDraw();
    }

    @Override
    public void onModeExit()
    {
        super.onModeExit();
        editor.requestDraw();
    }

    @Override
    public void onDraw(final GraphicsContext canvasContext)
    {
        drawModeText(canvasContext, label);
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void drawModeText(final GraphicsContext context, final String text)
    {
        context.setFill(Color.WHITE);
        context.setFont(FONT);
        context.setStroke(Color.BLACK);
        context.setLineWidth(1);

        context.fillText(text, 10, textHeight);
        context.strokeText(text, 10, textHeight);
    }
}
