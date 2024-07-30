
package com.github.bubb13.infinityareas.gui.editor.connector;

import com.github.bubb13.infinityareas.misc.OrderedInstanceSet;

import java.util.function.Consumer;

public abstract class AbstractConnector<FieldEnumType extends Enum<?>> implements Connector<FieldEnumType>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final OrderedInstanceSet<Consumer<?>>[] listeners;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public AbstractConnector(final FieldEnumType[] fieldEnumValues)
    {
        final int numValues = fieldEnumValues.length;
        //noinspection unchecked
        listeners = (OrderedInstanceSet<Consumer<?>>[])new OrderedInstanceSet[numValues];
        for (int i = 0; i < numValues; ++i)
        {
            listeners[i] = new OrderedInstanceSet<>();
        }
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void runByteListeners(final FieldEnumType field, final byte value)
    {
        for (final Consumer<?> uncastListener : listeners[field.ordinal()])
        {
            @SuppressWarnings("unchecked")
            final Consumer<Byte> listener = (Consumer<Byte>)uncastListener;
            listener.accept(value);
        }
    }

    public void runShortListeners(final FieldEnumType field, final short value)
    {
        for (final Consumer<?> uncastListener : listeners[field.ordinal()])
        {
            @SuppressWarnings("unchecked")
            final Consumer<Short> listener = (Consumer<Short>)uncastListener;
            listener.accept(value);
        }
    }

    public void runIntListeners(final FieldEnumType field, final int value)
    {
        for (final Consumer<?> uncastListener : listeners[field.ordinal()])
        {
            @SuppressWarnings("unchecked")
            final Consumer<Integer> listener = (Consumer<Integer>)uncastListener;
            listener.accept(value);
        }
    }

    public void runStringListeners(final FieldEnumType field, final String value)
    {
        for (final Consumer<?> uncastListener : listeners[field.ordinal()])
        {
            @SuppressWarnings("unchecked")
            final Consumer<String> listener = (Consumer<String>)uncastListener;
            listener.accept(value);
        }
    }

    @Override
    public void addByteListener(final FieldEnumType field, final Consumer<Byte> consumer)
    {
        listeners[field.ordinal()].addTail(consumer);
    }

    @Override
    public void addShortListener(final FieldEnumType field, final Consumer<Short> consumer)
    {
        listeners[field.ordinal()].addTail(consumer);
    }

    @Override
    public void addIntListener(final FieldEnumType field, final Consumer<Integer> consumer)
    {
        listeners[field.ordinal()].addTail(consumer);
    }

    @Override
    public void addStringListener(final FieldEnumType field, final Consumer<String> consumer)
    {
        listeners[field.ordinal()].addTail(consumer);
    }

    @Override
    public void removeListener(final FieldEnumType field, final Consumer<?> consumer)
    {
        listeners[field.ordinal()].remove(consumer);
    }
}
