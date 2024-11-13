
package com.github.bubb13.infinityareas.misc.reference;

public class ReadOnlyReference<T>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final Reference<T> ref;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public ReadOnlyReference(final Reference<T> ref)
    {
        this.ref = ref;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public T get()
    {
        return ref.get();
    }
}
