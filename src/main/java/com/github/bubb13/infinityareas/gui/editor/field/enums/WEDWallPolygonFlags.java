
package com.github.bubb13.infinityareas.gui.editor.field.enums;

import com.github.bubb13.infinityareas.gui.editor.field.implementation.MappedByteEnum;
import com.github.bubb13.infinityareas.util.MiscUtil;

public enum WEDWallPolygonFlags implements MappedByteEnum
{
    BIT0(0x1, "Shade wall"),
    BIT1(0x2, "Semi transparent"),
    BIT2(0x4, "Hovering wall"),
    BIT3(0x8, "Cover animations"),
    BIT7(0x80, "Is door");

    //////////////////////////
    // Public Static Fields //
    //////////////////////////

    public static final WEDWallPolygonFlags[] VALUES = WEDWallPolygonFlags.values();

    ////////////////////
    // Private Fields //
    ////////////////////

    private final byte value;
    private final String label;

    //////////////////////////////////
    // Package Private Constructors //
    //////////////////////////////////

    WEDWallPolygonFlags(final int value, final String label)
    {
        this.value = (byte)value;
        this.label = label;
    }

    ///////////////////////////
    // Public Static Methods //
    ///////////////////////////

    public static WEDWallPolygonFlags fromValue(final byte value)
    {
        return switch (MiscUtil.toUnsignedByte(value))
        {
            case 0x1 -> BIT0;
            case 0x2 -> BIT1;
            case 0x4 -> BIT2;
            case 0x8 -> BIT3;
            case 0x80 -> BIT7;
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
    public byte getValue()
    {
        return value;
    }
}
