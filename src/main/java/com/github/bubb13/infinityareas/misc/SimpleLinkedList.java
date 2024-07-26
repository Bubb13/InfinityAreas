
package com.github.bubb13.infinityareas.misc;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

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
    private int size = 0;

    ////////////////////
    // Public Methods //
    ////////////////////

    public T get(final int index)
    {
        return getNode(index).value;
    }

    public Node getNode(final int index)
    {
        if (index < 0 || index >= size)
        {
            throw new IndexOutOfBoundsException();
        }

        Node curNode = head.next;
        for (int i = 0; i < index; ++i)
        {
            curNode = curNode.next;
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

    public void clear()
    {
        head.next = tail;
        tail.previous = head;
        size = 0;
    }

    public int size()
    {
        return size;
    }

    public T getFirst()
    {
        return head.next.value;
    }

    public T getLast()
    {
        return tail.previous.value;
    }

    public Node getFirstNode()
    {
        return head.next;
    }

    public Node getLastNode()
    {
        return tail.previous;
    }

    @Override
    public Iterator<T> iterator()
    {
        return new Iterator<>()
        {
            private final Iterator<Node> nodeItr = nodeIterator();

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

    public Iterator<Node> nodeIterator()
    {
        return new Iterator<>()
        {
            private Node current = head;

            @Override
            public boolean hasNext()
            {
                return current.next != tail;
            }

            @Override
            public Node next()
            {
                current = current.next;
                if (current == tail) throw new NoSuchElementException();
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

    ///////////////////////
    // Protected Methods //
    ///////////////////////

    protected void onAdd(final Node node) {}

    protected void onRemove(final Node value) {}

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
        ++size;
        onAdd(newTail);
        return newTail;
    }

    protected Node addAfterInternal(final Node node, final Node newNode)
    {
        newNode.previous = node;
        newNode.next = node.next;
        node.next.previous = newNode;
        node.next = newNode;
        ++size;
        onAdd(newNode);
        return newNode;
    }

    ////////////////////
    // Public Classes //
    ////////////////////

    public class Node
    {
        private Node previous;
        private Node next;
        protected T value;

        protected Node(final T value)
        {
            this.value = value;
        }

        protected Node() {}

        public Node previous()
        {
            if (previous == head) return null;
            return previous;
        }

        public Node next()
        {
            if (next == tail) return null;
            return next;
        }

        public void remove()
        {
            onRemove(this);
            previous.next = next;
            next.previous = previous;
            --size;
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
    }
}
