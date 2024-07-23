
package com.github.bubb13.infinityareas.gui.editor;

import com.github.bubb13.infinityareas.misc.SimpleLinkedList;

public class GenericPolygon
{
    private int boundingBoxLeft;
    private int boundingBoxRight;
    private int boundingBoxTop;
    private int boundingBoxBottom;
    private final SimpleLinkedList<GenericVertex> vertices;

    public GenericPolygon(
        final int boundingBoxLeft, final int boundingBoxRight,
        final int boundingBoxTop, final int boundingBoxBottom,
        final SimpleLinkedList<GenericVertex> vertices)
    {
        this.boundingBoxLeft = boundingBoxLeft;
        this.boundingBoxRight = boundingBoxRight;
        this.boundingBoxTop = boundingBoxTop;
        this.boundingBoxBottom = boundingBoxBottom;
        this.vertices = vertices;
    }

    public GenericVertex addVertex(final int x, final int y)
    {
        return vertices.addTail((node) -> new GenericVertex(node, x, y)).value();
    }

    public int getBoundingBoxLeft()
    {
        return boundingBoxLeft;
    }

    public void setBoundingBoxLeft(final int boundingBoxLeft)
    {
        this.boundingBoxLeft = boundingBoxLeft;
    }

    public int getBoundingBoxRight()
    {
        return boundingBoxRight;
    }

    public void setBoundingBoxRight(final int boundingBoxRight)
    {
        this.boundingBoxRight = boundingBoxRight;
    }

    public int getBoundingBoxTop()
    {
        return boundingBoxTop;
    }

    public void setBoundingBoxTop(final int boundingBoxTop)
    {
        this.boundingBoxTop = boundingBoxTop;
    }

    public int getBoundingBoxBottom()
    {
        return boundingBoxBottom;
    }

    public void setBoundingBoxBottom(final int boundingBoxBottom)
    {
        this.boundingBoxBottom = boundingBoxBottom;
    }

    public SimpleLinkedList<GenericVertex> getVertices()
    {
        return vertices;
    }
}
