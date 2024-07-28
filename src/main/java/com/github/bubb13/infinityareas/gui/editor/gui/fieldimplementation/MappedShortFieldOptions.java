
package com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation;

import java.util.function.Function;

public class MappedShortFieldOptions<MappedEnumType extends MappedShortEnum>
    extends AbstractFieldOptions<MappedShortFieldOptions<MappedEnumType>>
{
    private MappedEnumType[] enumValues;
    private Function<Short, MappedEnumType> enumFromValueFunc;

    public MappedEnumType[] getEnumValues()
    {
        return enumValues;
    }

    public MappedEnumType getEnumFromValue(final short value)
    {
        return enumFromValueFunc.apply(value);
    }

    public MappedShortFieldOptions<MappedEnumType> enumFromValueFunction(final Function<Short, MappedEnumType> enumFromValueFunc)
    {
        this.enumFromValueFunc = enumFromValueFunc;
        return this;
    }

    public MappedShortFieldOptions<MappedEnumType> enumValues(final MappedEnumType[] enumTypes)
    {
        this.enumValues = enumTypes;
        return this;
    }
}
