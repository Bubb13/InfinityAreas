
package com.github.bubb13.infinityareas.misc;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * A simple linked list implementation that exposes its internal node objects to facilitate mid-list manipulation.
 * Also implements the ability to hide nodes from users to facilitate soft-deletion.
 */
public class SimpleLinkedList<T> implements Iterable<T>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final Node head = new Node();
    private final Node tail = new Node();
    {
        head.next = tail;
        tail.previous = head;
    }
    private int nodeCount = 0;
    private int hiddenNodeCount = 0;

    ////////////////////
    // Public Methods //
    ////////////////////

    public T get(final int index)
    {
        return getNode(index).value;
    }

    public Node getNode(final int index)
    {
        if (index < 0 || index >= size())
        {
            throw new IndexOutOfBoundsException();
        }

        Node curNode = head;
        for (int i = 0; i <= index; ++i)
        {
            assert curNode != null;
            curNode = getNext(curNode);
        }

        return curNode;
    }

    public Node addTail(final T value)
    {
        final Node newTail = new Node(value);
        return addTailInternal(newTail);
    }

    public Node addTail(final Function<Node, T> consumer)
    {
        final Node newTail = new Node();
        newTail.value = consumer.apply(newTail);
        return addTailInternal(newTail);
    }

    public void clear(final boolean runRemove)
    {
        if (runRemove)
        {
            for (final var node : nodes())
            {
                onRemove(node, false);
            }
        }

        head.next = tail;
        tail.previous = head;
        nodeCount = 0;
        hiddenNodeCount = 0;
    }

    public void clear()
    {
        clear(false);
    }

    public int size()
    {
        return nodeCount - hiddenNodeCount;
    }

    public T getFirst()
    {
        return getFirstNode().value;
    }

    public T getLast()
    {
        return getLastNode().value;
    }

    public Node getFirstNode()
    {
        final Node first = getNext(head);
        return first != null ? first : tail;
    }

    public Node getLastNode()
    {
        final Node last = getPrevious(tail);
        return last != null ? last : head;
    }

    public Iterator<Node> nodeIterator()
    {
        return new Iterator<>()
        {
            ////////////////////
            // Private Fields //
            ////////////////////

            private Node current = head;

            ////////////////////
            // Public Methods //
            ////////////////////

            //--------------------//
            // Iterator Overrides //
            //--------------------//

            @Override
            public boolean hasNext()
            {
                return getNext(current) != null;
            }

            @Override
            public Node next()
            {
                current = getNext(current);
                if (current == null) throw new NoSuchElementException();
                return current;
            }

            @Override
            public void remove()
            {
                if (current == head) throw new NoSuchElementException();
                current.remove();
            }
        };
    }

    public Iterable<Node> nodes()
    {
        return this::nodeIterator;
    }

    //--------------------//
    // Iterable Overrides //
    //--------------------//

    @Override
    public Iterator<T> iterator()
    {
        return new Iterator<>()
        {
            ////////////////////
            // Private Fields //
            ////////////////////

            private final Iterator<Node> nodeItr = nodeIterator();

            ////////////////////
            // Public Methods //
            ////////////////////

            //--------------------//
            // Iterator Overrides //
            //--------------------//

            @Override
            public boolean hasNext()
            {
                return nodeItr.hasNext();
            }

            @Override
            public T next()
            {
                return nodeItr.next().value;
            }

            @Override
            public void remove()
            {
                nodeItr.remove();
            }
        };
    }

    ///////////////////////
    // Protected Methods //
    ///////////////////////

    /**
     * Called after {@code node} has been added to the list.
     * @param node The {@link com.github.bubb13.infinityareas.misc.SimpleLinkedList.Node} that was added.
     */
    protected void onAdd(final Node node, final boolean fromHide) {}

    /**
     * Called before {@code node} has been physically removed from the list.
     * @param node The {@link com.github.bubb13.infinityareas.misc.SimpleLinkedList.Node} that is about to be removed.
     */
    protected void onRemove(final Node node, final boolean fromHide) {}

    protected Node addAfter(final Node node, final T value)
    {
        return addAfterInternal(node, new Node(value));
    }

    protected Node addAfter(final Node node, final Function<Node, T> consumer)
    {
        final Node newNode = new Node();
        newNode.value = consumer.apply(newNode);
        return addAfterInternal(node, newNode);
    }

    protected Node addTailInternal(final Node newTail)
    {
        newTail.previous = tail.previous;
        newTail.next = tail;
        tail.previous.next = newTail;
        tail.previous = newTail;
        ++nodeCount;
        onAdd(newTail, false);
        return newTail;
    }

    protected Node addAfterInternal(final Node node, final Node newNode)
    {
        newNode.previous = node;
        newNode.next = node.next;
        node.next.previous = newNode;
        node.next = newNode;
        ++nodeCount;
        onAdd(newNode, false);
        return newNode;
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private Node getPrevious(final Node node)
    {
        for (Node previous = node.previous; previous != head; previous = previous.previous)
        {
            if (!previous.hidden)
            {
                return previous;
            }
        }
        return null;
    }

    private Node getNext(final Node node)
    {
        for (Node next = node.next; next != tail; next = next.next)
        {
            if (!next.hidden)
            {
                return next;
            }
        }
        return null;
    }

    ////////////////////
    // Public Classes //
    ////////////////////

    public class Node
    {
        //////////////////////
        // Protected Fields //
        //////////////////////

        protected T value;

        ////////////////////
        // Private Fields //
        ////////////////////

        private Node previous;
        private Node next;
        private boolean hidden;

        ////////////////////////////
        // Protected Constructors //
        ////////////////////////////

        protected Node(final T value)
        {
            this.value = value;
        }

        protected Node() {}

        ////////////////////
        // Public Methods //
        ////////////////////

        public Node previous()
        {
            return getPrevious(this);
        }

        public Node next()
        {
            return getNext(this);
        }

        public void remove()
        {
            --nodeCount;
            if (hidden) --hiddenNodeCount;
            onRemove(this, false);
            previous.next = next;
            next.previous = previous;
        }

        public Node addAfter(final T value)
        {
            return SimpleLinkedList.this.addAfter(this, value);
        }

        public Node addAfter(final Function<Node, T> consumer)
        {
            return SimpleLinkedList.this.addAfter(this, consumer);
        }

        public T value()
        {
            return value;
        }

        public void setHidden(final boolean newHidden)
        {
            if (hidden == newHidden) return;

            if (newHidden)
            {
                ++hiddenNodeCount;
                onRemove(this, true);
            }

            hidden = newHidden;

            if (!newHidden)
            {
                --hiddenNodeCount;
                onAdd(this, true);
            }
        }

        public boolean isHidden()
        {
            return hidden;
        }
    }
}
