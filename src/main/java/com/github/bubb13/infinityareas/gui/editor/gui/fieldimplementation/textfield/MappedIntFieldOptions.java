
package com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation.textfield;

import java.util.function.Function;

public class MappedIntFieldOptions<MappedEnumType extends MappedIntEnum>
    extends AbstractFieldOptions<MappedIntFieldOptions<MappedEnumType>>
{
    private MappedEnumType[] enumValues;
    private Function<Integer, MappedEnumType> enumFromValueFunc;

    public MappedEnumType[] getEnumValues()
    {
        return enumValues;
    }

    public MappedEnumType getEnumFromValue(final int value)
    {
        return enumFromValueFunc.apply(value);
    }

    public MappedIntFieldOptions<MappedEnumType> enumFromValueFunction(final Function<Integer, MappedEnumType> enumFromValueFunc)
    {
        this.enumFromValueFunc = enumFromValueFunc;
        return this;
    }

    public MappedIntFieldOptions<MappedEnumType> enumValues(final MappedEnumType[] enumTypes)
    {
        this.enumValues = enumTypes;
        return this;
    }
}
