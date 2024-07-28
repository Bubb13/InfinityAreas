
package com.github.bubb13.infinityareas.gui.editor.connector;

public interface Connector<EnumType extends Enum<?>>
{
    short getShort(final EnumType field);
    void setShort(final EnumType field, final short value);
    int getInt(final EnumType field);
    void setInt(final EnumType field, final int value);
    String getString(final EnumType field);
    void setString(final EnumType field, final String name);
}
