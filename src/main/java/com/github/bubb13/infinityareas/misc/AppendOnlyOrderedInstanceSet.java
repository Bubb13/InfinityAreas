
package com.github.bubb13.infinityareas.misc;

import java.util.ArrayList;
import java.util.Iterator;

public class AppendOnlyOrderedInstanceSet<T> implements Iterable<T>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final ArrayList<T> values = new ArrayList<>();
    private final InstanceHashMap<T, Integer> valueToIndex = new InstanceHashMap<>();
    private int size = 0;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public AppendOnlyOrderedInstanceSet() {}

    public AppendOnlyOrderedInstanceSet(final Iterable<T> iterable)
    {
        for (final T t : iterable)
        {
            add(t);
        }
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public int add(final T value)
    {
        final Integer index = valueToIndex.get(value);
        if (index != null) return index;
        values.add(value);
        valueToIndex.put(value, size);
        return size++;
    }

    public Integer indexOf(final T value)
    {
        return valueToIndex.get(value);
    }

    public int size()
    {
        return size;
    }

    public Iterator<T> values()
    {
        return new ReadOnlyIterator<>(values.iterator());
    }

    @Override
    public Iterator<T> iterator()
    {
        return values();
    }
}
