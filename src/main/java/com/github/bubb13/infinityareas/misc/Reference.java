
package com.github.bubb13.infinityareas.misc;

public class Reference<T>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private T value;

    ////////////////////
    // Public Methods //
    ////////////////////

    public T get()
    {
        return value;
    }

    public T set(final T newValue)
    {
        value = newValue;
        return value;
    }
}
