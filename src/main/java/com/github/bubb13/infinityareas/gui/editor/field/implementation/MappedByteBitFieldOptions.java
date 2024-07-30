
package com.github.bubb13.infinityareas.gui.editor.field.implementation;

import java.util.function.Function;

public class MappedByteBitFieldOptions<MappedEnumType extends MappedByteEnum>
    extends AbstractFieldOptions<MappedByteBitFieldOptions<MappedEnumType>>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private MappedEnumType[] enumValues;
    private Function<Byte, MappedEnumType> enumFromValueFunc;
    private int numBitsPerColumn = 8;
    private boolean showUnmappedBits = false;

    ////////////////////
    // Public Methods //
    ////////////////////

    public MappedEnumType[] getEnumValues()
    {
        return enumValues;
    }

    public MappedByteBitFieldOptions<MappedEnumType> enumValues(final MappedEnumType[] enumTypes)
    {
        this.enumValues = enumTypes;
        return this;
    }

    public MappedEnumType getEnumFromValue(final byte value)
    {
        return enumFromValueFunc.apply(value);
    }

    public MappedByteBitFieldOptions<MappedEnumType> enumFromValueFunction(
        final Function<Byte, MappedEnumType> enumFromValueFunc)
    {
        this.enumFromValueFunc = enumFromValueFunc;
        return this;
    }

    public int getNumBitsPerColumn()
    {
        return numBitsPerColumn;
    }

    public MappedByteBitFieldOptions<MappedEnumType> numBitsPerColumn(final int numBitsPerColumn)
    {
        this.numBitsPerColumn = numBitsPerColumn;
        return this;
    }

    public boolean getShowUnmappedBits()
    {
        return showUnmappedBits;
    }

    public MappedByteBitFieldOptions<MappedEnumType> showUnmappedBits(final boolean showUnmappedBits)
    {
        this.showUnmappedBits = showUnmappedBits;
        return this;
    }
}
