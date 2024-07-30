
package com.github.bubb13.infinityareas.gui.editor.field.implementation;

public class NumericFieldOptions extends AbstractFieldOptions<NumericFieldOptions>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private Long minValue;
    private Long maxValue;

    ////////////////////
    // Public Methods //
    ////////////////////

    public Long getMinValue()
    {
        return minValue;
    }

    public NumericFieldOptions minValue(final long minValue)
    {
        this.minValue = minValue;
        return this;
    }

    public Long getMaxValue()
    {
        return maxValue;
    }

    public NumericFieldOptions maxValue(final long maxValue)
    {
        this.maxValue = maxValue;
        return this;
    }
}
