
package com.github.bubb13.infinityareas.misc.referencetracking;

// Intended to mask the actual type of a reference handle while still providing some type semantics
public final class ReferenceHandle
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final Object reference;

    ///////////////////////////
    // Public Static Methods //
    ///////////////////////////

    public static ReferenceHandle create(final Object reference)
    {
        return new ReferenceHandle(reference);
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    @SuppressWarnings("unchecked")
    public <T> T cast()
    {
        return (T)reference;
    }

    //////////////////////////
    // Private Constructors //
    //////////////////////////

    private ReferenceHandle(final Object reference)
    {
        this.reference = reference;
    }
}
