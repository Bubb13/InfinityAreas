
package com.github.bubb13.infinityareas.misc;

import java.util.function.Function;

/**
 * Simple set implementation using a backing linked list for order, and a backing hashmap for duplicate lookups.
 */
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
        final Node node = valueToNode.get(value);
        return node != null && !node.isHidden();
    }

    public void remove(final T value)
    {
        final var node = valueToNode.get(value);
        if (node == null || node.isHidden()) return;
        node.remove();
    }

    //----------------------------//
    // SimpleLinkedList Overrides //
    //----------------------------//

    @Override
    public Node addTail(final T value)
    {
        final var existingNode = valueToNode.get(value);

        if (existingNode != null)
        {
            moveToTailInternal(existingNode);
            existingNode.setHidden(false);
            return existingNode;
        }

        return addTailInternal(new Node(value));
    }

    @Override
    public Node addTail(final Function<Node, T> consumer)
    {
        final Node newTail = new Node();
        final T value = consumer.apply(newTail);

        final var existingNode = valueToNode.get(value);
        if (existingNode != null)
        {
            throw new IllegalStateException("Passed bad node to consumer");
        }

        newTail.value = value;
        return addTailInternal(newTail);
    }

    @Override
    public void clear()
    {
        super.clear();

        // Rebuild mapping between the preserved hidden elements and their nodes
        valueToNode.clear();
        for (final Node itrNode : nodesInternal())
        {
            valueToNode.put(itrNode.value(), itrNode);
        }
    }

    ///////////////////////
    // Protected Methods //
    ///////////////////////

    protected void setHidden(final T value, final boolean hidden)
    {
        final var node = valueToNode.get(value);
        if (node == null) return;
        node.setHidden(hidden);
    }

    //----------------------------//
    // SimpleLinkedList Overrides //
    //----------------------------//

    @Override
    protected Node addAfterInternal(final Node node, final T value)
    {
        final var existingNode = valueToNode.get(value);

        if (existingNode != null)
        {
            moveAfterInternal(node, existingNode);
            existingNode.setHidden(false);
            return existingNode;
        }

        return addAfterInternal(node, new Node(value));
    }

    @Override
    protected Node addAfterInternal(final Node node, final Function<Node, T> consumer)
    {
        final Node newNode = new Node();
        final T value = consumer.apply(newNode);

        final var existingNode = valueToNode.get(value);
        if (existingNode != null)
        {
            throw new IllegalStateException("Passed bad node to consumer");
        }

        newNode.value = value;
        return addAfterInternal(node, newNode);
    }

    @Override
    protected void onAdd(final Node node, final boolean fromHide)
    {
        if (fromHide) return;
        valueToNode.put(node.value, node);
    }

    @Override
    protected void onRemove(final Node node, final boolean fromHide)
    {
        if (fromHide) return;
        valueToNode.remove(node.value);
    }
}
