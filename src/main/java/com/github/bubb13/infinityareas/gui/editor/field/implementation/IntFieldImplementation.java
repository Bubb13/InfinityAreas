
package com.github.bubb13.infinityareas.gui.editor.field.implementation;

import com.github.bubb13.infinityareas.gui.editor.connector.Connector;

import java.util.function.Consumer;

public class IntFieldImplementation<FieldEnumType extends Enum<?>> extends NumericFieldImplementation<FieldEnumType>
{
    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public IntFieldImplementation(
        final FieldEnumType fieldEnum, final Connector<FieldEnumType> connector, final NumericFieldOptions options)
    {
        super(fieldEnum, connector, options);
    }

    ///////////////////////
    // Protected Methods //
    ///////////////////////

    @Override
    protected long getConnectedValue()
    {
        return connector.getInt(fieldEnum);
    }

    @Override
    protected void setConnectedValue(final long newValue)
    {
        connector.setInt(fieldEnum, (int)newValue);
    }

    @Override
    protected void addConnectedValueChangedListener()
    {
        final Consumer<Integer> intListener = this::onConnectedValueChanged;
        connectedValueChangedListener = intListener;
        connector.addIntListener(fieldEnum, intListener);
    }
}
