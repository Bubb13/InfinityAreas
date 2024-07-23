
package com.github.bubb13.infinityareas.gui.editor;

import com.github.bubb13.infinityareas.misc.SimpleLinkedList;

public class GenericVertex
{
    private final SimpleLinkedList<GenericVertex>.Node node;
    private int x;
    private int y;

    public GenericVertex(final SimpleLinkedList<GenericVertex>.Node node, final int x, final int y)
    {
        this.node = node;
        this.x = x;
        this.y = y;
    }

    public int x()
    {
        return x;
    }

    public void setX(final int x)
    {
        this.x = x;
    }

    public int y()
    {
        return y;
    }

    public void setY(final int y)
    {
        this.y = y;
    }

    public SimpleLinkedList<GenericVertex>.Node getNode()
    {
        return node;
    }
}
