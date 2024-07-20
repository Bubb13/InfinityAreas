
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

    public Node addTail(final T value)
    {
        final Node newTail = new Node(value);
        return addTail(newTail);
    }

    public Node addTail(final Function<Node, T> consumer)
    {
        final Node newTail = new Node();
        newTail.value = consumer.apply(newTail);
        return addTail(newTail);
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

    /////////////////////
    // Private Methods //
    /////////////////////

    private Node addTail(final Node newTail)
    {
        newTail.previous = tail.previous;
        newTail.next = tail;
        tail.previous.next = newTail;
        tail.previous = newTail;
        ++size;
        return newTail;
    }

    ////////////////////
    // Public Classes //
    ////////////////////

    public class Node
    {
        private Node previous;
        private Node next;
        private T value;

        private Node(final T value)
        {
            this.value = value;
        }

        private Node() {}

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
            previous.next = next;
            next.previous = previous;
            --size;
        }

        public Node addAfter(final T value)
        {
            return addAfterInternal(new Node(value));
        }

        public Node addAfter(final Function<Node, T> consumer)
        {
            final Node newNode = new Node();
            newNode.value = consumer.apply(newNode);
            return addAfterInternal(newNode);
        }

        public T value()
        {
            return value;
        }

        /////////////////////
        // Private Methods //
        /////////////////////

        private Node addAfterInternal(final Node newNode)
        {
            newNode.previous = this;
            newNode.next = next;
            next.previous = newNode;
            next = newNode;
            ++size;
            return newNode;
        }
    }
}
