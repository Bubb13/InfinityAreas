
package com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation;

import com.github.bubb13.infinityareas.gui.editor.connector.Connector;

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
}
