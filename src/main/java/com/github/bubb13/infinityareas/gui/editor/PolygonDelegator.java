
package com.github.bubb13.infinityareas.gui.editor;

public interface PolygonDelegator
{
    boolean enabled();
    GenericPolygon create();
    void add(GenericPolygon genericPolygon);
    void delete(GenericPolygon genericPolygon);
}
