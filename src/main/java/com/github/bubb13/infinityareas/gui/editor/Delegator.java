
package com.github.bubb13.infinityareas.gui.editor;

public interface Delegator<T>
{
    T create();
    void add(T genericPolygon);
    void delete(T genericPolygon);
}
