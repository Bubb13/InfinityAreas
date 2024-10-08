
package com.github.bubb13.infinityareas.misc;

public class TrackingOrderedInstanceSet<T extends ReferenceTrackable>
    extends OrderedInstanceSet<T> implements ReferenceHolder<T>
{
    @Override
    protected void onAdd(final Node node)
    {
        super.onAdd(node);
        node.value.addedTo(this);
    }

    @Override
    protected void onRemove(final Node node)
    {
        super.onRemove(node);
        node.value.removedFrom(this);
    }

    @Override
    public void referencedObjectDeleted(final T reference)
    {
        remove(reference);
    }
}
