
package com.github.bubb13.infinityareas.misc.referencetracking;

public interface ReferenceHolder<T extends ReferenceTrackable>
{
    void referencedObjectSoftDeleted(final T reference);
    void restoreSoftDeletedObject(final T reference);
    void referencedObjectDeleted(final T reference);
}
