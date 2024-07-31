
package com.github.bubb13.infinityareas.gui.editor.connector;

import com.github.bubb13.infinityareas.misc.OrderedInstanceSet;

import java.util.function.BiConsumer;

public abstract class AbstractConnector<FieldEnumType extends Enum<?>> implements Connector<FieldEnumType>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final OrderedInstanceSet<BiConsumer<?, ?>>[] listeners;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public AbstractConnector(final FieldEnumType[] fieldEnumValues)
    {
        final int numValues = fieldEnumValues.length;
        //noinspection unchecked
        listeners = (OrderedInstanceSet<BiConsumer<?, ?>>[])new OrderedInstanceSet[numValues];
        for (int i = 0; i < numValues; ++i)
        {
            listeners[i] = new OrderedInstanceSet<>();
        }
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void runByteListeners(final FieldEnumType field, final byte oldValue, final byte newValue)
    {
        for (final BiConsumer<?, ?> uncastListener : listeners[field.ordinal()])
        {
            @SuppressWarnings("unchecked")
            final BiConsumer<Byte, Byte> listener = (BiConsumer<Byte, Byte>)uncastListener;
            listener.accept(oldValue, newValue);
        }
    }

    public void runShortListeners(final FieldEnumType field, final short oldValue, final short newValue)
    {
        for (final BiConsumer<?, ?> uncastListener : listeners[field.ordinal()])
        {
            @SuppressWarnings("unchecked")
            final BiConsumer<Short, Short> listener = (BiConsumer<Short, Short>)uncastListener;
            listener.accept(oldValue, newValue);
        }
    }

    public void runIntListeners(final FieldEnumType field, final int oldValue, final int newValue)
    {
        for (final BiConsumer<?, ?> uncastListener : listeners[field.ordinal()])
        {
            @SuppressWarnings("unchecked")
            final BiConsumer<Integer, Integer> listener = (BiConsumer<Integer, Integer>)uncastListener;
            listener.accept(oldValue, newValue);
        }
    }

    public void runStringListeners(final FieldEnumType field, final String oldValue, final String newValue)
    {
        for (final BiConsumer<?, ?> uncastListener : listeners[field.ordinal()])
        {
            @SuppressWarnings("unchecked")
            final BiConsumer<String, String> listener = (BiConsumer<String, String>)uncastListener;
            listener.accept(oldValue, newValue);
        }
    }

    @Override
    public void addByteListener(final FieldEnumType field, final BiConsumer<Byte, Byte> consumer)
    {
        listeners[field.ordinal()].addTail(consumer);
    }

    @Override
    public void addShortListener(final FieldEnumType field, final BiConsumer<Short, Short> consumer)
    {
        listeners[field.ordinal()].addTail(consumer);
    }

    @Override
    public void addIntListener(final FieldEnumType field, final BiConsumer<Integer, Integer> consumer)
    {
        listeners[field.ordinal()].addTail(consumer);
    }

    @Override
    public void addStringListener(final FieldEnumType field, final BiConsumer<String, String> consumer)
    {
        listeners[field.ordinal()].addTail(consumer);
    }

    @Override
    public void removeListener(final FieldEnumType field, final BiConsumer<?, ?> consumer)
    {
        listeners[field.ordinal()].remove(consumer);
    }
}
