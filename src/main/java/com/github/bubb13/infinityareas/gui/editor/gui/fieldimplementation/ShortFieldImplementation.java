
package com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation;

import com.github.bubb13.infinityareas.gui.editor.connector.Connector;

import java.util.function.Consumer;

public class ShortFieldImplementation<FieldEnumType extends Enum<?>> extends NumericFieldImplementation<FieldEnumType>
{
    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public ShortFieldImplementation(
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
        return connector.getShort(fieldEnum);
    }

    @Override
    protected void setConnectedValue(final long newValue)
    {
        connector.setShort(fieldEnum, (short)newValue);
    }

    @Override
    protected void addConnectedValueChangedListener()
    {
        final Consumer<Short> shortListener = this::onConnectedValueChanged;
        connectedValueChangedListener = shortListener;
        connector.addShortListener(fieldEnum, shortListener);
    }
}