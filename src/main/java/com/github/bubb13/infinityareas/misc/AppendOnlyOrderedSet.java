
package com.github.bubb13.infinityareas.misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class AppendOnlyOrderedSet<T> implements Iterable<T>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final ArrayList<T> values = new ArrayList<>();
    private final HashMap<T, Integer> valueToIndex = new HashMap<>();
    private int size = 0;

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
