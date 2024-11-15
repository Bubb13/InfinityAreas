
package com.github.bubb13.infinityareas.misc.referencetracking;

public abstract class AbstractReferenceHolder<T extends ReferenceTrackable> implements ReferenceHolder<T>
{
    ////////////////////
    // Public Methods //
    ////////////////////

    //---------------------------//
    // ReferenceHolder Overrides //
    //---------------------------//

    @Override
    public void referencedObjectSoftDeleted(final ReferenceHandle referenceHandle)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restoreSoftDeletedObject(final ReferenceHandle referenceHandle)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void referencedObjectDeleted(final ReferenceHandle referenceHandle)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public T handleToObject(final ReferenceHandle referenceHandle)
    {
        throw new UnsupportedOperationException();
    }
}
