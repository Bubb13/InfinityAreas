
package com.github.bubb13.infinityareas.gui.editor.field;

import com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation.MappedShortEnum;

public enum ShortYesNoType implements MappedShortEnum
{
    NO(0,"No"),
    YES(1,"Yes");

    public final short value;
    public final String label;

    //////////////////
    // Constructors //
    //////////////////

    ShortYesNoType(final int value, final String label)
    {
        this.value = (short)value;
        this.label = label;
    }

    ///////////////////////////
    // Public Static Methods //
    ///////////////////////////

    public static ShortYesNoType fromValue(final int value)
    {
        return switch (value)
        {
            case 0 -> NO;
            case 1 -> YES;
            default -> null;
        };
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    @Override
    public String getLabel()
    {
        return label;
    }

    @Override
    public short getValue()
    {
        return value;
    }
}
