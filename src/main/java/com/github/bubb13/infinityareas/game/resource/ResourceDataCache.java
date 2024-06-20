
package com.github.bubb13.infinityareas.game.resource;

import com.github.bubb13.infinityareas.game.Game;

import java.nio.ByteBuffer;
import java.util.HashMap;

public class ResourceDataCache
{
    private final HashMap<ResourceIdentifier, ByteBuffer> cache = new HashMap<>();

    public void add(final ResourceIdentifier identifier, final ByteBuffer buffer)
    {
        cache.put(identifier, buffer);
    }

    public void remove(final ResourceIdentifier identifier)
    {
        cache.remove(identifier);
    }

    public ByteBuffer get(final ResourceIdentifier identifier)
    {
        return cache.get(identifier);
    }

    public ByteBuffer demand(final Game.ResourceSource source) throws Exception
    {
        final ResourceIdentifier identifier = source.getIdentifier();
        ByteBuffer buffer = this.get(identifier);

        if (buffer == null)
        {
            buffer = source.demandFileData();
            this.add(identifier, buffer);
        }

        return buffer;
    }
}
