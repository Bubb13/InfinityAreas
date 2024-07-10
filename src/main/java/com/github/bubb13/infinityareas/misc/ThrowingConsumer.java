
package com.github.bubb13.infinityareas.misc;

public interface ThrowingConsumer<T, ThrowableType extends Throwable>
{
    void accept(T o) throws ThrowableType;
}
