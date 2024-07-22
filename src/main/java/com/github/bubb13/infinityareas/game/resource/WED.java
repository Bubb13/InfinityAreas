
package com.github.bubb13.infinityareas.game.resource;

import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.misc.AppendOnlyOrderedInstanceSet;
import com.github.bubb13.infinityareas.misc.ImageAndGraphics;
import com.github.bubb13.infinityareas.misc.InstanceHashMap;
import com.github.bubb13.infinityareas.misc.ReferenceTrackable;
import com.github.bubb13.infinityareas.misc.SimpleCache;
import com.github.bubb13.infinityareas.misc.SimpleLinkedList;
import com.github.bubb13.infinityareas.misc.TaskTracker;
import com.github.bubb13.infinityareas.misc.TaskTrackerI;
import com.github.bubb13.infinityareas.misc.TrackedTask;
import com.github.bubb13.infinityareas.misc.TrackingOrderedInstanceSet;
import com.github.bubb13.infinityareas.util.BufferUtil;
import com.github.bubb13.infinityareas.util.MiscUtil;
import com.github.bubb13.infinityareas.util.TileUtil;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

public class WED
{
    ///////////////////////////
    // Private Static Fields //
    ///////////////////////////

    private static final int WED_OVERLAY_ENTRY_SIZE = 0x18;
    private static final int WED_TILEMAP_ENTRY_SIZE = 0xA;

    ////////////////////
    // Private Fields //
    ////////////////////

    private final Game.ResourceSource source;
    private final Stack<Integer> bufferMarks = new Stack<>();
    private ByteBuffer buffer;

    private final ArrayList<Overlay> overlays = new ArrayList<>();

    private final ArrayList<TiledObject> tiledObjects = new ArrayList<>();
    private final InstanceHashMap<Polygon, TiledObject> tiledObjectByReferencedPolygon = new InstanceHashMap<>();
    private final TrackingOrderedInstanceSet<Polygon> polygons = new TrackingOrderedInstanceSet<>();

    private final ResourceDataCache resourceDataCache = new ResourceDataCache();
    private final SimpleCache<String, PVRZ> pvrzCache = new SimpleCache<>();
    private final SimpleCache<String, TIS> tisCache = new SimpleCache<>();

