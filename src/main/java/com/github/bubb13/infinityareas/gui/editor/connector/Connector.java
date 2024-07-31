
package com.github.bubb13.infinityareas.gui.editor.connector;

import java.util.function.BiConsumer;

public interface Connector<FieldEnumType extends Enum<?>>
{
    byte getByte(final FieldEnumType field);
    void setByte(final FieldEnumType field, final byte newValue);
    short getShort(final FieldEnumType field);
    void setShort(final FieldEnumType field, final short newValue);
    int getInt(final FieldEnumType field);
    void setInt(final FieldEnumType field, final int newValue);
    String getString(final FieldEnumType field);
    void setString(final FieldEnumType field, final String name);
    void addByteListener(final FieldEnumType field, final BiConsumer<Byte, Byte> consumer);
    void addShortListener(final FieldEnumType field, final BiConsumer<Short, Short> consumer);
    void addIntListener(final FieldEnumType field, final BiConsumer<Integer, Integer> consumer);
    void addStringListener(final FieldEnumType field, final BiConsumer<String, String> consumer);
    void removeListener(final FieldEnumType field, final BiConsumer<?, ?> consumer);
}
