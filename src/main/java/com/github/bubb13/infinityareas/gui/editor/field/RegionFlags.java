
package com.github.bubb13.infinityareas.gui.editor.field;

import com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation.MappedIntEnum;

public enum RegionFlags implements MappedIntEnum
{
    KEY_REQUIRED(0x1, "Key required"),
    TRAP_RESETS(0x2, "Proximity trigger resets"),
    TRAVEL_PARTY_REQUIRED(0x4, "Travel region requires party"),
    TRAP_DETECTABLE(0x8, "Trap detectable"),
    TRAP_ACTIVATED_BY_ENEMIES(0x10, "Proximity trigger activated by enemies"),
    TUTORIAL_ONLY(0x20, "Tutorial only"),
    TRAP_ACTIVATED_BY_NPCS(0x40, "Proximity trigger activated by anyone"),
    NO_FEEDBACK_STRING(0x80, "Disable feedback string"),
    DEACTIVATED(0x100, "Deactivated"),
    TRAVEL_REGION_PARTY_ONLY(0x200, "Travel region only allows party"),
    USE_ACTIVATION_POINT(0x400, "Use activation point"),
    DOOR_CLOSED(0x800, "Travel region has door");

    //////////////////////////
    // Public Static Fields //
    //////////////////////////

    public static final RegionFlags[] VALUES = RegionFlags.values();

    ////////////////////
    // Private Fields //
    ////////////////////

    private final short value;
    private final String label;

    //////////////////////////////////
    // Package Private Constructors //
    //////////////////////////////////

    RegionFlags(final int value, final String label)
    {
        this.value = (short)value;
        this.label = label;
    }

    ///////////////////////////
    // Public Static Methods //
    ///////////////////////////

    public static RegionFlags fromValue(final int value)
    {
        return switch (value)
        {
            case 0x1 -> KEY_REQUIRED;
            case 0x2 -> TRAP_RESETS;
            case 0x4 -> TRAVEL_PARTY_REQUIRED;
            case 0x8 -> TRAP_DETECTABLE;
            case 0x10 -> TRAP_ACTIVATED_BY_ENEMIES;
            case 0x20 -> TUTORIAL_ONLY;
            case 0x40 -> TRAP_ACTIVATED_BY_NPCS;
            case 0x80 -> NO_FEEDBACK_STRING;
            case 0x100 -> DEACTIVATED;
            case 0x200 -> TRAVEL_REGION_PARTY_ONLY;
            case 0x400 -> USE_ACTIVATION_POINT;
            case 0x800 -> DOOR_CLOSED;
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
    public int getValue()
    {
        return value;
    }
}
