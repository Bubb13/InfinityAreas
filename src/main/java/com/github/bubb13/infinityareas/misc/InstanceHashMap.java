
package com.github.bubb13.infinityareas.misc;

import java.util.HashMap;

public class InstanceHashMap<Key, Value>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final HashMap<KeyWrapper<Key>, Value> map = new HashMap<>();

    ////////////////////
    // Public Methods //
    ////////////////////

    public Value put(final Key key, final Value value)
    {
        map.put(new KeyWrapper<>(key), value);
        return value;
    }

    public Value get(final Key key)
    {
        return map.get(new KeyWrapper<>(key));
    }

    public Value remove(final Key key)
    {
        return map.remove(new KeyWrapper<>(key));
    }

    public boolean containsKey(final Object key)
    {
        //noinspection SuspiciousMethodCalls
        return map.containsKey(key);
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private record KeyWrapper<Key>(Key key)
    {
        @Override
        public boolean equals(final Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final KeyWrapper<?> that = (KeyWrapper<?>)o;
            return key == that.key;
        }

        @Override
        public int hashCode()
        {
            return System.identityHashCode(key);
        }
    }
}
