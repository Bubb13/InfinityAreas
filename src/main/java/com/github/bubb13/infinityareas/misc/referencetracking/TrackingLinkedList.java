
package com.github.bubb13.infinityareas.misc.referencetracking;

import com.github.bubb13.infinityareas.misc.SimpleLinkedList;

/**
 * A simple linked list implementation that exposes its internal node objects to facilitate mid-list manipulation.
 * Also implements the ability to hide nodes from users to facilitate soft-deletion.
 * Implements the {@link ReferenceHolder} interface to facilitate the automatic removal of deleted held objects.
 */
public class TrackingLinkedList<T extends ReferenceTrackable> extends SimpleLinkedList<T> implements ReferenceHolder<T>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final String name;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public TrackingLinkedList(final String name)
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

    //----------------------------//
    // SimpleLinkedList Overrides //
    //----------------------------//

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
