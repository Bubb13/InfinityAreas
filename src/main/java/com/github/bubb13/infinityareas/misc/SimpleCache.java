
package com.github.bubb13.infinityareas.misc;

import java.util.HashMap;

public class SimpleCache<K, T>
{
    private final HashMap<K, T> cache = new HashMap<>();

    public void add(final K resref, final T element)
    {
        cache.put(resref, element);
    }

    public void remove(final K resref)
    {
        cache.remove(resref);
    }

    public T get(final K resref)
    {
        return cache.get(resref);
    }
}
