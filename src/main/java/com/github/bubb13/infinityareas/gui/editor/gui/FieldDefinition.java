
package com.github.bubb13.infinityareas.gui.editor.gui;

import com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation.AbstractFieldOptions;

public class FieldDefinition<FieldsEnum extends Enum<?>>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final FieldsEnum fieldEnum;
    private final FieldType fieldType;
    private final AbstractFieldOptions<?> options;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public FieldDefinition(final FieldsEnum fieldEnum, final FieldType fieldType, final AbstractFieldOptions<?> options)
    {
        this.fieldEnum = fieldEnum;
        this.fieldType = fieldType;
        this.options = options;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public FieldsEnum getFieldEnum()
    {
        return fieldEnum;
    }

    public FieldType getFieldType()
    {
        return fieldType;
    }

    public AbstractFieldOptions<?> getOptions()
    {
        return options;
    }
}
