
package com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation;

public class UnmappedFieldOptions extends AbstractFieldOptions<UnmappedFieldOptions>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private int size;

    ////////////////////
    // Public Methods //
    ////////////////////

    public int getSize()
    {
        return size;
    }

    public UnmappedFieldOptions size(final int size)
    {
        this.size = size;
        return this;
    }
}