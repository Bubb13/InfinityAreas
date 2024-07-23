
package com.github.bubb13.infinityareas.gui.editor;

import com.github.bubb13.infinityareas.game.resource.WED;
import com.github.bubb13.infinityareas.misc.SimpleLinkedList;

import java.util.function.Supplier;

public class WEDPolygonDelegator implements PolygonDelegator
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final Supplier<Boolean> enabledSupplier;
    private WED wed;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public WEDPolygonDelegator(final Supplier<Boolean> enabledSupplier)
    {
        this.enabledSupplier = enabledSupplier;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void setWED(final WED wed)
    {
        this.wed = wed;
    }

    @Override
    public boolean enabled()
    {
        return enabledSupplier.get();
    }

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
        wed.addPolygon((WED.Polygon)genericPolygon);
    }

    @Override
    public void delete(final GenericPolygon genericPolygon)
    {
        ((WED.Polygon)genericPolygon).delete();
    }
}
