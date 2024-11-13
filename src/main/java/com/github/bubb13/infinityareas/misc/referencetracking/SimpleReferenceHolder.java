
package com.github.bubb13.infinityareas.misc.referencetracking;

public class SimpleReferenceHolder<T extends ReferenceTrackable> implements ReferenceHolder<T>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private T referenceSoftDeleted;
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

    public void set(final T reference)
    {
        this.reference = reference;
        reference.addedTo(this);
    }

    public T get()
    {
        return reference;
    }

    //---------------------------//
    // ReferenceHolder Overrides //
    //---------------------------//

    @Override
    public void referencedObjectSoftDeleted(final T reference)
    {
        this.reference = null;
        referenceSoftDeleted = reference;
    }

    @Override
    public void restoreSoftDeletedObject(final T reference)
    {
        this.reference = referenceSoftDeleted;
        referenceSoftDeleted = null;
    }

    @Override
    public void referencedObjectDeleted(final T reference)
    {
        this.reference = null;
    }
}
