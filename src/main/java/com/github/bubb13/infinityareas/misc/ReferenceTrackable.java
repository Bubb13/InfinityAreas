
package com.github.bubb13.infinityareas.misc;

public abstract class ReferenceTrackable
{
    private final SimpleLinkedList<ReferenceHolder<?>> referenceHolders = new SimpleLinkedList<>();
    private final InstanceHashMap<ReferenceHolder<?>, SimpleLinkedList<ReferenceHolder<?>>.Node> referenceHolderToNode
        = new InstanceHashMap<>();

    public void addedTo(final ReferenceHolder<?> referenceHolder)
    {
        referenceHolderToNode.put(referenceHolder, referenceHolders.addTail(referenceHolder));
    }

    public void removedFrom(final ReferenceHolder<?> referenceHolder)
    {
        referenceHolderToNode.remove(referenceHolder).remove();
    }

    public void delete()
    {
        //noinspection rawtypes
        for (final ReferenceHolder referenceHolder : referenceHolders)
        {
            //noinspection unchecked
            referenceHolder.referencedObjectDeleted(this);
        }
    }
}
