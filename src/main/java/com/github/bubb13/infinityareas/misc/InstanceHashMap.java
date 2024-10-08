
package com.github.bubb13.infinityareas.misc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

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

    public Value computeIfAbsent(final Key key, final Function<? super Key, ? extends Value> mappingFunction)
    {
        // `new` KeyWrapper because it is used as the key on absence
        //noinspection unchecked
        return map.computeIfAbsent(new KeyWrapper(key), (keyWrapper) -> mappingFunction.apply((Key)keyWrapper.key));
    }

    public Iterator<MapEntry> entryIterator()
    {
        return new Iterator<>()
        {
            final Iterator<Map.Entry<KeyWrapper, Value>> itr = map.entrySet().iterator();
            final MapEntry tempMapEntry = new MapEntry();

            @Override
            public boolean hasNext()
            {
                return itr.hasNext();
            }

            @Override
            public MapEntry next()
            {
                final var entry = itr.next();
                //noinspection unchecked
                tempMapEntry.key = (Key)entry.getKey().key;
                tempMapEntry.value = entry.getValue();
                return tempMapEntry;
            }
        };
    }

    public Iterable<MapEntry> entries()
    {
        return this::entryIterator;
    }

    ////////////////////
    // Public Classes //
    ////////////////////

    public class MapEntry
    {
        private Key key;
        private Value value;

        private MapEntry() {}

        public Key getKey()
        {
            return key;
        }

        public Value getValue()
        {
            return value;
        }
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
