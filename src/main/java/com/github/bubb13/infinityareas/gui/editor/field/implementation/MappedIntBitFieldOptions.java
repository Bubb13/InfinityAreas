
package com.github.bubb13.infinityareas.gui.editor.field.implementation;

import java.util.function.Function;

public class MappedIntBitFieldOptions<MappedEnumType extends MappedIntEnum>
    extends AbstractFieldOptions<MappedIntBitFieldOptions<MappedEnumType>>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private MappedEnumType[] enumValues;
    private Function<Integer, MappedEnumType> enumFromValueFunc;
    private int numBitsPerColumn = 16;
    private boolean showUnmappedBits = false;

    ////////////////////
    // Public Methods //
    ////////////////////

    public MappedEnumType[] getEnumValues()
    {
        return enumValues;
    }

    public MappedIntBitFieldOptions<MappedEnumType> enumValues(final MappedEnumType[] enumTypes)
    {
        this.enumValues = enumTypes;
        return this;
    }

    public MappedEnumType getEnumFromValue(final int value)
    {
        return enumFromValueFunc.apply(value);
    }

    public MappedIntBitFieldOptions<MappedEnumType> enumFromValueFunction(
        final Function<Integer, MappedEnumType> enumFromValueFunc)
    {
        this.enumFromValueFunc = enumFromValueFunc;
        return this;
    }

    public int getNumBitsPerColumn()
    {
        return numBitsPerColumn;
    }

    public MappedIntBitFieldOptions<MappedEnumType> numBitsPerColumn(final int numBitsPerColumn)
    {
        this.numBitsPerColumn = numBitsPerColumn;
        return this;
    }

    public boolean getShowUnmappedBits()
    {
        return showUnmappedBits;
    }

    public MappedIntBitFieldOptions<MappedEnumType> showUnmappedBits(final boolean showUnmappedBits)
    {
        this.showUnmappedBits = showUnmappedBits;
        return this;
    }
}
