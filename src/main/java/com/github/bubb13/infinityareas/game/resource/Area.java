
package com.github.bubb13.infinityareas.game.resource;

import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.gui.editor.GenericPolygon;
import com.github.bubb13.infinityareas.misc.ImageAndGraphics;
import com.github.bubb13.infinityareas.misc.ReferenceHolder;
import com.github.bubb13.infinityareas.misc.ReferenceTrackable;
import com.github.bubb13.infinityareas.misc.ReferenceTracker;
import com.github.bubb13.infinityareas.misc.TaskTracker;
import com.github.bubb13.infinityareas.misc.TaskTrackerI;
import com.github.bubb13.infinityareas.misc.TrackedTask;
import com.github.bubb13.infinityareas.misc.TrackingOrderedInstanceSet;
import com.github.bubb13.infinityareas.util.BufferUtil;
import com.github.bubb13.infinityareas.util.MiscUtil;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Stack;

public class Area
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final Game.ResourceSource source;
    private final ArrayList<Actor> actors = new ArrayList<>();
    private final TrackingOrderedInstanceSet<Region> regions = new TrackingOrderedInstanceSet<>();
    private final Stack<Integer> bufferMarks = new Stack<>();

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

    public Iterable<Actor> actors()
    {
        return MiscUtil.readOnlyIterable(actors);
    }

    public void addRegion(final Region region)
    {
        regions.addTail(region);
    }

    public Iterable<Region> regions()
    {
        return MiscUtil.readOnlyIterable(regions);
    }

    public Graphics newGraphics(final ImageAndGraphics imageAndGraphics)
    {
        return new Graphics(imageAndGraphics);
    }

    public Graphics newGraphics()
    {
        final WED.Overlay baseOverlay = wed.getOverlays().get(0);
        final BufferedImage image = new BufferedImage(
            baseOverlay.getWidthInTiles() * 64,
            baseOverlay.getHeightInTiles() * 64,
            BufferedImage.TYPE_INT_ARGB
        );
        return new Graphics(new ImageAndGraphics(image, image.createGraphics()));
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

        position(0x5A);
        final short numRegions = buffer.getShort();
        final int regionsOffset = buffer.getInt();

        position(0x7C);
        final int verticesOffset = buffer.getInt();
        final short numVertices = buffer.getShort();

        parseActors(tracker, actorsOffset, numActors);
        parseRegions(tracker, regionsOffset, numRegions, verticesOffset);
    }

    private void parseActors(
        final TaskTrackerI tracker, final int actorsOffset, final short numActors)
    {
        tracker.updateMessage("Processing actors ...");
        tracker.updateProgress(0, numActors);

        int curActorOffset = actorsOffset;

        for (int i = 0; i < numActors; ++i, curActorOffset += 0x110, tracker.updateProgress(i, numActors))
        {
            position(curActorOffset);

            final String name = BufferUtil.readLUTF8(buffer, 32);
            final short x = buffer.getShort();
            final short y = buffer.getShort();

            position(curActorOffset + 0x34);
            final short orientation = buffer.getShort();

            actors.add(new Actor(name, x, y, orientation));
        }
    }

    private void parseRegions(
        final TaskTrackerI tracker, final int regionsOffset, final short numRegions, final int verticesOffset)
    {
        tracker.updateMessage("Processing regions ...");
        tracker.updateProgress(0, numRegions);

        position(regionsOffset);
        for (int i = 0; i < numRegions; ++i, tracker.updateProgress(i, numRegions))
        {
            regions.addTail(parseRegion(verticesOffset));
        }
    }

    private Region parseRegion(final int verticesOffset)
    {
        final Region region = new Region();

        region.setName(BufferUtil.readLUTF8(buffer, 32));
        region.setType(buffer.getShort());
        final short boundingBoxLeft = buffer.getShort();
        final short boundingBoxTop = buffer.getShort();
        final short boundingBoxRight = buffer.getShort();
        final short boundingBoxBottom = buffer.getShort();
        final short numVertices = buffer.getShort();
        final int firstVertexIndex = buffer.getInt();

        final GenericPolygon polygon = new GenericPolygon(
            boundingBoxLeft, boundingBoxRight,
            boundingBoxTop, boundingBoxBottom
        );
        parseVertices(polygon, verticesOffset, firstVertexIndex, numVertices);
        region.setPolygon(polygon);

        region.setTriggerValue(buffer.getInt());
        region.setCursorIndex(buffer.getInt());
        region.setDestAreaResref(BufferUtil.readLUTF8(buffer, 8));
        region.setEntranceNameInDestArea(BufferUtil.readLUTF8(buffer, 32));
        region.setFlags(buffer.getInt());
        region.setInfoStrref(buffer.getInt());
        region.setTrapDetectionDifficulty(buffer.getShort());
        region.setTrapDisarmDifficulty(buffer.getShort());
        region.setbTrapped(buffer.getShort());
        region.setbTrapDetected(buffer.getShort());
        region.setTrapLaunchPointX(buffer.getShort());
        region.setTrapLaunchPointY(buffer.getShort());
        region.setKeyResref(BufferUtil.readLUTF8(buffer, 8));
        region.setScriptResref(BufferUtil.readLUTF8(buffer, 8));
        region.setActivationPointX(buffer.getShort());
        region.setActivationPointY(buffer.getShort());
        final byte[] unknown = new byte[0x3C]; buffer.get(unknown);
        region.setUnknown(unknown);

        return region;
    }

    private void parseVertices(
        final GenericPolygon polygon,
        final int verticesOffset, final int firstVertexIndex, final short numVertices)
    {
        final int firstVertexOffset = verticesOffset + 4 * firstVertexIndex;

        mark();
        position(firstVertexOffset);
        for (int i = 0; i < numVertices; ++i)
        {
            final short x = buffer.getShort();
            final short y = buffer.getShort();
            polygon.addVertex(x, y);
        }
        reset();
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

    private void mark()
    {
        bufferMarks.push(buffer.position());
    }

    private void reset()
    {
        buffer.position(bufferMarks.pop());
    }

    ////////////////////
    // Public Classes //
    ////////////////////

    public class Graphics
    {
        private final BufferedImage image;
        private final Graphics2D graphics;
        private final WED.Graphics wedGraphics;
        private double zoomFactor = 1;

        public Graphics(final ImageAndGraphics imageAndGraphics)
        {
            this.image = imageAndGraphics.image();
            this.graphics = imageAndGraphics.graphics();
            wedGraphics = wed.newGraphics(imageAndGraphics);
        }

        public Graphics drawActors()
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

    public class Actor
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private String name;
        private short x;
        private short y;
        private short orientation;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public Actor(final String name, final short x, final short y, final short orientation)
        {
            this.name = name;
            this.x = x;
            this.y = y;
            this.orientation = orientation;
        }

        ///////////////////////////
        // Public Static Methods //
        ///////////////////////////

        public static double orientationToDegree(final short orientation)
        {
            return switch (orientation)
            {
                case 0 -> 270;  // S
                case 1 -> 255;  // SSW
                case 2 -> 225;  // SW
                case 3 -> 195;  // SWW
                case 4 -> 180;  // W
                case 5 -> 165;  // NWW
                case 6 -> 135;  // NW
                case 7 -> 105;  // NNW
                case 8 -> 90;   // N
                case 9 -> 75;   // NNE
                case 10 -> 45;  // NE
                case 11 -> 15;  // NEE
                case 12 -> 0;   // E
                case 13 -> 345; // SEE
                case 14 -> 315; // SE
                case 15 -> 285; // SSE
                default -> throw new IllegalStateException();
            };
        }

        ////////////////////
        // Public Methods //
        ////////////////////

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

        public double getOrientationDegree()
        {
            return orientationToDegree(orientation);
        }
    }

    public class Region implements ReferenceTrackable
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        // Mirrors in-file data
        private String name;
        private short type;
        private GenericPolygon polygon;
        private int triggerValue;
        private int cursorIndex; // CURSORS.BAM
        private String destAreaResref; // For type=2
        private String entranceNameInDestArea; // For type=2
        private int flags;
        private int infoStrref; // For type=1
        private short trapDetectionDifficulty;
        private short trapDisarmDifficulty;
        private short bTrapped; // 0 = No, 1 = Yes
        private short bTrapDetected; // 0 = No, 1 = Yes
        private short trapLaunchPointX;
        private short trapLaunchPointY;
        private String keyResref; // For type=?
        private String scriptResref; // For type=?
        private short activationPointX;
        private short activationPointY;
        private byte[] unknown;

        // Extra
        private final ReferenceTracker referenceTracker = new ReferenceTracker(this);

        ////////////////////
        // Public Methods //
        ////////////////////

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public short getType()
        {
            return type;
        }

        public void setType(final short type)
        {
            this.type = type;
        }

        public GenericPolygon getPolygon()
        {
            return polygon;
        }

        public void setPolygon(GenericPolygon polygon)
        {
            this.polygon = polygon;
        }

        public int getTriggerValue()
        {
            return triggerValue;
        }

        public void setTriggerValue(final int triggerValue)
        {
            this.triggerValue = triggerValue;
        }

        public int getCursorIndex()
        {
            return cursorIndex;
        }

        public void setCursorIndex(final int cursorIndex)
        {
            this.cursorIndex = cursorIndex;
        }

        public String getDestAreaResref()
        {
            return destAreaResref;
        }

        public void setDestAreaResref(final String destAreaResref)
        {
            this.destAreaResref = destAreaResref;
        }

        public String getEntranceNameInDestArea()
        {
            return entranceNameInDestArea;
        }

        public void setEntranceNameInDestArea(final String entranceNameInDestArea)
        {
            this.entranceNameInDestArea = entranceNameInDestArea;
        }

        public int getFlags()
        {
            return flags;
        }

        public void setFlags(final int flags)
        {
            this.flags = flags;
        }

        public int getInfoStrref()
        {
            return infoStrref;
        }

        public void setInfoStrref(final int infoStrref)
        {
            this.infoStrref = infoStrref;
        }

        public short getTrapDetectionDifficulty()
        {
            return trapDetectionDifficulty;
        }

        public void setTrapDetectionDifficulty(final short trapDetectionDifficulty)
        {
            this.trapDetectionDifficulty = trapDetectionDifficulty;
        }

        public short getTrapDisarmDifficulty()
        {
            return trapDisarmDifficulty;
        }

        public void setTrapDisarmDifficulty(final short trapDisarmDifficulty)
        {
            this.trapDisarmDifficulty = trapDisarmDifficulty;
        }

        public short getbTrapped()
        {
            return bTrapped;
        }

        public void setbTrapped(final short bTrapped)
        {
            this.bTrapped = bTrapped;
        }

        public short getbTrapDetected()
        {
            return bTrapDetected;
        }

        public void setbTrapDetected(final short bTrapDetected)
        {
            this.bTrapDetected = bTrapDetected;
        }

        public short getTrapLaunchPointX()
        {
            return trapLaunchPointX;
        }

        public void setTrapLaunchPointX(final short trapLaunchPointX)
        {
            this.trapLaunchPointX = trapLaunchPointX;
        }

        public short getTrapLaunchPointY()
        {
            return trapLaunchPointY;
        }

        public void setTrapLaunchPointY(final short trapLaunchPointY)
        {
            this.trapLaunchPointY = trapLaunchPointY;
        }

        public String getKeyResref()
        {
            return keyResref;
        }

        public void setKeyResref(final String keyResref)
        {
            this.keyResref = keyResref;
        }

        public String getScriptResref()
        {
            return scriptResref;
        }

        public void setScriptResref(final String scriptResref)
        {
            this.scriptResref = scriptResref;
        }

        public short getActivationPointX()
        {
            return activationPointX;
        }

        public void setActivationPointX(final short activationPointX)
        {
            this.activationPointX = activationPointX;
        }

        public short getActivationPointY()
        {
            return activationPointY;
        }

        public void setActivationPointY(final short activationPointY)
        {
            this.activationPointY = activationPointY;
        }

        public byte[] getUnknown()
        {
            return unknown;
        }

        public void setUnknown(final byte[] unknown)
        {
            this.unknown = unknown;
        }

        @Override
        public void addedTo(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.addedTo(referenceHolder);
        }

        @Override
        public void removedFrom(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.removedFrom(referenceHolder);
        }

        @Override
        public void delete()
        {
            referenceTracker.delete();
        }
    }
}
