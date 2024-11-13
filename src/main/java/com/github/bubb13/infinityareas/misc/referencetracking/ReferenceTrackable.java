
package com.github.bubb13.infinityareas.misc.referencetracking;

public interface ReferenceTrackable
{
    void addedTo(ReferenceHolder<?> referenceHolder);
    void removedFrom(ReferenceHolder<?> referenceHolder);
    void softDelete();
    void restore();
    void delete();
}
