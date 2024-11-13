
package com.github.bubb13.infinityareas.gui.editor;

import com.github.bubb13.infinityareas.misc.OrderedInstanceSet;
import com.github.bubb13.infinityareas.misc.SimpleLinkedList;

public class GenericPolygon
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private int boundingBoxLeft;
    private int boundingBoxRight;
    private int boundingBoxTop;
    private int boundingBoxBottom;

    private double[] xArray = new double[0];
    private double[] yArray = new double[0];

    private final OrderedInstanceSet<Vertex> vertices = new OrderedInstanceSet<>()
    {
        @Override
        protected void onAdd(final SimpleLinkedList<Vertex>.Node node, final boolean fromHide)
        {
            super.onAdd(node, fromHide);

            final Node nextNode = node.next();
            if (nextNode == null)
            {
                node.value().index = size() - 1;
            }
            else
            {
                node.value().index = nextNode.value().index;

                for (var itrNode = node.next(); itrNode != null; itrNode = itrNode.next())
                {
                    ++itrNode.value().index;
                }
            }

            rebuildArraysFrom(node);
        }

        @Override
        protected void onRemove(final Node node, final boolean fromHide)
        {
            super.onRemove(node, fromHide);

            var nextNode = node.next();
            var curItrNode = nextNode;

            for (; curItrNode != null; curItrNode = curItrNode.next())
            {
                --curItrNode.value().index;
            }

            if (nextNode != null)
            {
                rebuildArraysFrom(nextNode);
            }
        }

        private void rebuildArraysFrom(final SimpleLinkedList<Vertex>.Node node)
        {
            final int numVertices = vertices.size();
            final int startIndex = node.value().index;

            if (numVertices > xArray.length)
            {
                final double[] newXArray = new double[numVertices];
                final double[] newYArray = new double[numVertices];

                System.arraycopy(xArray, 0, newXArray, 0, startIndex);
                System.arraycopy(yArray, 0, newYArray, 0, startIndex);

                xArray = newXArray;
                yArray = newYArray;
            }

            var curItrNode = node;
            for (int i = startIndex; curItrNode != null; curItrNode = curItrNode.next(), ++i)
            {
                final Vertex vertex = curItrNode.value();
                xArray[i] = vertex.x();
                yArray[i] = vertex.y();
            }
        }
    };

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public GenericPolygon(
        final int boundingBoxLeft, final int boundingBoxRight,
        final int boundingBoxTop, final int boundingBoxBottom)
    {
        this.boundingBoxLeft = boundingBoxLeft;
        this.boundingBoxRight = boundingBoxRight;
        this.boundingBoxTop = boundingBoxTop;
        this.boundingBoxBottom = boundingBoxBottom;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public Vertex addVertex(final int x, final int y)
    {
        return vertices.addTail((node) -> new Vertex(node, x, y)).value();
    }

    public Vertex newVertex(final SimpleLinkedList<Vertex>.Node node, final int x, final int y)
    {
        return new Vertex(node, x, y);
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

    public SimpleLinkedList<Vertex> getVertices()
    {
        return vertices;
    }

    public double[] xArray()
    {
        return xArray;
    }

    public double[] yArray()
    {
        return yArray;
    }

    public boolean contains(final double x, final double y)
    {
        boolean result = false;
        final int numVertices = vertices.size();

        for (int i = 0, j = numVertices - 1; i < numVertices; j = i++)
        {
            if ((yArray[i] > y) != (yArray[j] > y)
                &&
                (x < (xArray[j] - xArray[i]) * (y - yArray[i]) / (yArray[j] - yArray[i]) + xArray[i]))
            {
                result = !result;
            }
        }

        return result;
    }

    ////////////////////
    // Public Classes //
    ////////////////////

    public class Vertex
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final SimpleLinkedList<Vertex>.Node node;
        private int x;
        private int y;
        private int index;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public Vertex(final SimpleLinkedList<Vertex>.Node node, final int x, final int y)
        {
            this.node = node;
            this.x = x;
            this.y = y;
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        public int x()
        {
            return x;
        }

        public void setX(final int newX)
        {
            x = newX;
            xArray[index] = x;
        }

        public int y()
        {
            return y;
        }

        public void setY(final int newY)
        {
            y = newY;
            yArray[index] = y;
        }

        public SimpleLinkedList<Vertex>.Node getNode()
        {
            return node;
        }
    }
}
