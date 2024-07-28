
package com.github.bubb13.infinityareas.gui.editor.connector;

import com.github.bubb13.infinityareas.game.resource.Area;
import com.github.bubb13.infinityareas.gui.editor.field.RegionFields;

public class RegionConnector implements Connector<RegionFields>
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
        this.region = region;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

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

    public void setShort(final RegionFields field, final short value)
    {
        switch (field)
        {
            case TYPE -> region.setType(value);
            case BOUNDING_BOX_LEFT -> region.getPolygon().setBoundingBoxLeft(value);
            case BOUNDING_BOX_TOP -> region.getPolygon().setBoundingBoxTop(value);
            case BOUNDING_BOX_RIGHT -> region.getPolygon().setBoundingBoxRight(value);
            case BOUNDING_BOX_BOTTOM -> region.getPolygon().setBoundingBoxBottom(value);
            case TRAP_DETECTION_DIFFICULTY -> region.setTrapDetectionDifficulty(value);
            case TRAP_DISARM_DIFFICULTY -> region.setTrapDisarmDifficulty(value);
            case TRAPPED -> region.setbTrapped(value);
            case DETECTED -> region.setbTrapDetected(value);
            case TRAP_LAUNCH_X -> region.getTrapLaunchPoint().setX(value);
            case TRAP_LAUNCH_Y -> region.getTrapLaunchPoint().setY(value);
            case ACTIVATION_X -> region.setActivationPointX(value);
            case ACTIVATION_Y -> region.setActivationPointY(value);
            default -> throw new IllegalArgumentException();
        }
    }

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

    public void setInt(final RegionFields field, final int value)
    {
        switch (field)
        {
            case TRIGGER_VALUE -> region.setTriggerValue(value);
            case CURSOR_INDEX -> region.setCursorIndex(value);
            case FLAGS -> region.setFlags(value);
            case INFO_TEXT -> region.setInfoStrref(value);
            default -> throw new IllegalArgumentException();
        }
    }

    public String getString(final RegionFields value)
    {
        return switch (value)
        {
            case NAME -> region.getName();
            case DESTINATION_AREA -> region.getDestAreaResref();
            case DESTINATION_ENTRANCE_NAME -> region.getEntranceNameInDestArea();
            case KEY -> region.getKeyResref();
            case SCRIPT -> region.getScriptResref();
            default -> throw new IllegalArgumentException();
        };
    }

    public void setString(final RegionFields field, final String value)
    {
        switch (field)
        {
            case NAME -> region.setName(value);
            case DESTINATION_AREA -> region.setDestAreaResref(value);
            case DESTINATION_ENTRANCE_NAME -> region.setEntranceNameInDestArea(value);
            case KEY -> region.setKeyResref(value);
            case SCRIPT -> region.setScriptResref(value);
            default -> throw new IllegalArgumentException();
        }
    }
}
