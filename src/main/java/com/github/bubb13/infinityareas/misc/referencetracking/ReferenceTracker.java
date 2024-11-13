
package com.github.bubb13.infinityareas.misc.referencetracking;

import com.github.bubb13.infinityareas.misc.InstanceHashMap;
import com.github.bubb13.infinityareas.misc.SimpleLinkedList;

/**
 * Reusable implementation of the {@link ReferenceTrackable} interface.
 */
public class ReferenceTracker implements ReferenceTrackable
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final ReferenceTrackable object;
    private final SimpleLinkedList<ReferenceHolder<?>> referenceHolders = new SimpleLinkedList<>();
    private final InstanceHashMap<ReferenceHolder<?>, SimpleLinkedList<ReferenceHolder<?>>.Node> referenceHolderToNode
        = new InstanceHashMap<>();

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public ReferenceTracker(final ReferenceTrackable object)
    {
        this.object = object;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    //------------------------------//
    // ReferenceTrackable Overrides //
    //------------------------------//

    @Override
    public void addedTo(final ReferenceHolder<?> referenceHolder)
    {
        // Add `referenceHolder` to `referenceHolders` and add node mapping to `referenceHolderToNode`
        referenceHolderToNode.put(referenceHolder, referenceHolders.addTail(referenceHolder));
    }

    @Override
    public void removedFrom(final ReferenceHolder<?> referenceHolder)
    {
        // Remove `referenceHolder` node mapping from `referenceHolderToNode` and value from `referenceHolders`
        referenceHolderToNode.remove(referenceHolder).remove();
    }

    @Override
    public void softDelete()
    {
        //noinspection rawtypes
        for (final ReferenceHolder referenceHolder : referenceHolders)
        {
            //noinspection unchecked
            referenceHolder.referencedObjectSoftDeleted(object);
        }
    }

    @Override
    public void restore()
    {
        //noinspection rawtypes
        for (final ReferenceHolder referenceHolder : referenceHolders)
        {
            //noinspection unchecked
            referenceHolder.restoreSoftDeletedObject(object);
        }
    }

    @Override
    public void delete()
    {
        //noinspection rawtypes
        for (final ReferenceHolder referenceHolder : referenceHolders)
        {
            //noinspection unchecked
            referenceHolder.referencedObjectDeleted(object);
        }
    }
}
