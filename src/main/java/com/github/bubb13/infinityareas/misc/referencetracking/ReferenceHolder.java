
package com.github.bubb13.infinityareas.misc.referencetracking;

public interface ReferenceHolder<T extends ReferenceTrackable>
{
    void referencedObjectSoftDeleted(ReferenceHandle referenceHandle);
    void restoreSoftDeletedObject(ReferenceHandle referenceHandle);
    void referencedObjectDeleted(ReferenceHandle referenceHandle);
    T handleToObject(ReferenceHandle referenceHandle);
}
