
package com.github.bubb13.infinityareas.misc;

import java.util.function.Function;

public class OrderedInstanceSet<T> extends SimpleLinkedList<T>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final InstanceHashMap<T, Node> valueToNode = new InstanceHashMap<>();

    ////////////////////
    // Public Methods //
    ////////////////////

    public boolean contains(final T value)
    {
        return valueToNode.containsKey(value);
    }

    public void remove(final T value)
    {
        final var node = valueToNode.remove(value);
        if (node == null) return;
        node.remove();
    }

    public void clear()
    {
        super.clear();
        valueToNode.clear();
    }

    //////////////////////
    // Public Overrides //
    //////////////////////

    @Override
    public Node addTail(final T value)
    {
        final var existingNode = valueToNode.get(value);
        if (existingNode != null) return existingNode;
        return addTailInternal(new Node(value));
    }

    @Override
    public Node addTail(final Function<Node, T> consumer)
    {
        final Node newTail = new Node();
        final T value = consumer.apply(newTail);

        final var existingNode = valueToNode.get(value);
        if (existingNode != null) return existingNode;

        newTail.value = value;
        return addTailInternal(newTail);
    }

    /////////////////////////
    // Protected Overrides //
    /////////////////////////

    @Override
    protected Node addAfter(final Node node, final T value)
    {
        final var existingNode = valueToNode.get(value);
        if (existingNode != null) return existingNode;
        return addAfterInternal(node, new Node(value));
    }

    @Override
    protected Node addAfter(final Node node, final Function<Node, T> consumer)
    {
        final Node newNode = new Node();
        final T value = consumer.apply(newNode);

        final var existingNode = valueToNode.get(value);
        if (existingNode != null) return existingNode;

        newNode.value = value;
        return addAfterInternal(node, newNode);
    }

    @Override
    protected void onAdd(final Node node)
    {
        valueToNode.put(node.value, node);
    }

    @Override
    protected void onRemove(final Node node)
    {
        valueToNode.remove(node.value);
    }
}
