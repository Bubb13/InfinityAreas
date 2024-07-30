
package com.github.bubb13.infinityareas.gui.editor.field.enums;

import com.github.bubb13.infinityareas.gui.editor.field.implementation.MappedShortEnum;

public enum ShortYesNoType implements MappedShortEnum
{
    NO(0,"No"),
    YES(1,"Yes");

    //////////////////////////
    // Public Static Fields //
    //////////////////////////

    public static final ShortYesNoType[] VALUES = ShortYesNoType.values();

    ////////////////////
    // Private Fields //
    ////////////////////

    private final short value;
    private final String label;

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
