
package com.github.bubb13.infinityareas.gui.editor.gui;

import com.github.bubb13.infinityareas.gui.editor.field.RegionFields;
import com.github.bubb13.infinityareas.gui.editor.field.RegionType;
import com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation.textfield.FieldOptions;
import com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation.textfield.MappedShortFieldOptions;
import com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation.textfield.TextFieldOptions;
import com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation.textfield.UnmappedFieldOptions;

public final class StandardStructureDefinitions
{
    public static final StructureDefinition<RegionFields> REGION = new StructureDefinition<>(

        new FieldDefinition<>(RegionFields.NAME, FieldType.TEXT, new TextFieldOptions()
            .label("Name")
            .characterLimit(32)),

        new FieldDefinition<>(RegionFields.TYPE, FieldType.MAPPED_SHORT, new MappedShortFieldOptions<>()
            .label("Type")
            .enumFromValueFunction(RegionType::fromValue)
            .enumValues(RegionType.values())),

        new FieldDefinition<>(RegionFields.BOUNDING_BOX_LEFT, FieldType.SIGNED_SHORT, new FieldOptions()
            .label("Bounding Box Left")),

        new FieldDefinition<>(RegionFields.BOUNDING_BOX_TOP, FieldType.SIGNED_SHORT, new FieldOptions()
            .label("Bounding Box Top")),

        new FieldDefinition<>(RegionFields.BOUNDING_BOX_RIGHT, FieldType.SIGNED_SHORT, new FieldOptions()
            .label("Bounding Box Right")),

        new FieldDefinition<>(RegionFields.BOUNDING_BOX_BOTTOM, FieldType.SIGNED_SHORT, new FieldOptions()
            .label("Bounding Box Bottom")),

        new FieldDefinition<>(RegionFields.VERTICES_COUNT, FieldType.UNSIGNED_SHORT, new FieldOptions()
            .label("# Vertices")),

        new FieldDefinition<>(RegionFields.FIRST_VERTEX_INDEX, FieldType.UNSIGNED_INT, new FieldOptions()
            .label("First Vertex Index")),

        new FieldDefinition<>(RegionFields.TRIGGER_VALUE, FieldType.UNSIGNED_INT, new FieldOptions()
            .label("Trigger Value")),

        new FieldDefinition<>(RegionFields.CURSOR_INDEX, FieldType.UNSIGNED_INT, new FieldOptions()
            .label("Cursor Index")),

        new FieldDefinition<>(RegionFields.DESTINATION_AREA, FieldType.RESREF, new FieldOptions()
            .label("Destination Area")),

        // TODO
        new FieldDefinition<>(RegionFields.DESTINATION_ENTRANCE_NAME, FieldType.RESREF, new TextFieldOptions()
            .label("Destination Entrance Name")
            .characterLimit(32)),

        new FieldDefinition<>(RegionFields.FLAGS, FieldType.UNSIGNED_INT, new FieldOptions()
            .label("Flags")),

        new FieldDefinition<>(RegionFields.INFO_TEXT, FieldType.UNSIGNED_INT, new FieldOptions()
            .label("Info Strref")),

        new FieldDefinition<>(RegionFields.TRAP_DETECTION_DIFFICULTY, FieldType.UNSIGNED_SHORT, new FieldOptions()
            .label("Trap Detection Difficulty")),

        new FieldDefinition<>(RegionFields.TRAP_DISARM_DIFFICULTY, FieldType.UNSIGNED_SHORT, new FieldOptions()
            .label("Trap Disarm Difficulty")),

        new FieldDefinition<>(RegionFields.TRAPPED, FieldType.UNSIGNED_SHORT, new FieldOptions()
            .label("Trapped")),

        new FieldDefinition<>(RegionFields.DETECTED, FieldType.UNSIGNED_SHORT, new FieldOptions()
            .label("Detected")),

        new FieldDefinition<>(RegionFields.TRAP_LAUNCH_X, FieldType.UNSIGNED_SHORT, new FieldOptions()
            .label("Trap Launch X")),

        new FieldDefinition<>(RegionFields.TRAP_LAUNCH_Y, FieldType.UNSIGNED_SHORT, new FieldOptions()
            .label("Trap Launch Y")),

        new FieldDefinition<>(RegionFields.KEY, FieldType.RESREF, new FieldOptions()
            .label("Key")),

        new FieldDefinition<>(RegionFields.SCRIPT, FieldType.RESREF, new FieldOptions()
            .label("Script")),

        new FieldDefinition<>(RegionFields.ACTIVATION_X, FieldType.UNSIGNED_SHORT, new FieldOptions()
            .label("Activation X")),

        new FieldDefinition<>(RegionFields.ACTIVATION_Y, FieldType.UNSIGNED_SHORT, new FieldOptions()
            .label("Activation Y")),

        new FieldDefinition<>(RegionFields.UNKNOWN, FieldType.UNMAPPED, new UnmappedFieldOptions()
            .label("Unknown")
            .size(0x3C))
    );

    //////////////////////////
    // Private Constructors //
    //////////////////////////

    private StandardStructureDefinitions() {}
}
