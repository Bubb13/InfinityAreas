
package com.github.bubb13.infinityareas.gui.editor.field;

import com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation.MappedShortEnum;

public enum RegionType implements MappedShortEnum
{
    PROXIMITY_TRIGGER(0, "Proximity trigger"),
    INFO_POINT(1, "Info point"),
    TRAVEL_REGION(2, "Travel region");

    //////////////////////////
    // Public Static Fields //
    //////////////////////////

    public static final RegionType[] VALUES = RegionType.values();

    ////////////////////
    // Private Fields //
    ////////////////////

    private final short value;
    private final String label;

    //////////////////////////////////
    // Package Private Constructors //
    //////////////////////////////////

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
