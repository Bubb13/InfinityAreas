
package com.github.bubb13.infinityareas.misc;

public interface ThrowingFunction<T, ReturnType, ThrowableType extends Throwable>
{
    ReturnType apply(T o) throws ThrowableType;
}
