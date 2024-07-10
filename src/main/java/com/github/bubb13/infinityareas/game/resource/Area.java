
package com.github.bubb13.infinityareas.game.resource;

import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.misc.TaskTracker;
import com.github.bubb13.infinityareas.misc.TaskTrackerI;
import com.github.bubb13.infinityareas.misc.TrackedTask;
import com.github.bubb13.infinityareas.util.BufferUtil;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class Area
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final Game.ResourceSource source;

    private ByteBuffer buffer;
    private WED wed;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public Area(final Game.ResourceSource source)
    {
        this.source = source;
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
                subtask(Area.this::loadInternal);
                return null;
            }
        };
    }

    public int getOverlayCount()
    {
        return wed.getOverlays().size();
    }

    public TrackedTask<BufferedImage> renderOverlaysTask(final int... overlayIndexes)
    {
        return wed.renderOverlaysTask(overlayIndexes);
    }

    public BufferedImage renderOverlays(final TaskTrackerI tracker, final int... overlayIndexes) throws Exception
    {
        final WED.WEDGraphics wedGraphics = wed.newGraphics();
        wedGraphics.renderOverlays(tracker, overlayIndexes);
        return wedGraphics.getImage();
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void position(final int pos)
    {
        buffer.position(pos);
    }

    ///////////////////////
    // START Loading ARE //
    ///////////////////////

    private void loadInternal(final TaskTrackerI tracker) throws Exception
    {
        buffer = source.demandFileData();

        tracker.updateProgress(0, 100);
        tracker.updateMessage("Processing area ...");

        position(0x0);

        final String signature = BufferUtil.readUTF8(buffer, 4);
        if (!signature.equals("AREA"))
        {
            throw new IllegalStateException("Invalid ARE signature: \"" + signature + "\"");
        }

        final String version = BufferUtil.readUTF8(buffer, 4);
        if (version.equals("V1.0"))
        {
            parse_V1_0(tracker);
        }
        else if (version.equals("V9.1"))
        {
            parse_V9_1(tracker);
        }
        else
        {
            throw new IllegalStateException("Invalid ARE version: \"" + version + "\"");
        }
    }

    private void parse_V1_0(final TaskTrackerI tracker) throws Exception
    {
        loadWED(tracker);
    }

    private void parse_V9_1(final TaskTrackerI tracker) throws Exception
    {
        loadWED(tracker);
    }

    private void loadWED(final TaskTrackerI tracker) throws Exception
    {
        position(0x8);
        final String wedResref = BufferUtil.readLUTF8(buffer, 8);
        final ResourceIdentifier wedIdentifier = new ResourceIdentifier(wedResref, KeyFile.NumericResourceType.WED);
        final Game.Resource wedResource = GlobalState.getGame().getResource(wedIdentifier);

        if (wedResource == null)
        {
            throw new IllegalStateException("Unable to find source for WED resource \"" + wedResref + "\"");
        }

        final WED tempWed = new WED(wedResource.getPrimarySource());
        tempWed.load(tracker);
        wed = tempWed;
    }

    /////////////////////
    // END Loading ARE //
    /////////////////////
}
