
package com.github.bubb13.infinityareas.misc;

public class ReferenceTracker implements ReferenceTrackable
{
    private final ReferenceTrackable object;
    private final SimpleLinkedList<ReferenceHolder<?>> referenceHolders = new SimpleLinkedList<>();
    private final InstanceHashMap<ReferenceHolder<?>, SimpleLinkedList<ReferenceHolder<?>>.Node> referenceHolderToNode
        = new InstanceHashMap<>();

    public ReferenceTracker(final ReferenceTrackable object)
    {
        this.object = object;
    }

    @Override
    public void addedTo(final ReferenceHolder<?> referenceHolder)
    {
        referenceHolderToNode.put(referenceHolder, referenceHolders.addTail(referenceHolder));
    }

    @Override
    public void removedFrom(final ReferenceHolder<?> referenceHolder)
    {
        referenceHolderToNode.remove(referenceHolder).remove();
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
