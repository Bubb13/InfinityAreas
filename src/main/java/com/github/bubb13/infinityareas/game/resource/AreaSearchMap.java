
package com.github.bubb13.infinityareas.game.resource;

import com.github.bubb13.infinityareas.misc.TaskTracker;
import com.github.bubb13.infinityareas.misc.TaskTrackerI;
import com.github.bubb13.infinityareas.misc.TrackedTask;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;

public class AreaSearchMap
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final ByteBuffer buffer;
    private BufferedImage image;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public AreaSearchMap(final ByteBuffer buffer)
    {
        this.buffer = buffer;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void load(final TaskTrackerI tracker) throws Exception
    {
        tracker.subtask(this::loadInternal);
    }

    public void load() throws Exception
    {
        loadInternal(TaskTracker.DUMMY);
    }

    public TrackedTask<Void> loadTask()
    {
        return new TrackedTask<>()
        {
            @Override
            protected Void doTask() throws Exception
            {
                subtask(AreaSearchMap.this::loadInternal);
                return null;
            }
        };
    }

    public BufferedImage getImage()
    {
        return image;
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void loadInternal(final TaskTrackerI tracker) throws Exception
    {
        try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(buffer.array()))
        {
            final BufferedImage image = ImageIO.read(inputStream);

            if (!(image.getColorModel() instanceof IndexColorModel indexColorModel)
                || (indexColorModel.getPixelSize() != 4 && indexColorModel.getPixelSize() != 8))
            {
                throw new IllegalStateException("Search map .BMP does not have a 4/8 bit indexed palette.");
            }

            this.image = image;
        }
    }

    ////////////////////
    // Public Classes //
    ////////////////////

    public enum PixelType
    {
        IMPASSABLE_BLOCK_LOS_AND_FLIGHT,      //  0 Black
        SAND,                                 //  1 Dark Red     - WAL_04
        WOOD_1,                               //  2 Dark Green   - WAL_MT
        WOOD_2,                               //  3 Dark Yellow  - WAL_02
        STONE_1,                              //  4 Dark Blue    - WAL_05
        GRASS_1,                              //  5 Dark Magenta - WAL_06
        WATER_1,                              //  6 Dark Cyan    - WAL_01
        STONE_2,                              //  7 Gray         - WAL_03
        IMPASSABLE,                           //  8 Dark Gray
        WOOD_3,                               //  9 Red          - WAL_02
        WALL_IMPASSABLE_BLOCK_LOS_AND_FLIGHT, // 10 Green
        WATER_2,                              // 11 Yellow       - WAL_01
        WATER_IMPASSIBLE,                     // 12 Blue
        ROOF_IMPASSABLE_BLOCK_LOS_AND_FLIGHT, // 13 Magenta
        WORLD_MAP_EXIT_IMPASSABLE,            // 14 Cyan
        GRASS_2,                              // 15 White        - WAL_04
    }
}
