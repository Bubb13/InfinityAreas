
package com.github.bubb13.infinityareas.gui.editor.field;

public class StructureDefinition<FieldsEnum extends Enum<?>>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final FieldDefinition<FieldsEnum>[] fieldDefinitions;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public StructureDefinition(final FieldDefinition<FieldsEnum>... fieldDefinitions)
    {
        this.fieldDefinitions = fieldDefinitions;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public FieldDefinition<FieldsEnum>[] getFieldDefinitions()
    {
        return fieldDefinitions;
    }
}
