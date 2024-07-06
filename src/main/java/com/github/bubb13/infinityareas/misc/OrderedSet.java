
package com.github.bubb13.infinityareas.misc;

import java.util.HashMap;
import java.util.Iterator;

public class OrderedSet<T> implements Iterable<T>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final Node<T> head = new Node<>();
    private final Node<T> tail = new Node<>();
    {
        head.next = tail;
        tail.prev = head;
    }
    private final HashMap<T, Node<T>> valueToNode = new HashMap<>();

    ////////////////////
    // Public Methods //
    ////////////////////

    public void addTail(final T value)
    {
        if (valueToNode.containsKey(value)) return;

        final Node<T> node = new Node<>();
        node.prev = tail.prev;
        node.next = tail;
        node.value = value;

        tail.prev.next = node;
        tail.prev = node;
        valueToNode.put(value, node);
    }

    public void remove(final T value)
    {
        final Node<T> node = valueToNode.get(value);
        if (node == null) return;
        removeNode(node);
    }

    public Iterator<T> values()
    {
        return new Iterator<>()
        {
            private Node<T> current = head;

            @Override
            public boolean hasNext()
            {
                return current.next != tail;
            }

            @Override
            public T next()
            {
                current = current.next;
                return current.value;
            }

            @Override
            public void remove()
            {
                removeNode(current);
            }
        };
    }

    @Override
    public Iterator<T> iterator()
    {
        return values();
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void removeNode(final Node<T> node)
    {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private static class Node<T>
    {
        private Node<T> prev;
        private Node<T> next;
        private T value;
    }
}
