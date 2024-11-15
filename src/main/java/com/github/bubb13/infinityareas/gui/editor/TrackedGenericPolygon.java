
package com.github.bubb13.infinityareas.gui.editor;

import com.github.bubb13.infinityareas.misc.referencetracking.ReferenceHandle;
import com.github.bubb13.infinityareas.misc.referencetracking.ReferenceHolder;
import com.github.bubb13.infinityareas.misc.referencetracking.ReferenceTrackable;
import com.github.bubb13.infinityareas.misc.referencetracking.ReferenceTracker;

public class TrackedGenericPolygon extends GenericPolygon implements ReferenceTrackable
{
    private final ReferenceTracker referenceTracker = new ReferenceTracker();

    public TrackedGenericPolygon(
        final int boundingBoxLeft, final int boundingBoxRight,
        final int boundingBoxTop, final int boundingBoxBottom)
    {
        super(boundingBoxLeft, boundingBoxRight, boundingBoxTop, boundingBoxBottom);
    }

    //------------------------------//
    // ReferenceTrackable Overrides //
    //------------------------------//

    @Override
    public void addedTo(final ReferenceHolder<?> referenceHolder, final ReferenceHandle referenceHandle)
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
