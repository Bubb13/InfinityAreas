
package com.github.bubb13.infinityareas.gui.editor.field;

import com.github.bubb13.infinityareas.game.resource.KeyFile;
import com.github.bubb13.infinityareas.gui.editor.field.enums.RegionFields;
import com.github.bubb13.infinityareas.gui.editor.field.enums.RegionFlags;
import com.github.bubb13.infinityareas.gui.editor.field.enums.RegionType;
import com.github.bubb13.infinityareas.gui.editor.field.enums.ShortYesNoType;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.MappedIntBitFieldOptions;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.MappedIntEnum;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.MappedShortEnum;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.MappedShortFieldOptions;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.NumericFieldOptions;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.ResrefFieldOptions;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.TextFieldOptions;

import java.util.function.Function;

public final class StandardStructureDefinitions
{
    /////////////////
    // Definitions //
    /////////////////

    public static final StructureDefinition<RegionFields> REGION = new StructureDefinition<>(
        limitedText(RegionFields.NAME, "Name", 32),
        mappedShort(RegionFields.TYPE, "Type", RegionType::fromValue, RegionType.VALUES),
        unsignedInt(RegionFields.TRIGGER_VALUE, "Trigger Value"),
        unsignedInt(RegionFields.CURSOR_INDEX, "Cursor Index"),
        resref(RegionFields.DESTINATION_AREA, "Destination Area", KeyFile.NumericResourceType.ARE),
        // TODO
        limitedText(RegionFields.DESTINATION_ENTRANCE_NAME, "Destination Entrance Name", 32),
        intBitfield(RegionFields.FLAGS, "Flags", RegionFlags::fromValue, RegionFlags.VALUES),
        unsignedInt(RegionFields.INFO_TEXT, "Info Strref"),
        unsignedShort(RegionFields.TRAP_DETECTION_DIFFICULTY, "Trap Detection Difficulty"),
        unsignedShort(RegionFields.TRAP_DISARM_DIFFICULTY, "Trap Disarm Difficulty"),
        mappedShortNoYes(RegionFields.TRAPPED, "Trapped"),
        mappedShortNoYes(RegionFields.DETECTED, "Detected"),
        unsignedShort(RegionFields.TRAP_LAUNCH_X, "Trap Launch X"),
        unsignedShort(RegionFields.TRAP_LAUNCH_Y, "Trap Launch Y"),
        resref(RegionFields.KEY, "Key", KeyFile.NumericResourceType.ITM),
        resref(RegionFields.SCRIPT, "Script", KeyFile.NumericResourceType.BCS),
        unsignedShort(RegionFields.ACTIVATION_X, "Activation X"),
        unsignedShort(RegionFields.ACTIVATION_Y, "Activation Y")
    );

    ///////////////
    // Shortcuts //
    ///////////////

    //------//
    // Text //
    //------//

    public static <T extends Enum<?>> FieldDefinition<T> limitedText(
        final T fieldEnum, final String label, final int limit)
    {
        return new FieldDefinition<>(fieldEnum, FieldType.TEXT, new TextFieldOptions()
            .label(label)
            .characterLimit(limit));
    }

    //--------//
    // Shorts //
    //--------//

    public static <T extends Enum<?>> FieldDefinition<T> signedShort(final T fieldEnum, final String label)
    {
        return new FieldDefinition<>(fieldEnum, FieldType.SIGNED_SHORT, new NumericFieldOptions()
            .label(label));
    }

    public static <T extends Enum<?>> FieldDefinition<T> unsignedShort(final T fieldEnum, final String label)
    {
        return new FieldDefinition<>(fieldEnum, FieldType.UNSIGNED_SHORT, new NumericFieldOptions()
            .label(label));
    }

    public static <T extends Enum<?>, MappedEnumType extends MappedShortEnum> FieldDefinition<T>
    mappedShort(
        final T fieldEnum, final String label,
        final Function<Short, MappedEnumType> enumFromValueFunc, final MappedEnumType[] enumTypes)
    {
        return new FieldDefinition<>(fieldEnum, FieldType.MAPPED_SHORT, new MappedShortFieldOptions<MappedEnumType>()
            .label(label)
            .enumFromValueFunction(enumFromValueFunc)
            .enumValues(enumTypes));
    }

    public static <T extends Enum<?>> FieldDefinition<T> mappedShortNoYes(final T fieldEnum, final String label)
    {
        return new FieldDefinition<>(fieldEnum, FieldType.MAPPED_SHORT, new MappedShortFieldOptions<>()
            .label(label)
            .enumFromValueFunction(ShortYesNoType::fromValue)
            .enumValues(ShortYesNoType.VALUES));
    }

    //------//
    // Ints //
    //------//

    public static <T extends Enum<?>> FieldDefinition<T> signedInt(final T fieldEnum, final String label)
    {
        return new FieldDefinition<>(fieldEnum, FieldType.SIGNED_INT, new NumericFieldOptions()
            .label(label));
    }

    public static <T extends Enum<?>> FieldDefinition<T> unsignedInt(final T fieldEnum, final String label)
    {
        return new FieldDefinition<>(fieldEnum, FieldType.UNSIGNED_INT, new NumericFieldOptions()
            .label(label));
    }

    public static <T extends Enum<?>, MappedEnumType extends MappedIntEnum> FieldDefinition<T>
    intBitfield(
        final T fieldEnum, final String label,
        final Function<Integer, MappedEnumType> enumFromValueFunc, final MappedEnumType[] enumTypes)
    {
        return new FieldDefinition<>(fieldEnum, FieldType.INT_BITFIELD, new MappedIntBitFieldOptions<MappedEnumType>()
            .label(label)
            .enumFromValueFunction(enumFromValueFunc)
            .enumValues(enumTypes));
    }

    //-----------------------//
    // Infinity Engine Types //
    //-----------------------//

    public static <T extends Enum<?>> FieldDefinition<T> resref(
        final T fieldEnum, final String label, final KeyFile.NumericResourceType type)
    {
        return new FieldDefinition<>(fieldEnum, FieldType.RESREF, new ResrefFieldOptions()
            .label(label)
            .resourceTypes(type));
    }

    //////////////////////////
    // Private Constructors //
    //////////////////////////

    private StandardStructureDefinitions() {}
}
