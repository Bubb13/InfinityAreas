
package com.github.bubb13.infinityareas;

@FunctionalInterface
public interface ThrowingConsumer<T, E extends Throwable>
{
    void accept(T t) throws Throwable;
}
