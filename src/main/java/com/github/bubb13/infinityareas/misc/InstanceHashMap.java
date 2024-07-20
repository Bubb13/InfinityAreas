
package com.github.bubb13.infinityareas.misc;

import java.util.HashMap;

public class InstanceHashMap<Key, Value>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final HashMap<KeyWrapper, Value> map = new HashMap<>();
    private final KeyWrapper tempWrapper = new KeyWrapper(null);

    ////////////////////
    // Public Methods //
    ////////////////////

    public Value put(final Key key, final Value value)
    {
        map.put(new KeyWrapper(key), value);
        return value;
    }

    public Value get(final Key key)
    {
        tempWrapper.key = key;
        return map.get(tempWrapper);
    }

    public Value remove(final Key key)
    {
        tempWrapper.key = key;
        return map.remove(tempWrapper);
    }

    public boolean containsKey(final Object key)
    {
        tempWrapper.key = key;
        return map.containsKey(tempWrapper);
    }

    public void clear()
    {
        map.clear();
    }

    public int size()
    {
        return map.size();
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private static class KeyWrapper
    {
        private Object key;

        public KeyWrapper(final Object key)
        {
            this.key = key;
        }

        @Override
        public boolean equals(final Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final KeyWrapper that = (KeyWrapper)o;
            return key == that.key;
        }

        @Override
        public int hashCode()
        {
            return System.identityHashCode(key);
        }
    }
}
