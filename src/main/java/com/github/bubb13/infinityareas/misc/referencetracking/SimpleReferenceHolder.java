
package com.github.bubb13.infinityareas.misc.referencetracking;

public class SimpleReferenceHolder<T extends ReferenceTrackable> implements ReferenceHolder<T>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private T reference;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public SimpleReferenceHolder() {}

    public SimpleReferenceHolder(final T reference)
    {
        set(reference);
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void set(final T newReference)
    {
        if (newReference == null)
        {
            if (reference != null)
            {
                reference.removedFrom(this);
            }

            reference = null;
        }
        else if (newReference != reference)
        {
            if (reference != null)
            {
                reference.removedFrom(this);
            }

            reference = newReference;
            reference.addedTo(this, ReferenceHandle.create(reference));
        }
    }

    public T get()
    {
        return reference;
    }

    //---------------------------//
    // ReferenceHolder Overrides //
    //---------------------------//

    @Override
    public void referencedObjectSoftDeleted(final ReferenceHandle referenceHandle)
    {
        this.reference = null;
    }

    @Override
    public void restoreSoftDeletedObject(final ReferenceHandle referenceHandle)
    {
        this.reference = handleToObject(referenceHandle);
    }

    @Override
    public void referencedObjectDeleted(final ReferenceHandle referenceHandle)
    {
        this.reference = null;
    }

    @Override
    public T handleToObject(final ReferenceHandle referenceHandle)
    {
        return referenceHandle.cast();
    }
}
