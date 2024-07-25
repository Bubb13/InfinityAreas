
package com.github.bubb13.infinityareas.gui.editor;

import com.github.bubb13.infinityareas.game.resource.WED;
import com.github.bubb13.infinityareas.misc.ReadOnlyReference;
import com.github.bubb13.infinityareas.misc.SimpleLinkedList;

public class WEDPolygonDelegator implements Delegator<GenericPolygon>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final ReadOnlyReference<WED> wedRef;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public WEDPolygonDelegator(final ReadOnlyReference<WED> wedRef)
    {
        this.wedRef = wedRef;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    @Override
    public GenericPolygon create()
    {
        return new WED.Polygon(
            (byte)0, (byte)0,
            (short)0, (short)0,
            (short)0, (short)0,
            new SimpleLinkedList<>()
        );
    }

    @Override
    public void add(final GenericPolygon genericPolygon)
    {
        wedRef.get().addPolygon((WED.Polygon)genericPolygon);
    }

    @Override
    public void delete(final GenericPolygon genericPolygon)
    {
        ((WED.Polygon)genericPolygon).delete();
    }
}
