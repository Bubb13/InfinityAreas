
package com.github.bubb13.infinityareas.game.resource;

import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.misc.ImageAndGraphics;
import com.github.bubb13.infinityareas.misc.TaskTracker;
import com.github.bubb13.infinityareas.misc.TaskTrackerI;
import com.github.bubb13.infinityareas.misc.TrackedTask;
import com.github.bubb13.infinityareas.util.BufferUtil;
import com.github.bubb13.infinityareas.util.MiscUtil;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Area
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final Game.ResourceSource source;
    private final ArrayList<Actor> actors = new ArrayList<>();

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
        final WED.Graphics wedGraphics = wed.newGraphics();
        wedGraphics.renderOverlays(tracker, overlayIndexes);
        return wedGraphics.getImage();
    }

    public Iterable<Actor> getActors()
    {
        return MiscUtil.readOnlyIterable(actors);
    }

    public AreaGraphics newGraphics(final ImageAndGraphics imageAndGraphics)
    {
        return new AreaGraphics(imageAndGraphics);
    }

    public AreaGraphics newGraphics()
    {
        final WED.Overlay baseOverlay = wed.getOverlays().get(0);
        final BufferedImage image = new BufferedImage(
            baseOverlay.getWidthInTiles() * 64,
            baseOverlay.getHeightInTiles() * 64,
            BufferedImage.TYPE_INT_ARGB
        );
        return new AreaGraphics(new ImageAndGraphics(image, image.createGraphics()));
    }

    public WED getWed()
    {
        return wed;
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void position(final int pos)
    {
        buffer.position(pos);
    }

    //-------------------//
    // START Loading ARE //
    //-------------------//

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

        position(0x54);
        final int actorsOffset = buffer.getInt();
        final short numActors = buffer.getShort();

        parseActors(tracker, actorsOffset, numActors);
    }

    private void parseActors(
        final TaskTrackerI tracker, final int actorsOffset, final short numActors) throws Exception
    {
        tracker.updateMessage("Processing actors ...");

        int curActorOffset = actorsOffset;

        for (int i = 0; i < numActors; ++i, curActorOffset += 0x110)
        {
            tracker.updateProgress(i, numActors);
            position(curActorOffset);

            final String name = BufferUtil.readLUTF8(buffer, 32);
            final short x = buffer.getShort();
            final short y = buffer.getShort();

            position(curActorOffset + 0x34);
            final short orientation = buffer.getShort();

            actors.add(new Actor(name, x, y, orientation));


        }
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

    //-----------------//
    // END Loading ARE //
    //-----------------//

    ////////////////////
    // Public Classes //
    ////////////////////

    public class AreaGraphics
    {
        private final BufferedImage image;
        private final Graphics2D graphics;
        private final WED.Graphics wedGraphics;
        private double zoomFactor = 1;

        public AreaGraphics(final ImageAndGraphics imageAndGraphics)
        {
            this.image = imageAndGraphics.image();
            this.graphics = imageAndGraphics.graphics();
            wedGraphics = wed.newGraphics(imageAndGraphics);
        }

        public AreaGraphics drawActors()
        {
//            graphics.setStroke(new BasicStroke((int)(2 * zoomFactor)));
//            graphics.setFont(new Font("Times New Roman", Font.PLAIN, (int)(18 * zoomFactor)));
//
//            for (final Actor actor : actors)
//            {
//                graphics.drawString(actor.getName(), actor.getX(), actor.getY());
//                graphics.drawOval(actor.getX(), actor.getY(), 100, 100);
//            }

            return this;
        }

        public WED.Graphics getWedGraphics()
        {
            return wedGraphics;
        }

        public BufferedImage getImage()
        {
            return image;
        }

        public void setZoomFactor(final double zoomFactor)
        {
            this.zoomFactor = zoomFactor;
        }

        public void drawImage(final BufferedImage image, final int x, final int y)
        {
            graphics.drawImage(image, x, y, null);
        }
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    public class Actor
    {
        String name;
        short x;
        short y;
        short orientation;

        public Actor(final String name, final short x, final short y, final short orientation)
        {
            this.name = name;
            this.x = x;
            this.y = y;
            this.orientation = orientation;
        }

        public String getName()
        {
            return name;
        }

        public void setName(final String name)
        {
            this.name = name;
        }

        public short getX()
        {
            return x;
        }

        public void setX(final short x)
        {
            this.x = x;
        }

        public short getY()
        {
            return y;
        }

        public void setY(final short y)
        {
            this.y = y;
        }

        public short getOrientation()
        {
            return orientation;
        }

        public void setOrientation(short orientation)
        {
            this.orientation = orientation;
        }
    }
}
