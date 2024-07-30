
package com.github.bubb13.infinityareas.misc;

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

    public void set(final T reference)
    {
        this.reference = reference;
        reference.addedTo(this);
    }

    public T get(final T reference)
    {
        return reference;
    }

    @Override
    public void referencedObjectDeleted(final T reference)
    {
        this.reference = null;
    }
}
