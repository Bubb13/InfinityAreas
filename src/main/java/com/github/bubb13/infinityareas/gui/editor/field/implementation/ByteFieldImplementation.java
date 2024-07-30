
package com.github.bubb13.infinityareas.gui.editor.field.implementation;

import com.github.bubb13.infinityareas.gui.editor.connector.Connector;

import java.util.function.Consumer;

public class ByteFieldImplementation<FieldEnumType extends Enum<?>> extends NumericFieldImplementation<FieldEnumType>
{
    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public ByteFieldImplementation(
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
        return connector.getByte(fieldEnum);
    }

    @Override
    protected void setConnectedValue(final long newValue)
    {
        connector.setByte(fieldEnum, (byte)newValue);
    }

    @Override
    protected void addConnectedValueChangedListener()
    {
        final Consumer<Byte> byteListener = this::onConnectedValueChanged;
        connectedValueChangedListener = byteListener;
        connector.addByteListener(fieldEnum, byteListener);
    }
}
