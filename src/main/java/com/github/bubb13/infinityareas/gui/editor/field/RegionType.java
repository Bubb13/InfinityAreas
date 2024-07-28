
package com.github.bubb13.infinityareas.gui.editor.field;

import com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation.textfield.MappedShortEnum;

public enum RegionType implements MappedShortEnum
{
    PROXIMITY_TRIGGER(0, "Proximity trigger"),
    INFO_POINT(1, "Info point"),
    TRAVEL_REGION(2, "Travel region");

    public final short value;
    public final String label;

    //////////////////
    // Constructors //
    //////////////////

    RegionType(final int value, final String label)
    {
        this.value = (short)value;
        this.label = label;
    }

    ///////////////////////////
    // Public Static Methods //
    ///////////////////////////

    public static RegionType fromValue(final int value)
    {
        return switch (value)
        {
            case 0 -> PROXIMITY_TRIGGER;
            case 1 -> INFO_POINT;
            case 2 -> TRAVEL_REGION;
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
