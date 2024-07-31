
package com.github.bubb13.infinityareas.gui.editor.connector;

import com.github.bubb13.infinityareas.game.resource.Area;
import com.github.bubb13.infinityareas.gui.editor.field.enums.RegionFields;

import java.util.function.BiConsumer;

public class RegionConnector extends AbstractConnector<RegionFields>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final Area.Region region;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public RegionConnector(final Area.Region region)
    {
        super(RegionFields.VALUES);
        this.region = region;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    @Override
    public byte getByte(final RegionFields field)
    {
        throw new IllegalArgumentException();
    }

    @Override
    public void setByte(final RegionFields field, final byte newValue)
    {
        throw new IllegalArgumentException();
    }

    @Override
    public short getShort(final RegionFields field)
    {
        return switch (field)
        {
            case TYPE -> region.getType();
            case BOUNDING_BOX_LEFT -> (short)region.getPolygon().getBoundingBoxLeft();
            case BOUNDING_BOX_TOP -> (short)region.getPolygon().getBoundingBoxTop();
            case BOUNDING_BOX_RIGHT -> (short)region.getPolygon().getBoundingBoxRight();
            case BOUNDING_BOX_BOTTOM -> (short)region.getPolygon().getBoundingBoxBottom();
            case TRAP_DETECTION_DIFFICULTY -> region.getTrapDetectionDifficulty();
            case TRAP_DISARM_DIFFICULTY -> region.getTrapDisarmDifficulty();
            case TRAPPED -> region.getbTrapped();
            case DETECTED -> region.getbTrapDetected();
            case TRAP_LAUNCH_X -> (short)region.getTrapLaunchPoint().getX();
            case TRAP_LAUNCH_Y -> (short)region.getTrapLaunchPoint().getY();
            case ACTIVATION_X -> region.getActivationPointX();
            case ACTIVATION_Y -> region.getActivationPointY();
            default -> throw new IllegalArgumentException();
        };
    }

    @Override
    public void setShort(final RegionFields field, final short newValue)
    {
        final short oldValue = getShort(field);
        switch (field)
        {
            case TYPE -> region.setType(newValue);
            case BOUNDING_BOX_LEFT -> region.getPolygon().setBoundingBoxLeft(newValue);
            case BOUNDING_BOX_TOP -> region.getPolygon().setBoundingBoxTop(newValue);
            case BOUNDING_BOX_RIGHT -> region.getPolygon().setBoundingBoxRight(newValue);
            case BOUNDING_BOX_BOTTOM -> region.getPolygon().setBoundingBoxBottom(newValue);
            case TRAP_DETECTION_DIFFICULTY -> region.setTrapDetectionDifficulty(newValue);
            case TRAP_DISARM_DIFFICULTY -> region.setTrapDisarmDifficulty(newValue);
            case TRAPPED -> region.setbTrapped(newValue);
            case DETECTED -> region.setbTrapDetected(newValue);
            case TRAP_LAUNCH_X -> region.getTrapLaunchPoint().setX(newValue);
            case TRAP_LAUNCH_Y -> region.getTrapLaunchPoint().setY(newValue);
            case ACTIVATION_X -> region.setActivationPointX(newValue);
            case ACTIVATION_Y -> region.setActivationPointY(newValue);
            default -> throw new IllegalArgumentException();
        }
        runShortListeners(field, oldValue, newValue);
    }

    @Override
    public int getInt(final RegionFields field)
    {
        return switch (field)
        {
            case TRIGGER_VALUE -> region.getTriggerValue();
            case CURSOR_INDEX -> region.getCursorIndex();
            case FLAGS -> region.getFlags();
            case INFO_TEXT -> region.getInfoStrref();
            default -> throw new IllegalArgumentException();
        };
    }

    @Override
    public void setInt(final RegionFields field, final int newValue)
    {
        final int oldValue = getInt(field);
        switch (field)
        {
            case TRIGGER_VALUE -> region.setTriggerValue(newValue);
            case CURSOR_INDEX -> region.setCursorIndex(newValue);
            case FLAGS -> region.setFlags(newValue);
            case INFO_TEXT -> region.setInfoStrref(newValue);
            default -> throw new IllegalArgumentException();
        }
        runIntListeners(field, oldValue, newValue);
    }

    @Override
    public String getString(final RegionFields field)
    {
        return switch (field)
        {
            case NAME -> region.getName();
            case DESTINATION_AREA -> region.getDestAreaResref();
            case DESTINATION_ENTRANCE_NAME -> region.getEntranceNameInDestArea();
            case KEY -> region.getKeyResref();
            case SCRIPT -> region.getScriptResref();
            default -> throw new IllegalArgumentException();
        };
    }

    @Override
    public void setString(final RegionFields field, final String newValue)
    {
        final String oldValue = getString(field);
        switch (field)
        {
            case NAME -> region.setName(newValue);
            case DESTINATION_AREA -> region.setDestAreaResref(newValue);
            case DESTINATION_ENTRANCE_NAME -> region.setEntranceNameInDestArea(newValue);
            case KEY -> region.setKeyResref(newValue);
            case SCRIPT -> region.setScriptResref(newValue);
            default -> throw new IllegalArgumentException();
        }
        runStringListeners(field, oldValue, newValue);
    }

    @Override
    public void addByteListener(final RegionFields field, final BiConsumer<Byte, Byte> consumer)
    {
        throw new IllegalArgumentException();
    }

    @Override
    public void addShortListener(final RegionFields field, final BiConsumer<Short, Short> consumer)
    {
        switch (field)
        {
            case TYPE, BOUNDING_BOX_LEFT, BOUNDING_BOX_TOP, BOUNDING_BOX_RIGHT, BOUNDING_BOX_BOTTOM,
                 TRAP_DETECTION_DIFFICULTY, TRAP_DISARM_DIFFICULTY, TRAPPED, DETECTED, TRAP_LAUNCH_X, TRAP_LAUNCH_Y,
                 ACTIVATION_X, ACTIVATION_Y -> {}
            default -> throw new IllegalArgumentException();
        }
        super.addShortListener(field, consumer);
    }

    @Override
    public void addIntListener(final RegionFields field, final BiConsumer<Integer, Integer> consumer)
    {
        switch (field)
        {
            case TRIGGER_VALUE, CURSOR_INDEX, FLAGS, INFO_TEXT -> {}
            default -> throw new IllegalArgumentException();
        }
        super.addIntListener(field, consumer);
    }

    @Override
    public void addStringListener(final RegionFields field, final BiConsumer<String, String> consumer)
    {
        switch (field)
        {
            case NAME, DESTINATION_AREA, DESTINATION_ENTRANCE_NAME, KEY, SCRIPT -> {}
            default -> throw new IllegalArgumentException();
        }
        super.addStringListener(field, consumer);
    }
}