    private boolean changed = false;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public WED(final Game.ResourceSource source)
    {
        this.source = source;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public Game.ResourceSource getSource()
    {
        return source;
    }

    public TrackedTask<BufferedImage> renderOverlaysTask(final int... overlayIndexes)
    {
        return new RenderOverlaysTask(overlayIndexes);
    }

    public TrackedTask<Void> saveWEDTask(final Path path)
    {
        return new SaveWEDTask(path);
    }

    public List<Overlay> getOverlays()
    {
        return overlays;
    }

    public Iterable<Polygon> getPolygons()
    {
        return MiscUtil.readOnlyIterable(polygons);
    }

    public void addPolygon(final Polygon polygon)
    {
        polygons.addTail(polygon);
    }

    public boolean checkAndClearChanged()
    {
        final boolean temp = changed;
        changed = false;
        return temp;
    }

    public Graphics newGraphics(final ImageAndGraphics imageAndGraphics)
    {
        return new Graphics(imageAndGraphics);
    }

    public Graphics newGraphics()
    {
        final Overlay baseOverlay = overlays.get(0);
        final BufferedImage image = new BufferedImage(
            baseOverlay.getWidthInTiles() * 64,
            baseOverlay.getHeightInTiles() * 64,
            BufferedImage.TYPE_INT_ARGB
        );
        return new Graphics(new ImageAndGraphics(image, image.createGraphics()));
    }

    public void load(final TaskTrackerI tracker) throws Exception
    {
        tracker.subtask(this::loadInternal);
    }

    public void load() throws Exception
    {
        loadInternal(TaskTracker.DUMMY);
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void position(final int pos)
    {
        buffer.position(pos);
    }

    private void mark()
    {
        bufferMarks.push(buffer.position());
    }

    private void reset()
    {
        buffer.position(bufferMarks.pop());
    }

    private void changed()
    {
        changed = true;
    }

    private WallGroupDimensions calculateWallGroupDimensions()
    {
        final Overlay baseOverlay = overlays.get(0);
        final short baseOverlayWidth = baseOverlay.getWidthInTiles();
        final short baseOverlayHeight = baseOverlay.getHeightInTiles();
        final short wallGroupCountX = (short)MiscUtil.divideRoundUp(baseOverlayWidth, 10);
        final short wallGroupCountY = (short)MiscUtil.multiplyByRatioRoundUp(baseOverlayHeight, 2, 15);
        return new WallGroupDimensions(wallGroupCountX, wallGroupCountY);
    }

    private int calculateNumberOfWallGroups()
    {
        final WallGroupDimensions wallGroupDimensions = calculateWallGroupDimensions();
        return (int)wallGroupDimensions.widthInTiles() * wallGroupDimensions.heightInTiles();
    }

    ////////////////////
    // Public Classes //
    ////////////////////

    public record SecondaryHeaderInfo(
        int polygonsOffset, short maxReferencedPolygonIndex, int verticesOffset
    ) {}

    public class Overlay
    {
        private short widthInTiles;
        private short heightInTiles;
        private String tilesetResref;
        private short uniqueTileCount;
        private short movementType;
        private final ArrayList<TilemapEntry> tilemapEntries;

        public Overlay(
            final short widthInTiles, final short heightInTiles, final String tilesetResref,
            final short uniqueTileCount, final short movementType, final ArrayList<TilemapEntry> tilemapEntries)
        {
            this.widthInTiles = widthInTiles;
            this.heightInTiles = heightInTiles;
            this.tilesetResref = tilesetResref;
            this.uniqueTileCount = uniqueTileCount;
            this.movementType = movementType;
            this.tilemapEntries = tilemapEntries;
        }

        public short getWidthInTiles()
        {
            return widthInTiles;
        }

        public void setWidthInTiles(final short widthInTiles)
        {
            this.widthInTiles = widthInTiles;
            changed();
        }

        public short getHeightInTiles()
        {
            return heightInTiles;
        }

        public void setHeightInTiles(final short heightInTiles)
        {
            this.heightInTiles = heightInTiles;
            changed();
        }

        public String getTilesetResref()
        {
            return tilesetResref;
        }

        public void setTilesetResref(final String tilesetResref)
        {
            this.tilesetResref = tilesetResref;
            changed();
        }

        public short getUniqueTileCount()
        {
            return uniqueTileCount;
        }

        public void setUniqueTileCount(final short uniqueTileCount)
        {
            this.uniqueTileCount = uniqueTileCount;
            changed();
        }

        public short getMovementType()
        {
            return movementType;
        }

        public void setMovementType(final short movementType)
        {
            this.movementType = movementType;
            changed();
        }

        public ArrayList<TilemapEntry> getTilemapEntries()
        {
            return tilemapEntries;
        }

        public void dump()
        {
            System.out.println("  Width (tiles): " + widthInTiles);
            System.out.println("  Height (tiles): " + heightInTiles);
            System.out.println("  TIS resref: \"" + tilesetResref + "\"");
            System.out.println("  Unique tile count: " + uniqueTileCount);
            System.out.println("  Movement type: " + movementType);

            System.out.println("  Tilemap Entries:");
            for (int i = 0; i < tilemapEntries.size(); ++i)
            {
                System.out.println("    [Tilemap " + i + "]");
                final TilemapEntry tilemapEntry = tilemapEntries.get(i);
                System.out.println("      Alternate Tile TIS Index: " + tilemapEntry.secondaryTisTileIndex);
                System.out.println("      Draw Flags: " + tilemapEntry.drawFlags);

                System.out.println("      Tile Index Lookup Table:");

                final short[] tileIndexLookupArray = tilemapEntry.tisTileIndexArray;
                for (final short value : tileIndexLookupArray)
                {
                    System.out.println("        " + value);
                }
            }
        }
    }

    public class TilemapEntry
    {
        private short secondaryTisTileIndex;
        private byte drawFlags;
        private byte animationSpeed;

        /**
         * <pre>
         * A bitfield with a single meaningful flag. Only checked for the base overlay (0).
         * 0x2
         *   If secondary tile != -1, render secondary tile
         *   If secondary tile == -1, behave as if !0x2
         * !0x2
         * {@code
         *   byte nAnimSpeed = max(1, pBaseOverlayTileData->bAnimSpeed);
         *   size_t nTileIndex = pBaseOverlayTileData->nStartingTile + ((nGameTime / nAnimSpeed) % pBaseOverlayTileData->nNumTiles);
         * }
         *
         * If this TilemapEntry is linked to a TiledObject, the engine sets the field to 1 or 2 based on whether
         * the TiledObject is in its primary (1) or secondary (2) state.
         * <br><br>
         * </pre>
         */
        private short extraFlags;

        private short[] tisTileIndexArray;

        public TilemapEntry(
            final short secondaryTisTileIndex, final byte drawFlags, final byte animationSpeed,
            final short extraFlags, final short[] tisTileIndexArray)
        {
            this.secondaryTisTileIndex = secondaryTisTileIndex;
            this.drawFlags = drawFlags;
            this.animationSpeed = animationSpeed;
            this.extraFlags = extraFlags;
            this.tisTileIndexArray = tisTileIndexArray;
        }

        public short getSecondaryTisTileIndex()
        {
            return secondaryTisTileIndex;
        }

        public void setSecondaryTisTileIndex(final short secondaryTisTileIndex)
        {
            this.secondaryTisTileIndex = secondaryTisTileIndex;
            changed();
        }

        public byte getDrawFlags()
        {
            return drawFlags;
        }

        public void setDrawFlags(final byte drawFlags)
        {
            this.drawFlags = drawFlags;
            changed();
        }

        public byte getAnimationSpeed()
        {
            return animationSpeed;
        }

        public void setAnimationSpeed(final byte animationSpeed)
        {
            this.animationSpeed = animationSpeed;
            changed();
        }

        public short getExtraFlags()
        {
            return extraFlags;
        }

        public void setExtraFlags(final short extraFlags)
        {
            this.extraFlags = extraFlags;
            changed();
        }

        public short[] getTisTileIndexArray()
        {
            return tisTileIndexArray;
        }

        public void setTisTileIndexArray(final short[] tisTileIndexArray)
        {
            this.tisTileIndexArray = tisTileIndexArray;
            changed();
        }
    }

    public static class Vertex
    {
        private final SimpleLinkedList<Vertex>.Node node;
        private short x;
        private short y;

        public Vertex(final SimpleLinkedList<Vertex>.Node node, final short x, final short y)
        {
            this.node = node;
            this.x = x;
            this.y = y;
        }

        public short x()
        {
            return x;
        }

        public void setX(final short x)
        {
            this.x = x;
        }

        public short y()
        {
            return y;
        }

        public void setY(final short y)
        {
            this.y = y;
        }

        public SimpleLinkedList<Vertex>.Node getNode()
        {
            return node;
        }
    }

    public static class Polygon extends ReferenceTrackable
    {
        // In-file
        private byte flags;
        private byte height;
        private short boundingBoxLeft;
        private short boundingBoxRight;
        private short boundingBoxTop;
        private short boundingBoxBottom;

        // Derived
        private final SimpleLinkedList<Vertex> vertices;

        public Polygon(
            final byte flags, final byte height,
            final short boundingBoxLeft, final short boundingBoxRight,
            final short boundingBoxTop, final short boundingBoxBottom,
            final SimpleLinkedList<Vertex> vertices)
        {
            this.flags = flags;
            this.height = height;
            this.boundingBoxLeft = boundingBoxLeft;
            this.boundingBoxRight = boundingBoxRight;
            this.boundingBoxTop = boundingBoxTop;
            this.boundingBoxBottom = boundingBoxBottom;
            this.vertices = vertices;
        }

        public Vertex addVertex(final short x, final short y)
        {
            return vertices.addTail((node) -> new Vertex(node, x, y)).value();
        }

        public byte getFlags()
        {
            return flags;
        }

        public void setFlags(final byte flags)
        {
            this.flags = flags;
        }

        public byte getHeight()
        {
            return height;
        }

        public void setHeight(final byte height)
        {
            this.height = height;
        }

        public short getBoundingBoxLeft()
        {
            return boundingBoxLeft;
        }

        public void setBoundingBoxLeft(final short boundingBoxLeft)
        {
            this.boundingBoxLeft = boundingBoxLeft;
        }

        public short getBoundingBoxRight()
        {
            return boundingBoxRight;
        }

        public void setBoundingBoxRight(final short boundingBoxRight)
        {
            this.boundingBoxRight = boundingBoxRight;
        }

        public short getBoundingBoxTop()
        {
            return boundingBoxTop;
        }

        public void setBoundingBoxTop(final short boundingBoxTop)
        {
            this.boundingBoxTop = boundingBoxTop;
        }

        public short getBoundingBoxBottom()
        {
            return boundingBoxBottom;
        }

        public void setBoundingBoxBottom(final short boundingBoxBottom)
        {
            this.boundingBoxBottom = boundingBoxBottom;
        }

        public SimpleLinkedList<Vertex> getVertices()
        {
            return vertices;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Polygon polygon = (Polygon) o;
            return flags == polygon.flags && height == polygon.height
                && boundingBoxLeft == polygon.boundingBoxLeft && boundingBoxRight == polygon.boundingBoxRight
                && boundingBoxTop == polygon.boundingBoxTop && boundingBoxBottom == polygon.boundingBoxBottom
                && Objects.equals(vertices, polygon.vertices);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(flags, height, boundingBoxLeft, boundingBoxRight,
                boundingBoxTop, boundingBoxBottom, vertices);
        }
    }

    public class TiledObject
    {
        private String resref; // Unused
        private short openOrClosed; // open = 0, closed = 1
        private final ArrayList<Short> tilemapIndices; // TODO
        private final TrackingOrderedInstanceSet<Polygon> openPolygons;
        private final TrackingOrderedInstanceSet<Polygon> closedPolygons;

        public TiledObject(
            final String resref, final short openOrClosed,
            final ArrayList<Short> tilemapIndices,
            final TrackingOrderedInstanceSet<Polygon> openPolygons,
            final TrackingOrderedInstanceSet<Polygon> closedPolygons)
        {
            this.resref = resref;
            this.openOrClosed = openOrClosed;
            this.tilemapIndices = tilemapIndices;
            this.openPolygons = openPolygons;
            this.closedPolygons = closedPolygons;
        }

        public String getResref()
        {
            return resref;
        }

        public void setResref(final String resref)
        {
            this.resref = resref;
            changed();
        }

        public short getOpenOrClosed()
        {
            return openOrClosed;
        }

        public void setOpenOrClosed(final short openOrClosed)
        {
            this.openOrClosed = openOrClosed;
            changed();
        }

        public ArrayList<Short> getTilemapIndices()
        {
            return tilemapIndices;
        }

        public TrackingOrderedInstanceSet<Polygon> getOpenPolygons()
        {
            return openPolygons;
        }

        public TrackingOrderedInstanceSet<Polygon> getClosedPolygons()
        {
            return closedPolygons;
        }
    }

    ///////////////////////
    // START Loading WED //
    ///////////////////////

    private void loadInternal(final TaskTrackerI tracker) throws Exception
    {
        buffer = source.demandFileData();

        tracker.updateProgress(0, 100);
        tracker.updateMessage("Processing WED ...");

        position(0x0);

        final String signature = BufferUtil.readUTF8(buffer, 4);
        if (!signature.equals("WED "))
        {
            throw new IllegalStateException("Invalid WED signature: \"" + signature + "\"");
        }

        final String version = BufferUtil.readUTF8(buffer, 4);
        if (!version.equals("V1.3"))
        {
            throw new IllegalStateException("Invalid WED version: \"" + version + "\"");
        }

        final int numOverlays = buffer.getInt();
        final int numTiledObjects = buffer.getInt();
        final int overlaysOffset = buffer.getInt();
        final int secondaryHeaderOffset = buffer.getInt();
        final int tiledObjectsOffset = buffer.getInt();
        final int tiledObjectsTilemapIndicesOffset = buffer.getInt();

        parseOverlays(overlaysOffset, numOverlays);
        final SecondaryHeaderInfo secondaryHeaderInfo = parseSecondaryHeader(secondaryHeaderOffset);
        parseTiledObjects(tiledObjectsOffset, numTiledObjects,
            tiledObjectsTilemapIndicesOffset, secondaryHeaderInfo);
    }

    private void parseOverlays(final int offset, final int count) throws Exception
    {
        int curBase = offset;
        for (int i = 0; i < count; ++i)
        {
            position(curBase);        final short widthInTiles = buffer.getShort();
            position(curBase + 0x2);  final short heightInTiles = buffer.getShort();
            position(curBase + 0x4);  final String tilesetResref = BufferUtil.readLUTF8(buffer, 8);
            position(curBase + 0xC);  final short uniqueTileCount = buffer.getShort();
            position(curBase + 0xE);  final short movementType = buffer.getShort();
            position(curBase + 0x10); final int tilemapEntriesOffset = buffer.getInt();
            position(curBase + 0x14); final int tileIndexLookupTableOffset = buffer.getInt();

            final ArrayList<TilemapEntry> tilemapEntries = parseTilemapEntries(
                tilemapEntriesOffset, widthInTiles * heightInTiles, tileIndexLookupTableOffset);

            overlays.add(new Overlay(
                widthInTiles, heightInTiles, tilesetResref,
                uniqueTileCount, movementType, tilemapEntries
            ));

            curBase += WED_OVERLAY_ENTRY_SIZE;
        }
    }

    /**
     * Parses the per-overlay tilemap table.
     */
    private ArrayList<TilemapEntry> parseTilemapEntries(
        final int offset, final int count, final int tisTileIndexLookupTableOffset)
    {
        final ArrayList<TilemapEntry> tilemapEntries = new ArrayList<>();

        position(offset);
        for (int i = 0; i < count; ++i)
        {
            final short tisTileIndexLookupTableStartIndex = buffer.getShort();
            final short numTisTileIndexLookupTableEntries = buffer.getShort();
            final short secondaryTisTileIndex = buffer.getShort();
            final byte drawFlags = buffer.get();
            final byte animationSpeed = buffer.get();
            final short extraFlags = buffer.getShort();

            final short[] tisTileIndexArray = new short[numTisTileIndexLookupTableEntries];

            ////////////////////////////
            // Read tisTileIndexArray //
            ////////////////////////////

            mark();
            position(tisTileIndexLookupTableOffset + 2 * tisTileIndexLookupTableStartIndex);
            for (int j = 0; j < numTisTileIndexLookupTableEntries; ++j)
            {
                tisTileIndexArray[j] = buffer.getShort();
            }
            reset();

            /////////////////////////
            // Create TilemapEntry //
            /////////////////////////

            tilemapEntries.add(new TilemapEntry(
                secondaryTisTileIndex, drawFlags, animationSpeed, extraFlags, tisTileIndexArray));
        }

        return tilemapEntries;
    }

    private SecondaryHeaderInfo parseSecondaryHeader(final int offset)
    {
        position(offset);

        // Important:
        //   The engine doesn't care what this is set to, and outright ignores it.
        //   Certain WED files even store (and index) wall polygons past this number.
        //   Don't assume there are *only* this number of polygons!
        final int numPolygons = buffer.getInt();
        final int polygonsOffset = buffer.getInt();
        final int verticesOffset = buffer.getInt();
        final int wallGroupsOffset = buffer.getInt();
        final int polygonIndicesLookupTableOffset = buffer.getInt();

        // Wall group polygons used in CInfinity::FXRenderClippingPolys()
        final WallGroupsInfo wallGroupsInfo = parseWallGroups(
            wallGroupsOffset, polygonIndicesLookupTableOffset, polygonsOffset, verticesOffset
        );
        return new SecondaryHeaderInfo(polygonsOffset, wallGroupsInfo.maxReferencedPolygonIndex(), verticesOffset);
    }

    private Polygon readPolygon(final int verticesOffset)
    {
        final int vertexStartIndex = buffer.getInt();
        final int numVertices = buffer.getInt();
        final byte flags = buffer.get();
        final byte height = buffer.get();
        final short boundingBoxLeft = buffer.getShort();
        final short boundingBoxRight = buffer.getShort();
        final short boundingBoxTop = buffer.getShort();
        final short boundingBoxBottom = buffer.getShort();

        final SimpleLinkedList<Vertex> vertices = new SimpleLinkedList<>();

        // Read vertices
        mark();
        position(verticesOffset + vertexStartIndex * 4);
        for (int j = 0; j < numVertices; ++j)
        {
            final short vertexX = buffer.getShort();
            final short vertexY = buffer.getShort();
            vertices.addTail((node) -> new Vertex(node, vertexX, vertexY));
        }
        reset();

        return new Polygon(
            flags, height,
            boundingBoxLeft, boundingBoxRight,
            boundingBoxTop, boundingBoxBottom,
            vertices
        );
    }

    private WallGroupsInfo parseWallGroups(
        final int offset, final int polygonIndicesLookupTableOffset,
        final int polygonsOffset, final int verticesOffset)
    {
        ////////////////////////////////////
        // Find maxReferencedPolygonIndex //
        ////////////////////////////////////

        final int numWallGroups = calculateNumberOfWallGroups();
        short maxReferencedPolygonIndex = Short.MIN_VALUE;

        position(offset);
        for (int i = 0; i < numWallGroups; ++i)
        {
            final short startPolygonsIndexIndex = buffer.getShort();
            final short polygonIndexCount = buffer.getShort();

            // Read polygons indices
            mark();
            position(polygonIndicesLookupTableOffset + startPolygonsIndexIndex * 0x2);
            for (int j = 0; j < polygonIndexCount; ++j)
            {
                final short polygonIndex = buffer.getShort();
                if (polygonIndex > maxReferencedPolygonIndex)
                {
                    maxReferencedPolygonIndex = polygonIndex;
                }
            }
            reset();
        }

        ///////////////////
        // Read Polygons //
        ///////////////////

        position(polygonsOffset);
        for (int i = 0; i <= maxReferencedPolygonIndex; ++i)
        {
            polygons.addTail(readPolygon(verticesOffset));
        }

        return new WallGroupsInfo(maxReferencedPolygonIndex);
    }

    private void parseTiledObjects(
        final int offset, final int numTiledObjects, final int tiledObjectsTilemapIndicesOffset,
        final SecondaryHeaderInfo secondaryHeaderInfo)
    {
        position(offset);

        for (int i = 0; i < numTiledObjects; ++i)
        {
            final String resref = BufferUtil.readLUTF8(buffer, 8);
            final short openOrClosed = buffer.getShort();
            final short tiledObjectsTilemapStartIndex = buffer.getShort();
            final short numTiledObjectsTilemaps = buffer.getShort();
            final short numOpenPolygons = buffer.getShort();
            final short numClosedPolygons = buffer.getShort();
            final int openPolygonsOffset = buffer.getInt();
            final int closedPolygonsOffset = buffer.getInt();

            //////////////////////////////////////
            // Read tiledObjectsTilemapIndices //
            //////////////////////////////////////

            final ArrayList<Short> tiledObjectsTilemapIndices = new ArrayList<>();

            mark();
            position(tiledObjectsTilemapIndicesOffset + tiledObjectsTilemapStartIndex * 2);
            for (int j = 0; j < numTiledObjectsTilemaps; ++j)
            {
                final short tiledObjectsTilemapIndex = buffer.getShort();
                tiledObjectsTilemapIndices.add(tiledObjectsTilemapIndex);
            }
            reset();

            ///////////////////////
            // Read openPolygons //
            ///////////////////////

            final TrackingOrderedInstanceSet<Polygon> openPolygons = handleTiledObjectReferencedPolygons(
                secondaryHeaderInfo, i, "open", openPolygonsOffset, numOpenPolygons);

            /////////////////////////
            // Read closedPolygons //
            /////////////////////////

            final TrackingOrderedInstanceSet<Polygon> closedPolygons = handleTiledObjectReferencedPolygons(
                secondaryHeaderInfo, i, "closed", closedPolygonsOffset, numClosedPolygons);

            ////////////////////////
            // Create TiledObject //
            ////////////////////////

            final TiledObject tiledObject = new TiledObject(resref, openOrClosed,
                tiledObjectsTilemapIndices, openPolygons, closedPolygons);

            for (final Polygon openPolygon : openPolygons)
            {
                tiledObjectByReferencedPolygon.put(openPolygon, tiledObject);
            }

            for (final Polygon closedPolygon : closedPolygons)
            {
                tiledObjectByReferencedPolygon.put(closedPolygon, tiledObject);
            }

            tiledObjects.add(tiledObject);
        }
    }

    private TrackingOrderedInstanceSet<Polygon> handleTiledObjectReferencedPolygons(
        final SecondaryHeaderInfo secondaryHeaderInfo, final int tiledObjectI, final String polygonTypeName,
        final int polygonsOffset, final short numPolygons)
    {
        validateTiledObjectPolygonsOffset(secondaryHeaderInfo, tiledObjectI,
            polygonTypeName, polygonsOffset, numPolygons);

        final int polygonsStartIndex = (polygonsOffset - secondaryHeaderInfo.polygonsOffset()) / 0x12;
        final int polygonsStartIndexBounded = Math.max(0, polygonsStartIndex);
        final int polygonsEndIndexBounded = Math.min(
            polygonsStartIndex + numPolygons, secondaryHeaderInfo.maxReferencedPolygonIndex() + 1);

        final TrackingOrderedInstanceSet<Polygon> referencedPolygons = new TrackingOrderedInstanceSet<>();

        if (polygonsStartIndexBounded < polygonsEndIndexBounded)
        {
            var curNode = polygons.getNode(polygonsStartIndexBounded);
            for (int i = polygonsStartIndexBounded; i < polygonsEndIndexBounded; ++i, curNode = curNode.next())
            {
                referencedPolygons.addTail(curNode.value());
            }
        }

        return referencedPolygons;
    }

    private void validateTiledObjectPolygonsOffset(
        final SecondaryHeaderInfo secondaryHeaderInfo, final int tiledObjectIndex, final String openCloseLabel,
        final int startPolygonsOffset, final short numPolygons)
    {
        if (numPolygons == 0)
        {
            return;
        }

        final int startWallPolygonsOffset = secondaryHeaderInfo.polygonsOffset();
        final int afterWallPolygonsOffset = startWallPolygonsOffset
            + (secondaryHeaderInfo.maxReferencedPolygonIndex() + 1) * 0x12;

        Integer specialNegativeInfringingPolygonIndex = null;
        Integer firstInfringingPolygonIndex = null;
        int lastInfringingPolygonIndex;

        if (startPolygonsOffset < startWallPolygonsOffset || startPolygonsOffset >= afterWallPolygonsOffset)
        {
            firstInfringingPolygonIndex = (startPolygonsOffset - startWallPolygonsOffset) / 0x12;
        }

        final int afterPolygonsOffset = startPolygonsOffset + numPolygons * 0x12;

        if (afterPolygonsOffset > afterWallPolygonsOffset)
        {
            if (firstInfringingPolygonIndex != null && firstInfringingPolygonIndex < 0)
            {
                // Two invalid groups, one before the table, and one after
                specialNegativeInfringingPolygonIndex = firstInfringingPolygonIndex;
            }

            if (firstInfringingPolygonIndex == null || firstInfringingPolygonIndex < 0)
            {
                // One invalid group, starts after the table
                firstInfringingPolygonIndex = (afterWallPolygonsOffset - startWallPolygonsOffset) / 0x12;
            }

            lastInfringingPolygonIndex = (afterPolygonsOffset - startWallPolygonsOffset) / 0x12 - 1;
        }
        else
        {
            if (firstInfringingPolygonIndex == null)
            {
                // All polygons are valid
                return;
            }
            // One invalid group, starts before the table
            lastInfringingPolygonIndex = -1;
        }

        int numInfringing = lastInfringingPolygonIndex - firstInfringingPolygonIndex + 1;
        if (specialNegativeInfringingPolygonIndex != null)
        {
            numInfringing += -1 - specialNegativeInfringingPolygonIndex + 1;
        }

        if (numInfringing < numPolygons)
        {
            System.out.printf(
                "[%s.WED] Tiled object %d references %s polygons which are not part of the wall polygons table, " +
                "but has at least one valid polygon\n",
                source.getIdentifier().resref(), tiledObjectIndex, openCloseLabel);
        }
        else
        {
            if (specialNegativeInfringingPolygonIndex != null)
            {
                System.out.printf(
                    "[%s.WED] Tiled object %d references %s polygons [%d-%d], " +
                    "which are not part of the wall polygons table\n",
                    source.getIdentifier().resref(), tiledObjectIndex, openCloseLabel,
                    firstInfringingPolygonIndex, -1);
            }

            System.out.printf(
                "[%s.WED] Tiled object %d references %s polygons [%d-%d], " +
                "which are not part of the wall polygons table\n",
                source.getIdentifier().resref(), tiledObjectIndex, openCloseLabel,
                firstInfringingPolygonIndex, lastInfringingPolygonIndex);

            if (firstInfringingPolygonIndex >= 0)
            {
                mark();
                position(startWallPolygonsOffset + firstInfringingPolygonIndex * 0x12);
                for (int i = firstInfringingPolygonIndex; i <= lastInfringingPolygonIndex; ++i)
                {
                    final Polygon polygon = readPolygon(secondaryHeaderInfo.verticesOffset());
                    System.out.printf("  [%d, %d, %d, %d]\n",
                        polygon.getBoundingBoxLeft(),
                        polygon.getBoundingBoxRight(),
                        polygon.getBoundingBoxTop(),
                        polygon.getBoundingBoxBottom());
                }
                reset();
            }
        }
    }

    private record WallGroupsInfo(short maxReferencedPolygonIndex) {}
    private record WallGroupDimensions(short widthInTiles, short heightInTiles) {}

    /////////////////////
    // END Loading WED //
    /////////////////////

    ///////////////////////
    // START Loading TIS //
    ///////////////////////

    private TIS loadTIS(final TaskTrackerI tracker, final String tisResref) throws Exception
    {
        TIS tis = tisCache.get(tisResref);

        if (tis == null)
        {
            final Game.Resource tisResource = GlobalState.getGame().getResource(new ResourceIdentifier(
                tisResref, KeyFile.NumericResourceType.TIS));

            if (tisResource == null)
            {
                throw new IllegalStateException("Unable to find source for TIS resource \"" + tisResref + "\"");
            }

            tis = new TIS(tisResource.getPrimarySource(), resourceDataCache, pvrzCache);
            tis.load(tracker);

            tisCache.add(tisResref, tis);
        }

        return tis;
    }

    /////////////////////
    // END Loading TIS //
    /////////////////////

    PrintWriter openFileForAppend(String fileName) throws IOException
    {
        new FileWriter(fileName).close();
        return new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)), true);
    }

    public class Graphics
    {
        private final ImageAndGraphics imageAndGraphics;
        private final HashMap<String, TIS.Graphics> tisGraphicsCache = new HashMap<>();

        public Graphics(final ImageAndGraphics imageAndGraphics)
        {
            this.imageAndGraphics = imageAndGraphics;
        }

        public BufferedImage getImage()
        {
            return imageAndGraphics.image();
        }

        public Graphics renderOverlays(final TaskTrackerI tracker, final int... overlayIndexes) throws Exception
        {
            final Game game = GlobalState.getGame();
            final Game.Type engineType = game.getEngineType();

            final boolean eeStencil = engineType == Game.Type.BGEE || engineType == Game.Type.BG2EE
                || engineType == Game.Type.IWDEE || engineType == Game.Type.PSTEE;

            final List<WED.Overlay> overlays = getOverlays();
            final WED.Overlay baseOverlay = overlays.get(0);
            final String baseOverlayTISResref = baseOverlay.getTilesetResref();

            if (baseOverlayTISResref.isEmpty())
            {
                // Weirdly happens on real overlays(?)
                return null;
            }

            final int baseOverlayWidth = baseOverlay.getWidthInTiles();
            final int baseOverlayHeight = baseOverlay.getHeightInTiles();
            final int baseOverlayWidthInPixels = baseOverlayWidth * 64;
            final int baseOverlayHeightInPixels = baseOverlayHeight * 64;

            final ArrayList<WED.TilemapEntry> baseOverlayTilemapEntries = baseOverlay.getTilemapEntries();
            final int nGameTime = 0; // TODO - animate

            final BufferedImage image = imageAndGraphics.image();
            final Graphics2D graphics = imageAndGraphics.graphics();

            for (int overlayIndex = 4; overlayIndex > 0; --overlayIndex)
            {
                if (!renderRequested(overlayIndexes, overlayIndex))
                {
                    continue;
                }

                final WED.Overlay overlay = overlays.get(overlayIndex);
                if (overlay == null || overlay.getWidthInTiles() == 0 || overlay.getHeightInTiles() == 0)
                {
                    continue;
                }

                if ((overlay.getMovementType() & 1) != 0)
                {
                    continue;
                }

                final ArrayList<WED.TilemapEntry> overlayTilemapEntries = baseOverlay.getTilemapEntries();

                if (overlayTilemapEntries.isEmpty())
                {
                    continue;
                }

                final int overlayRenderFlag = 1 << overlayIndex;
                final WED.TilemapEntry overlayTilemapEntry = overlayTilemapEntries.get(0);

                final String overlayTisResref = overlay.getTilesetResref();
                final TIS overlayTIS = loadTIS(tracker, overlayTisResref);
                final TIS.Graphics tisGraphics = tisGraphicsCache.computeIfAbsent(overlayTisResref,
                    (ignored) -> overlayTIS.newGraphics(imageAndGraphics));

                for (int yPos = 0, i = 0; yPos < baseOverlayHeightInPixels; yPos += 64)
                {
                    for (int xPos = 0; xPos < baseOverlayWidthInPixels; xPos += 64, ++i)
                    {
                        final WED.TilemapEntry baseOverlayTilemapEntry = baseOverlayTilemapEntries.get(i);

                        if ((baseOverlayTilemapEntry.getDrawFlags() & overlayRenderFlag) == 0)
                        {
                            continue;
                        }

                        final int overlayTileLookupIndex = ((nGameTime / 2)
                            % overlayTilemapEntry.tisTileIndexArray.length
                        );
                        final int overlayTileIndex = overlayTilemapEntry.tisTileIndexArray[overlayTileLookupIndex];
                        tisGraphics.drawTile(overlayTileIndex, xPos, yPos);
                    }
                }
            }

            if (renderRequested(overlayIndexes, 0))
            {
                final int dwRenderFlagsBase =
                    (
                        (baseOverlay.getMovementType() & 2) != 0
                            || (engineType != Game.Type.BG1 && engineType != Game.Type.BGEE)
                    )
                    ? 0x4000000 : 0;

                final TIS baseOverlayTIS = loadTIS(tracker, baseOverlayTISResref);
                final TIS.Graphics baseOverlayTISGraphics = tisGraphicsCache.computeIfAbsent(baseOverlayTISResref,
                    (ignored) -> baseOverlayTIS.newGraphics(imageAndGraphics));

                for (int yPos = 0, i = 0; yPos < baseOverlayHeightInPixels; yPos += 64)
                {
                    for (int xPos = 0; xPos < baseOverlayWidthInPixels; xPos += 64, ++i)
                    {
                        final WED.TilemapEntry tilemapEntry = baseOverlayTilemapEntries.get(i);

                        if ((tilemapEntry.getDrawFlags() & 1) == 0)
                        {
                            int nTile;

                            if ((tilemapEntry.getExtraFlags() & 2) == 0
                                || tilemapEntry.getSecondaryTisTileIndex() == -1)
                            {
                                // Not using secondary tile
                                final byte nAnimSpeed = (byte)Math.max(1, tilemapEntry.getAnimationSpeed());
                                final int nTileLookupIndex = ((nGameTime / nAnimSpeed)
                                    % tilemapEntry.tisTileIndexArray.length);

                                nTile = tilemapEntry.tisTileIndexArray[nTileLookupIndex];
                            }
                            else
                            {
                                // Using secondary tile
                                nTile = tilemapEntry.getSecondaryTisTileIndex();
                            }

                            // if ((baseOverlay.getMovementType() & 2) != 0)
                            // {
                            //     // dwRenderFlags |= 0x4000000;
                            // }

                            int nStencilTile = -1;
                            int dwRenderFlags = dwRenderFlagsBase;

                            if ((tilemapEntry.getDrawFlags() & 0x1E) != 0)
                            {
                                nStencilTile = tilemapEntry.getSecondaryTisTileIndex();
                                dwRenderFlags |= 0x2;
                            }

                            if (baseOverlayTIS.getType() == TIS.Type.PALETTED)
                            {
                                if (nStencilTile == -1)
                                {
                                    final IntBuffer tileData = baseOverlayTIS.getPreRenderedTileData(nTile);
                                    TileUtil.drawTileData(graphics, 64, tileData, xPos, yPos);
                                }
                                else
                                {
                                    final TIS.PalettedTileData tileData = baseOverlayTIS
                                        .getPalettedTileData(nTile);

                                    final TIS.PalettedTileData stencilTileData = baseOverlayTIS
                                        .getPalettedTileData(nStencilTile);

                                    if (eeStencil)
                                    {
                                        final int dwAlpha = (dwRenderFlags & 0x4000000) != 0
                                            ? TIS.WATER_ALPHA
                                            : 0xFF;

                                        TileUtil.drawStenciledTo(
                                            64, xPos, yPos,
                                            dwAlpha, dwRenderFlags,
                                            tileData.getPaletteData(),
                                            tileData.getPalettedData(),
                                            stencilTileData.getPalettedData(),
                                            graphics);
                                    }
                                    else
                                    {
                                        TileUtil.drawClassicStenciledTo(
                                            64, xPos, yPos,
                                            tileData.getPaletteData(),
                                            tileData.getPalettedData(),
                                            stencilTileData.getPalettedData(),
                                            graphics
                                        );
                                    }
                                }
                            }
                            else if (baseOverlayTIS.getType() == TIS.Type.PVRZ)
                            {
                                int dwAlpha = 0xFF000000;

                                if ((dwRenderFlags & 0x4000000) == 0)
                                {
                                    dwRenderFlags &= ~0x2;
                                }
                                else if ((dwRenderFlags & 2) != 0)
                                {
                                    if (nStencilTile != -1)
                                    {
                                        dwRenderFlags &= ~0x2;
                                    }
                                    else
                                    {
                                        dwAlpha = TIS.WATER_ALPHA << 24;
                                    }
                                }

                                baseOverlayTISGraphics.drawTileWithAlpha(nTile, xPos, yPos, dwAlpha, null);

                                if (nStencilTile != -1)
                                {
                                    baseOverlayTISGraphics.drawTileWithAlpha(nStencilTile, xPos, yPos,
                                        TIS.WATER_ALPHA << 24, null);
                                }
                            }
                        }
                        else
                        {
                            // TODO: All black
                        }
                    }
                }
            }

            return this;
        }

        public Graphics clear()
        {
            final BufferedImage image = imageAndGraphics.image();
            imageAndGraphics.graphics().clearRect(0, 0, image.getWidth(), image.getHeight());
            return this;
        }

//        public Graphics renderPolygons(final float width)
//        {
//            final Graphics2D graphics = imageAndGraphics.graphics();
//            for (final Polygon polygon : polygons)
//            {
//                final ArrayList<Vertex> vertices = polygon.getVertices();
//                final int limit = vertices.size() - 1;
//
//                if (limit <= 1)
//                {
//                    continue;
//                }
//
//                graphics.setStroke(new BasicStroke(width));
//
//                for (int i = 0; i < limit; ++i)
//                {
//                    final Vertex v1 = vertices.get(i);
//                    final Vertex v2 = vertices.get(i + 1);
//                    graphics.drawLine(v1.x(), v1.y(), v2.x(), v2.y());
//                }
//
//                final Vertex vFirst = vertices.get(0);
//                final Vertex vLast = vertices.get(limit);
//                graphics.drawLine(vFirst.x(), vFirst.y(), vLast.x(), vLast.y());
//            }
//            return this;
//        }

        private boolean renderRequested(final int[] overlayIndexes, final int overlayIndex)
        {
            for (int requestedOverlayIndex : overlayIndexes)
            {
                if (requestedOverlayIndex == overlayIndex)
                {
                    return true;
                }
            }
            return false;
        }
    }

    private class RenderOverlaysTask extends TrackedTask<BufferedImage>
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final int[] overlayIndexes;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public RenderOverlaysTask(final int... overlayIndexes)
        {
            this.overlayIndexes = overlayIndexes;
        }

        ///////////////////////
        // Protected Methods //
        ///////////////////////

        @Override
        protected BufferedImage doTask() throws Exception
        {
            final Overlay baseOverlay = overlays.get(0);
            final BufferedImage image = new BufferedImage(
                baseOverlay.getWidthInTiles() * 64,
                baseOverlay.getHeightInTiles() * 64,
                BufferedImage.TYPE_INT_ARGB);

            final Graphics wedGraphics = new Graphics(new ImageAndGraphics(image, image.createGraphics()));
            wedGraphics.renderOverlays(getTracker(), overlayIndexes);
            return image;
        }
    }

    private class SaveWEDTask extends TrackedTask<Void>
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final Path path;
        private AppendOnlyOrderedInstanceSet<Polygon> polygonsArray = null;
        private ArrayList<Short>[] wallGroupPolygonIndicesArray = null;
        private ByteBuffer buffer;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public SaveWEDTask(final Path path)
        {
            this.path = path;
        }

        ///////////////////////
        // Protected Methods //
        ///////////////////////

        @Override
        protected Void doTask() throws Exception
        {
            save();
            return null;
        }

        /////////////////////
        // Private Methods //
        /////////////////////

        private void save() throws Exception
        {
            final WEDSectionSizes sectionSizes = calculateSectionSizes();
            buffer = ByteBuffer.allocate(sectionSizes.total());
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            final int headerOffset = 0x0;
            final int overlaysArrayOffset = headerOffset + sectionSizes.header();
            final int secondaryHeaderOffset = overlaysArrayOffset + sectionSizes.overlaysArray();
            final int tiledObjectsArrayOffset = secondaryHeaderOffset + sectionSizes.secondaryHeader();
            final int tilemapArraysOffset = tiledObjectsArrayOffset + sectionSizes.tiledObjectsArray();
            final int tiledObjectTilemapIndicesOffset = tilemapArraysOffset + sectionSizes.tilemapArrays();

            final int tisTileIndicesLookupArrayOffset = tiledObjectTilemapIndicesOffset
                + sectionSizes.tiledObjectTilemapIndices();

            final int wallGroupsArrayOffset = tisTileIndicesLookupArrayOffset
                + sectionSizes.tisTileIndicesLookupTable();

            final int polygonsArrayOffset = wallGroupsArrayOffset + sectionSizes.wallGroupsArray();
            final int polygonsIndicesArrayOffset = polygonsArrayOffset + sectionSizes.polygonsArray();
            final int verticesArrayOffset = polygonsIndicesArrayOffset + sectionSizes.polygonIndicesArray();

            saveHeader(headerOffset, overlaysArrayOffset, secondaryHeaderOffset,
                tiledObjectsArrayOffset, tiledObjectTilemapIndicesOffset);

            saveOverlaysArray(overlaysArrayOffset, tilemapArraysOffset, tisTileIndicesLookupArrayOffset);

            saveSecondaryHeader(secondaryHeaderOffset, polygonsArrayOffset, verticesArrayOffset,
                wallGroupsArrayOffset, polygonsIndicesArrayOffset);

            saveTiledObjectsArray(tiledObjectsArrayOffset, polygonsArrayOffset);
            saveTilemapArrays(tilemapArraysOffset);
            saveTiledObjectTilemapIndices(tiledObjectTilemapIndicesOffset);
            saveTisTileIndicesLookupTable(tisTileIndicesLookupArrayOffset);
            saveWallGroupsArray(wallGroupsArrayOffset);
            savePolygonsArray(polygonsArrayOffset);
            savePolygonIndicesArray(polygonsIndicesArrayOffset);
            saveVerticesArray(verticesArrayOffset);

            Files.write(path, buffer.array());
        }

        private void saveHeader(
            final int offset,
            final int overlaysArrayOffset,
            final int secondaryHeaderOffset,
            final int tiledObjectsArrayOffset,
            final int tiledObjectTilemapIndicesOffset)
        {
            // 0x0000  4 (char array)  Signature ('WED ')
            // 0x0004  4 (char array)  Version ('V1.3')
            // 0x0008  4 (dword)       Number of overlays
            // 0x000C  4 (dword)       Number of tiled objects
            // 0x0010  4 (dword)       Offset to overlays array
            // 0x0014  4 (dword)       Offset to secondary header
            // 0x0018  4 (dword)       Offset to tiled objects array
            // 0x001C  4 (dword)       Offset to tiled object tilemap indices

            ////////////////////////
            // Write Header Array //
            ////////////////////////

            position(offset);
            buffer.put("WED ".getBytes(StandardCharsets.UTF_8));
            buffer.put("V1.3".getBytes(StandardCharsets.UTF_8));
            buffer.putInt(overlays.size());
            buffer.putInt(tiledObjects.size());
            buffer.putInt(overlaysArrayOffset);
            buffer.putInt(secondaryHeaderOffset);
            buffer.putInt(tiledObjectsArrayOffset);
            buffer.putInt(tiledObjectTilemapIndicesOffset);
        }

        private void saveOverlaysArray(
            final int offset, final int tilemapArraysOffset, final int tisTileIndicesLookupArraysOffset)
        {
            // 0x0000  2 (word)    Width (in tiles)
            // 0x0002  2 (word)    Height (in tiles)
            // 0x0004  8 (resref)  TIS Resref
            // 0x000C  2 (word)    Unique tile count
            // 0x000E  2 (word)    Movement type
            // 0x0010  4 (dword)   Offset to tilemap array
            // 0x0014  4 (dword)   Offset to TIS tile indices lookup array

            position(offset);
            int curTilemapArrayOffset = tilemapArraysOffset;
            int curTisTileIndicesLookupArrayOffset = tisTileIndicesLookupArraysOffset;

            //////////////////////////
            // Write Overlays Array //
            //////////////////////////

            for (final Overlay overlay : overlays)
            {
                ///////////////////
                // Write Overlay //
                ///////////////////

                buffer.putShort(overlay.getWidthInTiles());
                buffer.putShort(overlay.getHeightInTiles());
                BufferUtil.writeLUTF8(buffer, 8, overlay.getTilesetResref());
                buffer.putShort(overlay.getUniqueTileCount());
                buffer.putShort(overlay.getMovementType());
                buffer.putInt(curTilemapArrayOffset);
                buffer.putInt(curTisTileIndicesLookupArrayOffset);

                /////////////////////
                // Advance Offsets //
                /////////////////////

                curTilemapArrayOffset += overlay.getTilemapEntries().size() * 0xA;

                for (final TilemapEntry tilemapEntry : overlay.getTilemapEntries())
                {
                    curTisTileIndicesLookupArrayOffset += tilemapEntry.getTisTileIndexArray().length * 0x2;
                }
            }
        }

        private void saveSecondaryHeader(
            final int offset,
            final int polygonsArrayOffset,
            final int verticesArrayOffset,
            final int wallGroupsArrayOffset,
            final int wallGroupPolygonIndicesLookupArrayOffset
        )
        {
            // 0x0000  4 (dword)  Number of polygons (unused and inaccurate)
            // 0x0004  4 (dword)  Offset to polygons array
            // 0x0008  4 (dword)  Offset to vertices array
            // 0x000C  4 (dword)  Offset to wall groups array
            // 0x0010  4 (dword)  Offset to wall group polygon indices lookup table

            position(offset);
            buffer.putInt(polygonsArray.size());
            buffer.putInt(polygonsArrayOffset);
            buffer.putInt(verticesArrayOffset);
            buffer.putInt(wallGroupsArrayOffset);
            buffer.putInt(wallGroupPolygonIndicesLookupArrayOffset);
        }

        private void saveTiledObjectsArray(final int offset, final int polygonsArrayOffset)
        {
            // 0x0000  8 (char array)  Name of tiled object in ARE (unused)
            // 0x0008  2 (word)        Open (0) / Closed (1)
            // 0x000A  2 (word)        First tilemap lookup array index
            // 0x000C  2 (word)        Number of tilemap lookup array indices
            // 0x000E  2 (word)        Number of primary polygons
            // 0x0010  2 (word)        Number of secondary polygons
            // 0x0012  4 (dword)       Offset to primary polygons
            // 0x0016  4 (dword)       Offset to secondary polygons

            position(offset);
            short curFirstTilemapLookupArrayIndex = 0;

            ///////////////////////////////
            // Write Tiled Objects Array //
            ///////////////////////////////

            for (final TiledObject tiledObject : tiledObjects)
            {
                final short numTilemapIndices = (short)tiledObject.getTilemapIndices().size();
                final TrackingOrderedInstanceSet<Polygon> primaryPolygons = tiledObject.getOpenPolygons();
                final TrackingOrderedInstanceSet<Polygon> secondaryPolygons = tiledObject.getClosedPolygons();
                final short numPrimaryPolygons = (short)primaryPolygons.size();
                final short numSecondaryPolygons = (short)secondaryPolygons.size();

                ////////////////////////
                // Write Tiled Object //
                ////////////////////////

                BufferUtil.writeLUTF8(buffer, 8, tiledObject.getResref());
                buffer.putShort(tiledObject.getOpenOrClosed());
                buffer.putShort(curFirstTilemapLookupArrayIndex);
                buffer.putShort(numTilemapIndices);
                buffer.putShort(numPrimaryPolygons);
                buffer.putShort(numSecondaryPolygons);
                buffer.putInt(polygonsArrayOffset
                    + (numPrimaryPolygons > 0 ? polygonsArray.indexOf(primaryPolygons.get(0)) : 0) * 0x12);
                buffer.putInt(polygonsArrayOffset
                    + (numSecondaryPolygons > 0 ? polygonsArray.indexOf(secondaryPolygons.get(0)) : 0) * 0x12);

                /////////////////////
                // Advance Indices //
                /////////////////////

                curFirstTilemapLookupArrayIndex += numTilemapIndices;
            }
        }

        private void saveTilemapArrays(final int offset)
        {
            // 0x0000  2 (word)  First TIS tile lookup array index
            // 0x0002  2 (word)  Number of TIS tile lookup array indices
            // 0x0004  2 (word)  Secondary TIS tile index
            // 0x0006  1 (byte)  Draw flags
            // 0x0007  1 (byte)  Animation speed
            // 0x0008  2 (word)  Extra flags

            position(offset);

            //////////////////////////
            // Write Tilemap Arrays //
            //////////////////////////

            for (final Overlay overlay : overlays)
            {
                short curFirstTisTileLookupArrayIndex = 0;

                /////////////////////////
                // Write Tilemap Array //
                /////////////////////////

                for (final TilemapEntry tilemapEntry : overlay.getTilemapEntries())
                {
                    final short numTisTileIndices = (short)tilemapEntry.getTisTileIndexArray().length;

                    /////////////////////////
                    // Write Tilemap Entry //
                    /////////////////////////

                    buffer.putShort(curFirstTisTileLookupArrayIndex);
                    buffer.putShort(numTisTileIndices);
                    buffer.putShort(tilemapEntry.getSecondaryTisTileIndex());
                    buffer.put(tilemapEntry.getDrawFlags());
                    buffer.put(tilemapEntry.getAnimationSpeed());
                    buffer.putShort(tilemapEntry.getExtraFlags());

                    /////////////////////
                    // Advance Indices //
                    /////////////////////

                    curFirstTisTileLookupArrayIndex += numTisTileIndices;
                }
            }
        }

        private void saveTiledObjectTilemapIndices(final int offset)
        {
            position(offset);

            /////////////////////////////////////////////////////
            // Write Tiled Object Tilemap Indices Lookup Array //
            /////////////////////////////////////////////////////

            for (final TiledObject tiledObject : tiledObjects)
            {
                for (final short tilemapIndex : tiledObject.getTilemapIndices())
                {
                    buffer.putShort(tilemapIndex);
                }
            }
        }

        private void saveTisTileIndicesLookupTable(final int offset)
        {
            position(offset);

            /////////////////////////////////////////////////
            // Write Tilemap TIS Tile Indices Lookup Array //
            /////////////////////////////////////////////////

            for (final Overlay overlay : overlays)
            {
                for (final TilemapEntry tilemapEntry : overlay.getTilemapEntries())
                {
                    for (final short tisTileIndex : tilemapEntry.getTisTileIndexArray())
                    {
                        buffer.putShort(tisTileIndex);
                    }
                }
            }
        }

        private void saveWallGroupsArray(final int offset)
        {
            // 0x0000  2 (word)  First wall group polygon lookup array index
            // 0x0002  2 (word)  Number of wall group polygon lookup array indices

            position(offset);
            short curFirstWallgroupPolygonLookupArrayIndex = 0;

            /////////////////////////////
            // Write Wall Groups Array //
            /////////////////////////////

            for (final ArrayList<Short> wallGroupPolygonIndices : wallGroupPolygonIndicesArray)
            {
                final short numWallGroupPolygonIndices = (short)(wallGroupPolygonIndices != null
                    ? wallGroupPolygonIndices.size()
                    : 0);

                //////////////////////
                // Write Wall Group //
                //////////////////////

                buffer.putShort(curFirstWallgroupPolygonLookupArrayIndex);
                buffer.putShort(numWallGroupPolygonIndices);

                /////////////////////
                // Advance Indices //
                /////////////////////

                curFirstWallgroupPolygonLookupArrayIndex += numWallGroupPolygonIndices;
            }
        }

        private void savePolygonsArray(final int offset)
        {
            // 0x0000  4 (dword)          First vertex index
            // 0x0004  4 (dword)          Number of vertex indices
            // 0x0008  1 (unsigned byte)  Flags
            // 0x0009  1 (byte)           Height
            // 0x000A  2 (word)           Minimum X coordinate of bounding box
            // 0x000C  2 (word)           Maximum X coordinate of bounding box
            // 0x000E  2 (word)           Minimum Y coordinate of bounding box
            // 0x0010  2 (word)           Maximum Y coordinate of bounding box

            position(offset);
            int curFirstVertexIndex = 0;

            //////////////////////////
            // Write Polygons Array //
            //////////////////////////

            for (final Polygon polygon : polygonsArray)
            {
                final int numVertices = polygon.getVertices().size();

                ///////////////////
                // Write Polygon //
                ///////////////////

                buffer.putInt(curFirstVertexIndex);
                buffer.putInt(numVertices);
                buffer.put(polygon.getFlags());
                buffer.put(polygon.getHeight());
                buffer.putShort(polygon.getBoundingBoxLeft());
                buffer.putShort(polygon.getBoundingBoxRight());
                buffer.putShort(polygon.getBoundingBoxTop());
                buffer.putShort(polygon.getBoundingBoxBottom());

                /////////////////////
                // Advance Indices //
                /////////////////////

                curFirstVertexIndex += numVertices;
            }
        }

        private void savePolygonIndicesArray(final int offset)
        {
            position(offset);

            ///////////////////////////////////////////////////
            // Write Wall Group Polygon Indices Lookup Array //
            ///////////////////////////////////////////////////

            for (final ArrayList<Short> wallGroupPolygonIndices : wallGroupPolygonIndicesArray)
            {
                if (wallGroupPolygonIndices == null)
                {
                    continue;
                }

                for (short polygonIndex : wallGroupPolygonIndices)
                {
                    buffer.putShort(polygonIndex);
                }
            }
        }

        private void saveVerticesArray(final int offset)
        {
            // 0x0000  2 (word)  X coordinate
            // 0x0002  2 (word)  Y coordinate

            position(offset);

            //////////////////////////
            // Write Vertices Array //
            //////////////////////////

            for (final Polygon polygon : polygonsArray)
            {
                for (final Vertex vertex : polygon.getVertices())
                {
                    buffer.putShort(vertex.x());
                    buffer.putShort(vertex.y());
                }
            }
        }

        private WEDSectionSizes calculateSectionSizes()
        {
            final int headerSize = calculateHeaderSize();
            final int overlaysArraySize = calculateOverlaysArraySize();
            final int secondaryHeaderSize = calculateSecondaryHeaderSize();
            final int tiledObjectsArraySize = calculateTiledObjectsArraySize();
            final int tilemapArraysSize = calculateTilemapArraysSize();
            final int tiledObjectTilemapIndicesSize = calculateTiledObjectTilemapIndicesSize();
            final int tisTileIndicesLookupTableSize = calculateTisTileIndicesLookupTableSize();
            final int wallGroupsArraySize = calculateWallGroupsArraySize();
            final int polygonsArraySize = calculatePolygonsArraySize();
            final int polygonIndicesArraySize = calculatePolygonIndicesArraySize();
            final int verticesArraySize = calculateVerticesArraySize();
            final int total = headerSize + overlaysArraySize + secondaryHeaderSize + tiledObjectsArraySize
                + tilemapArraysSize + tiledObjectTilemapIndicesSize + tisTileIndicesLookupTableSize + wallGroupsArraySize
                + polygonsArraySize + polygonIndicesArraySize + verticesArraySize;

            return new WEDSectionSizes(
                headerSize,
                overlaysArraySize,
                secondaryHeaderSize,
                tiledObjectsArraySize,
                tilemapArraysSize,
                tiledObjectTilemapIndicesSize,
                tisTileIndicesLookupTableSize,
                wallGroupsArraySize,
                polygonsArraySize,
                polygonIndicesArraySize,
                verticesArraySize,
                total
            );
        }

        /**
         * Main header.
         */
        private int calculateHeaderSize()
        {
            return 0x20;
        }

        /**
         * Table that stores Overlay data (there are normally / maximally 5 overlays, indices [0-4]).
         */
        private int calculateOverlaysArraySize()
        {
            return overlays.size() * 0x18;
        }

        /**
         * Per-overlay table that stores metadata about TIS tiles to be rendered at derived tile coordinates
         * (tile coordinates being derived based on the metadata's position in this table).
         */
        private int calculateTilemapArraysSize()
        {
            int total = 0;
            for (final Overlay overlay : overlays)
            {
                total += overlay.getTilemapEntries().size();
            }
            return total * 0xA;
        }

        /**
         * TilemapEntry structures use this per-overlay indirect table to index into TIS tiles
         */
        private int calculateTisTileIndicesLookupTableSize()
        {
            // TODO: Compress using something like LZW
            int total = 0;
            for (final Overlay overlay : overlays)
            {
                for (final TilemapEntry tilemapEntry : overlay.getTilemapEntries())
                {
                    total += tilemapEntry.getTisTileIndexArray().length;
                }
            }
            return total * 0x2;
        }

        /**
         * Table that stores TiledObject data (doors, anything that should switch between two states, altering
         * whether primary / secondary tiles are displayed, and whether primary / secondary polygons are used).
         */
        private int calculateTiledObjectsArraySize()
        {
            return tiledObjects.size() * 0x1A;
        }

        /**
         * TiledObject entries use this indirect table to index into Overlay 0's TilemapEntry structures
          */
        private int calculateTiledObjectTilemapIndicesSize()
        {
            int total = 0;
            for (final TiledObject tiledObject : tiledObjects)
            {
                total += tiledObject.getTilemapIndices().size();
            }
            return total * 0x2;
        }

        /**
         * Header that stores offsets related to wallgroups / wallgroup polygons.
         */
        private int calculateSecondaryHeaderSize()
        {
            return 0x14;
        }

        /**
         * WallGroups point to which polygons are present in their area, (each WallGroup is 10 tiles wide and 7.5
         * tiles high). Like Tilemap entries, the position of a WallGroup in this table determines which part of
         * the area it is associated with.
         */
        private int calculateWallGroupsArraySize()
        {
            return 0x4 * calculateNumberOfWallGroups();
        }

        private int calculatePolygonsArraySize()
        {
            polygonsArray = new AppendOnlyOrderedInstanceSet<>(polygons);
            return polygonsArray.size() * 0x12;
        }

        // WallGroup instances use this indirect table into wall polygons
        private int calculatePolygonIndicesArraySize()
        {
            final WallGroupDimensions wallGroupDimensions = calculateWallGroupDimensions();
            final short wallGroupsWidth = wallGroupDimensions.widthInTiles();
            final short wallGroupsHeight = wallGroupDimensions.heightInTiles();

            //noinspection unchecked
            wallGroupPolygonIndicesArray = new ArrayList[wallGroupsWidth * wallGroupsHeight];
            int totalPolygonIndices = 0;

            for (final Polygon polygon : polygons)
            {
                final int boundsX = polygon.getBoundingBoxLeft();
                final int boundsWidth = polygon.getBoundingBoxRight() - boundsX;
                if (boundsWidth <= 0) continue;

                final int boundsY = polygon.getBoundingBoxTop();
                final int boundsHeight = polygon.getBoundingBoxBottom() - boundsY;
                if (boundsHeight <= 0) continue;

                final int startWallGroupX = boundsX / 640;
                final int startWallGroupY = boundsY / 480;

                int numWallGroupsX = MiscUtil.divideRoundUp(boundsWidth, 640);
                if (boundsWidth < 640 && boundsX % 640 + boundsWidth >= 640) ++numWallGroupsX;

                int numWallGroupsY = MiscUtil.divideRoundUp(boundsWidth, 480);
                if (boundsHeight < 480 && boundsY % 480 + boundsHeight >= 480) ++numWallGroupsY;

                final Short polygonIndex = (short)(int)polygonsArray.indexOf(polygon);
                final int pitch = wallGroupsWidth - numWallGroupsX;

                for (int wallGroupIndex = startWallGroupY * wallGroupsWidth + startWallGroupX,
                     indexLimitY = wallGroupIndex + numWallGroupsY * wallGroupsWidth;
                     wallGroupIndex < indexLimitY; wallGroupIndex += pitch)
                {
                    for (int indexLimitX = wallGroupIndex + numWallGroupsX;
                         wallGroupIndex < indexLimitX; ++wallGroupIndex)
                    {
                        ArrayList<Short> polygonIndices = wallGroupPolygonIndicesArray[wallGroupIndex];
                        if (polygonIndices == null)
                        {
                            polygonIndices = new ArrayList<>();
                            wallGroupPolygonIndicesArray[wallGroupIndex] = polygonIndices;
                        }
                        polygonIndices.add(polygonIndex);
                        ++totalPolygonIndices;
                    }
                }
            }

            return totalPolygonIndices * 0x2;
        }

        // Referenced by WallGroup Polygons and Door open/close Polygons
        private int calculateVerticesArraySize()
        {
            int total = 0;
            for (final Polygon polygon : polygonsArray)
            {
                total += polygon.getVertices().size();
            }

            return total * 0x4;
        }

        /////////////////////
        // Private Classes //
        /////////////////////

        // Vanilla WED files stored in the following order:
        //   Header
        //   Overlays Array
        //   Secondary Header
        //   Tiled Objects Array
        //   Tilemap Array (Overlay 0)
        //   Tilemap Array (Overlay 1)
        //   ...
        //   Tiled Object Tilemap Indices Lookup Array
        //   Tilemap TIS Tile Indices Lookup Array (Overlay 0)
        //   Tilemap TIS Tile Indices Lookup Array (Overlay 1)
        //   ...
        //   Wall Groups Array
        //   Polygons Array
        //   Wall Group Polygon Indices Lookup Array
        //   Vertices Array
        private record WEDSectionSizes(
            int header,
            int overlaysArray,
            int secondaryHeader,
            int tiledObjectsArray,
            int tilemapArrays,
            int tiledObjectTilemapIndices,
            int tisTileIndicesLookupTable,
            int wallGroupsArray,
            int polygonsArray,
            int polygonIndicesArray,
            int verticesArray,
            int total
        ) {}
    }
}
