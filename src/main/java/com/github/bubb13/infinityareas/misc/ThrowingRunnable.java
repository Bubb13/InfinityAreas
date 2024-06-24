
package com.github.bubb13.infinityareas.misc;

public interface ThrowingRunnable<ThrowableType extends Throwable>
{
    void run() throws ThrowableType;
}
