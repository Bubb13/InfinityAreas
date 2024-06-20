
package com.github.bubb13.infinityareas.misc;

import java.util.Iterator;

public class IteratorToIterable<T> implements Iterable<T>
{
    private final Iterator<T> iterator;

    public IteratorToIterable(final Iterator<T> iterator)
    {
        if (iterator == null)
        {
            throw new NullPointerException();
        }
        this.iterator = iterator;
    }

    @Override
    public Iterator<T> iterator()
    {
        return iterator;
    }
}
