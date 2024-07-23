
package com.github.bubb13.infinityareas.misc;

public interface ReferenceTrackable
{
    void addedTo(ReferenceHolder<?> referenceHolder);
    void removedFrom(ReferenceHolder<?> referenceHolder);
    void delete();
}
