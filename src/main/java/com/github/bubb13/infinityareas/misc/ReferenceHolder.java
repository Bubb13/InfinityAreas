
package com.github.bubb13.infinityareas.misc;

public interface ReferenceHolder<T extends ReferenceTrackable>
{
    void referencedObjectDeleted(final T reference);
}
