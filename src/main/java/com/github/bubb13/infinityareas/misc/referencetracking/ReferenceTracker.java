
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

    private final SimpleLinkedList<ReferenceHolderPair> referenceHolderPairs = new SimpleLinkedList<>();
    private final InstanceHashMap<ReferenceHolder<?>, SimpleLinkedList<ReferenceHolderPair>.Node> referenceHolderToNode
        = new InstanceHashMap<>();

    ////////////////////
    // Public Methods //
    ////////////////////

    //------------------------------//
    // ReferenceTrackable Overrides //
    //------------------------------//

    @Override
    public void addedTo(final ReferenceHolder<?> referenceHolder, final ReferenceHandle referenceHandle)
    {
        // Add `referenceHolder` to `referenceHolders` and add node mapping to `referenceHolderToNode`
        final var node = referenceHolderPairs.addTail(new ReferenceHolderPair(referenceHolder, referenceHandle));
        referenceHolderToNode.put(referenceHolder, node);
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
        for (final ReferenceHolderPair referenceHolderPair : referenceHolderPairs)
        {
            referenceHolderPair.holder().referencedObjectSoftDeleted(referenceHolderPair.handle());
        }
    }

    @Override
    public void restore()
    {
        for (final ReferenceHolderPair referenceHolderPair : referenceHolderPairs)
        {
            referenceHolderPair.holder().restoreSoftDeletedObject(referenceHolderPair.handle());
        }
    }

    @Override
    public void delete()
    {
        for (final ReferenceHolderPair referenceHolderPair : referenceHolderPairs)
        {
            referenceHolderPair.holder().referencedObjectDeleted(referenceHolderPair.handle());
        }
    }

    ////////////////////////////
    // Private Static Classes //
    ////////////////////////////

    private record ReferenceHolderPair(ReferenceHolder<?> holder, ReferenceHandle handle) {}
}
