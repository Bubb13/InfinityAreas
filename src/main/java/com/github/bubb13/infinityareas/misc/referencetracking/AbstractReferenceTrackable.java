
package com.github.bubb13.infinityareas.misc.referencetracking;

public abstract class AbstractReferenceTrackable implements ReferenceTrackable
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final ReferenceTracker referenceTracker = new ReferenceTracker();

    ////////////////////
    // Public Methods //
    ////////////////////

    //------------------------------//
    // ReferenceTrackable Overrides //
    //------------------------------//

    @Override
    public void addedTo(final ReferenceHolder<?> referenceHolder, ReferenceHandle referenceHandle)
    {
        referenceTracker.addedTo(referenceHolder, referenceHandle);
    }

    @Override
    public void removedFrom(final ReferenceHolder<?> referenceHolder)
    {
        referenceTracker.removedFrom(referenceHolder);
    }

    @Override
    public void softDelete()
    {
        referenceTracker.softDelete();
    }

    @Override
    public void restore()
    {
        referenceTracker.restore();
    }

    @Override
    public void delete()
    {
        referenceTracker.delete();
    }
}
