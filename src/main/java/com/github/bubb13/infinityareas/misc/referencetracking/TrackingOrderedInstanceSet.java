
package com.github.bubb13.infinityareas.misc.referencetracking;

import com.github.bubb13.infinityareas.misc.OrderedInstanceSet;

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
    public void referencedObjectSoftDeleted(final T reference)
    {
        setHidden(reference, true);
    }

    @Override
    public void restoreSoftDeletedObject(final T reference)
    {
        setHidden(reference, false);
    }

    @Override
    public void referencedObjectDeleted(final T reference)
    {
        remove(reference);
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
        node.value().addedTo(this);
    }

    @Override
    protected void onRemove(final Node node, final boolean fromHide)
    {
        super.onRemove(node, fromHide);
        if (fromHide) return;
        node.value().removedFrom(this);
    }
}
