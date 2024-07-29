
package com.github.bubb13.infinityareas.gui.editor.connector;

import java.util.function.Consumer;

public interface Connector<FieldEnumType extends Enum<?>>
{
    short getShort(final FieldEnumType field);
    void setShort(final FieldEnumType field, final short value);
    int getInt(final FieldEnumType field);
    void setInt(final FieldEnumType field, final int value);
    String getString(final FieldEnumType field);
    void setString(final FieldEnumType field, final String name);
    void addShortListener(final FieldEnumType field, final Consumer<Short> consumer);
    void addIntListener(final FieldEnumType field, final Consumer<Integer> consumer);
    void addStringListener(final FieldEnumType field, final Consumer<String> consumer);
    void removeListener(final FieldEnumType field, final Consumer<?> consumer);
}
