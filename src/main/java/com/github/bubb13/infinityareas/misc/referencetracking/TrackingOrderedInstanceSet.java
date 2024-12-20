
package com.github.bubb13.infinityareas.misc.referencetracking;

import com.github.bubb13.infinityareas.misc.OrderedInstanceSet;
import com.github.bubb13.infinityareas.misc.SimpleLinkedList;

/**
 * Simple set implementation using a backing linked list for order, and a backing hashmap for duplicate lookups.
 * Implements the {@link ReferenceHolder} interface to facilitate the automatic removal of deleted held objects.
 */
public class TrackingOrderedInstanceSet<T extends ReferenceTrackable>
    extends OrderedInstanceSet<T> implements ReferenceHolder<T>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final String name;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public TrackingOrderedInstanceSet(final String name)
    {
        this.name = name;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    //---------------------------//
    // ReferenceHolder Overrides //
    //---------------------------//

    @Override
    public void referencedObjectSoftDeleted(final ReferenceHandle referenceHandle)
    {
        final SimpleLinkedList<T>.Node node = referenceHandle.cast();
        node.setHidden(true);
    }

    @Override
    public void restoreSoftDeletedObject(final ReferenceHandle referenceHandle)
    {
        final SimpleLinkedList<T>.Node node = referenceHandle.cast();
        node.setHidden(false);
    }

    @Override
    public void referencedObjectDeleted(final ReferenceHandle referenceHandle)
    {
        final SimpleLinkedList<T>.Node node = referenceHandle.cast();
        node.remove();
    }

    @Override
    public T handleToObject(final ReferenceHandle referenceHandle)
    {
        final SimpleLinkedList<T>.Node node = referenceHandle.cast();
        return node.value();
    }

    //------------------//
    // Object Overrides //
    //------------------//

    @Override
    public String toString()
    {
        return name;
    }

    ///////////////////////
    // Protected Methods //
    ///////////////////////

    //------------------------------//
    // OrderedInstanceSet Overrides //
    //------------------------------//

    @Override
    protected void onAdd(final Node node, final boolean fromHide)
    {
        super.onAdd(node, fromHide);
        if (fromHide) return;
        node.value().addedTo(this, ReferenceHandle.create(node));
    }

    @Override
    protected void onRemove(final Node node, final boolean fromHide)
    {
        super.onRemove(node, fromHide);
        if (fromHide) return;
        node.value().removedFrom(this);
    }
}
