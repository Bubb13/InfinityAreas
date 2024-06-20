
package com.github.bubb13.infinityareas.misc;

import java.util.Iterator;

public class ReadOnlyIterator<E> implements Iterator<E>
{
    private final Iterator<? extends E> iterator;

    public ReadOnlyIterator(final Iterator<? extends E> iterator)
    {
        this.iterator = iterator;
    }

    @Override
    public boolean hasNext()
    {
        return iterator.hasNext();
    }

    @Override
    public E next()
    {
        return iterator.next();
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
