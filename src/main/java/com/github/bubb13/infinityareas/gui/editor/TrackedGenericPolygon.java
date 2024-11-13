
package com.github.bubb13.infinityareas.gui.editor;

import com.github.bubb13.infinityareas.misc.referencetracking.ReferenceHolder;
import com.github.bubb13.infinityareas.misc.referencetracking.ReferenceTrackable;
import com.github.bubb13.infinityareas.misc.referencetracking.ReferenceTracker;

public class TrackedGenericPolygon extends GenericPolygon implements ReferenceTrackable
{
    private final ReferenceTracker referenceTracker = new ReferenceTracker(this);

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
    public void addedTo(ReferenceHolder<?> referenceHolder)
    {
        referenceTracker.addedTo(referenceHolder);
    }

    @Override
    public void removedFrom(ReferenceHolder<?> referenceHolder)
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
