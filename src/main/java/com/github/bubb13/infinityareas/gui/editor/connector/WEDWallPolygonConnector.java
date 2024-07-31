
package com.github.bubb13.infinityareas.gui.editor.connector;

import com.github.bubb13.infinityareas.game.resource.WED;
import com.github.bubb13.infinityareas.gui.editor.field.enums.WEDWallPolygonFields;

import java.util.function.BiConsumer;

public class WEDWallPolygonConnector extends AbstractConnector<WEDWallPolygonFields>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final WED.Polygon polygon;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public WEDWallPolygonConnector(final WED.Polygon polygon)
    {
        super(WEDWallPolygonFields.VALUES);
        this.polygon = polygon;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    @Override
    public byte getByte(final WEDWallPolygonFields field)
    {
        return switch (field)
        {
            case FLAGS -> polygon.getFlags();
            case HEIGHT -> polygon.getHeight();
            default -> throw new IllegalArgumentException();
        };
    }

    @Override
    public void setByte(final WEDWallPolygonFields field, final byte newValue)
    {
        final byte oldValue = getByte(field);
        switch (field)
        {
            case FLAGS -> polygon.setFlags(newValue);
            case HEIGHT -> polygon.setHeight(newValue);
            default -> throw new IllegalArgumentException();
        }
        runByteListeners(field, oldValue, newValue);
    }

    @Override
    public short getShort(final WEDWallPolygonFields field)
    {
        return switch (field)
        {
            case BOUNDING_BOX_LEFT -> (short)polygon.getBoundingBoxLeft();
            case BOUNDING_BOX_RIGHT -> (short)polygon.getBoundingBoxRight();
            case BOUNDING_BOX_TOP -> (short)polygon.getBoundingBoxTop();
            case BOUNDING_BOX_BOTTOM -> (short)polygon.getBoundingBoxBottom();
            default -> throw new IllegalArgumentException();
        };
    }

    @Override
    public void setShort(final WEDWallPolygonFields field, final short newValue)
    {
        final short oldValue = getShort(field);
        switch (field)
        {
            case BOUNDING_BOX_LEFT -> polygon.setBoundingBoxLeft(newValue);
            case BOUNDING_BOX_RIGHT -> polygon.setBoundingBoxRight(newValue);
            case BOUNDING_BOX_TOP -> polygon.setBoundingBoxTop(newValue);
            case BOUNDING_BOX_BOTTOM -> polygon.setBoundingBoxBottom(newValue);
            default -> throw new IllegalArgumentException();
        }
        runShortListeners(field, oldValue, newValue);
    }

    @Override
    public int getInt(final WEDWallPolygonFields field)
    {
        throw new IllegalArgumentException();
    }

    @Override
    public void setInt(final WEDWallPolygonFields field, final int newValue)
    {
        throw new IllegalArgumentException();
    }

    @Override
    public String getString(final WEDWallPolygonFields value)
    {
        throw new IllegalArgumentException();
    }

    @Override
    public void setString(final WEDWallPolygonFields field, final String value)
    {
        throw new IllegalArgumentException();
    }

    @Override
    public void addByteListener(final WEDWallPolygonFields field, final BiConsumer<Byte, Byte> consumer)
    {
        switch (field)
        {
            case FLAGS, HEIGHT -> {}
            default -> throw new IllegalArgumentException();
        }
        super.addByteListener(field, consumer);
    }

    @Override
    public void addShortListener(final WEDWallPolygonFields field, final BiConsumer<Short, Short> consumer)
    {
        switch (field)
        {
            case BOUNDING_BOX_LEFT, BOUNDING_BOX_RIGHT, BOUNDING_BOX_TOP, BOUNDING_BOX_BOTTOM -> {}
            default -> throw new IllegalArgumentException();
        }
        super.addShortListener(field, consumer);
    }

    @Override
    public void addIntListener(final WEDWallPolygonFields field, final BiConsumer<Integer, Integer> consumer)
    {
        throw new IllegalArgumentException();
    }

    @Override
    public void addStringListener(final WEDWallPolygonFields field, final BiConsumer<String, String> consumer)
    {
        throw new IllegalArgumentException();
    }
}
