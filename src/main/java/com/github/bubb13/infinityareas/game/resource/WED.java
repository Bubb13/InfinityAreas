
package com.github.bubb13.infinityareas.game.resource;

import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.misc.SimpleCache;
import com.github.bubb13.infinityareas.util.BufferUtil;
import com.github.bubb13.infinityareas.util.JavaFXUtil;
import com.github.bubb13.infinityareas.util.MiscUtil;
import com.github.bubb13.infinityareas.util.TileUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
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

    private ArrayList<Overlay> overlays = new ArrayList<>();
    private ArrayList<Polygon> polygons = new ArrayList<>();
    private ArrayList<WallGroup> wallGroups = new ArrayList<>();
    private ArrayList<Door> doors = new ArrayList<>();

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

    public JavaFXUtil.TaskManager.ManagedTask<Void> loadWEDTask()
    {
        return new LoadWEDTask();
    }

    public JavaFXUtil.TaskManager.ManagedTask<BufferedImage> renderOverlaysTask(final int... overlayIndexes)
    {
        return new RenderOverlaysTask(overlayIndexes);
    }

    public List<Overlay> getOverlays()
    {
        return overlays;
    }

    public boolean checkAndClearChanged()
    {
        final boolean temp = changed;
        changed = false;
        return temp;
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

    ////////////////////
    // Public Classes //
    ////////////////////

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
                System.out.println("      Alternate Tile TIS Index: " + tilemapEntry.tisIndexOfAlternateTile);
                System.out.println("      Draw Flags: " + tilemapEntry.drawFlags);

                System.out.println("      Tile Index Lookup Table:");

                final short[] tileIndexLookupArray = tilemapEntry.tileIndexLookupArray;
                for (final short value : tileIndexLookupArray)
                {
                    System.out.println("        " + value);
                }
            }
        }
    }

    public class TilemapEntry
    {
        private short tisIndexOfAlternateTile;
        private short drawFlags;
        private byte animationSpeed;
        private short extraFlags;
        private short[] tileIndexLookupArray;

        public TilemapEntry(
            final short tisIndexOfAlternateTile, final short drawFlags, final byte animationSpeed,
            final short extraFlags, final short[] tileIndexLookupArray)
        {
            this.tisIndexOfAlternateTile = tisIndexOfAlternateTile;
            this.drawFlags = drawFlags;
            this.animationSpeed = animationSpeed;
            this.extraFlags = extraFlags;
            this.tileIndexLookupArray = tileIndexLookupArray;
        }

        public short getTisIndexOfAlternateTile()
        {
            return tisIndexOfAlternateTile;
        }

        public void setTisIndexOfAlternateTile(final short tisIndexOfAlternateTile)
        {
            this.tisIndexOfAlternateTile = tisIndexOfAlternateTile;
            changed();
        }

        public short getDrawFlags()
        {
            return drawFlags;
        }

        public void setDrawFlags(final short drawFlags)
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

        public short[] getTileIndexLookupArray()
        {
            return tileIndexLookupArray;
        }

        public void setTileIndexLookupArray(final short[] tileIndexLookupArray)
        {
            this.tileIndexLookupArray = tileIndexLookupArray;
            changed();
        }
    }

    public record Vertex(short x, short y) {}

    public class Polygon
    {
        // In-file
        private byte flags;
        private byte height;
        private short boundingBoxLeft;
        private short boundingBoxRight;
        private short boundingBoxTop;
        private short boundingBoxBottom;

        // Derived
        private final ArrayList<Vertex> vertices;

        public Polygon(
            final byte flags, final byte height,
            final short boundingBoxLeft, final short boundingBoxRight,
            final short boundingBoxTop, final short boundingBoxBottom,
            final ArrayList<Vertex> vertices)
        {
            this.flags = flags;
            this.height = height;
            this.boundingBoxLeft = boundingBoxLeft;
            this.boundingBoxRight = boundingBoxRight;
            this.boundingBoxTop = boundingBoxTop;
            this.boundingBoxBottom = boundingBoxBottom;
            this.vertices = vertices;
        }

        public byte getFlags()
        {
            return flags;
        }

        public void setFlags(final byte flags)
        {
            this.flags = flags;
            changed();
        }

        public byte getHeight()
        {
            return height;
        }

        public void setHeight(final byte height)
        {
            this.height = height;
            changed();
        }

        public short getBoundingBoxLeft()
        {
            return boundingBoxLeft;
        }

        public void setBoundingBoxLeft(final short boundingBoxLeft)
        {
            this.boundingBoxLeft = boundingBoxLeft;
            changed();
        }

        public short getBoundingBoxRight()
        {
            return boundingBoxRight;
        }

        public void setBoundingBoxRight(final short boundingBoxRight)
        {
            this.boundingBoxRight = boundingBoxRight;
            changed();
        }

        public short getBoundingBoxTop()
        {
            return boundingBoxTop;
        }

        public void setBoundingBoxTop(final short boundingBoxTop)
        {
            this.boundingBoxTop = boundingBoxTop;
            changed();
        }

        public short getBoundingBoxBottom()
        {
            return boundingBoxBottom;
        }

        public void setBoundingBoxBottom(final short boundingBoxBottom)
        {
            this.boundingBoxBottom = boundingBoxBottom;
            changed();
        }

        public ArrayList<Vertex> getVertices()
        {
            return vertices;
        }
    }

    public static class WallGroup
    {
        private final ArrayList<Short> polygonIndices;

        public WallGroup(final ArrayList<Short> polygonIndices)
        {
            this.polygonIndices = polygonIndices;
        }

        public ArrayList<Short> getPolygonIndices()
        {
            return polygonIndices;
        }
    }

    public class Door
    {
        private String doorResref;
        private short openOrClosed; // open = 0, closed = 1
        private final ArrayList<Short> doorTileCellIndices;
        private final ArrayList<Polygon> openPolygons;
        private final ArrayList<Polygon> closedPolygons;

        public Door(
            final String doorResref, final short openOrClosed,
            final ArrayList<Short> doorTileCellIndices,
            final ArrayList<Polygon> openPolygons, final ArrayList<Polygon> closedPolygons)
        {
            this.doorResref = doorResref;
            this.openOrClosed = openOrClosed;
            this.doorTileCellIndices = doorTileCellIndices;
            this.openPolygons = openPolygons;
            this.closedPolygons = closedPolygons;
        }

        public String getDoorResref()
        {
            return doorResref;
        }

        public void setDoorResref(final String doorResref)
        {
            this.doorResref = doorResref;
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

        public ArrayList<Short> getDoorTileCellIndices()
        {
            return doorTileCellIndices;
        }

        public ArrayList<Polygon> getOpenPolygons()
        {
            return openPolygons;
        }

        public ArrayList<Polygon> getClosedPolygons()
        {
            return closedPolygons;
        }
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private class LoadWEDTask extends JavaFXUtil.TaskManager.ManagedTask<Void>
    {
        ///////////////////////
        // Protected Methods //
        ///////////////////////

        @Override
        protected Void call() throws Exception
        {
            buffer = source.demandFileData();
            parse();
            return null;
        }

        /////////////////////
        // Private Methods //
        /////////////////////

        private void parse() throws Exception
        {
            updateProgress(0, 100);
            updateMessage("Processing WED ...");

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
            final int numDoors = buffer.getInt();
            final int overlaysOffset = buffer.getInt();
            final int secondaryHeaderOffset = buffer.getInt();
            final int doorsOffset = buffer.getInt();
            final int doorTileCellIndicesOffset = buffer.getInt();

            parseOverlays(overlaysOffset, numOverlays);
            final int verticesOffset = parseSecondaryHeader(secondaryHeaderOffset);
            parseDoors(doorsOffset, numDoors, doorTileCellIndicesOffset, verticesOffset);
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

        private ArrayList<TilemapEntry> parseTilemapEntries(
            final int offset, final int count, final int tileIndexLookupTableOffset)
        {
            final ArrayList<TilemapEntry> tilemapEntries = new ArrayList<>();

            int curBase = offset;
            for (int i = 0; i < count; ++i)
            {
                position(curBase);       final short tileIndexLookupTableBaseIndex = buffer.getShort();
                position(curBase + 0x2); final short numTileIndexLookupTableEntries = buffer.getShort();
                position(curBase + 0x4); final short tisIndexOfAlternateTile = buffer.getShort();
                position(curBase + 0x6); final byte drawFlags = buffer.get();
                position(curBase + 0x7); final byte animationSpeed = buffer.get();
                position(curBase + 0x8); final short extraFlags = buffer.get();

                final short[] tileIndexLookupArray = new short[numTileIndexLookupTableEntries];

                // TODO: Suboptimal
                int curTileIndexLookupTableOffset = tileIndexLookupTableOffset + 2 * tileIndexLookupTableBaseIndex;
                for (int j = 0; j < numTileIndexLookupTableEntries; ++j)
                {
                    position(curTileIndexLookupTableOffset); tileIndexLookupArray[j] = buffer.getShort();
                    curTileIndexLookupTableOffset += 2;
                }

                tilemapEntries.add(new TilemapEntry(
                    tisIndexOfAlternateTile, drawFlags, animationSpeed, extraFlags, tileIndexLookupArray));

                curBase += WED_TILEMAP_ENTRY_SIZE;
            }

            return tilemapEntries;
        }

        private int parseSecondaryHeader(final int offset)
        {
            position(offset);

            final int numPolygons = buffer.getInt();
            final int polygonsOffset = buffer.getInt();
            final int verticesOffset = buffer.getInt();
            final int wallGroupsOffset = buffer.getInt();
            final int polygonIndicesLookupTableOffset = buffer.getInt();

            parseWallPolygons(polygonsOffset, numPolygons, verticesOffset);
            parseWallGroups(wallGroupsOffset, polygonIndicesLookupTableOffset);
            return verticesOffset;
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

            final ArrayList<Vertex> vertices = new ArrayList<>();

            // Read vertices
            mark();
            position(verticesOffset + vertexStartIndex * 4);
            for (int j = 0; j < numVertices; ++j)
            {
                final short vertexX = buffer.getShort();
                final short vertexY = buffer.getShort();
                vertices.add(new Vertex(vertexX, vertexY));
            }
            reset();

            return new Polygon(
                flags, height,
                boundingBoxLeft, boundingBoxRight,
                boundingBoxTop, boundingBoxBottom,
                vertices
            );
        }

        private void parseWallPolygons(final int offset, final int count, final int verticesOffset)
        {
            position(offset);

            for (int i = 0; i < count; ++i)
            {
                polygons.add(readPolygon(verticesOffset));
            }
        }

        private void parseWallGroups(final int offset, final int polygonIndicesLookupTableOffset)
        {
            final Overlay baseOverlay = overlays.get(0);
            final short baseOverlayWidth = baseOverlay.getWidthInTiles();
            final short baseOverlayHeight = baseOverlay.getHeightInTiles();

            final short wallGroupCountX = (short)MiscUtil.divideRoundUp(baseOverlayWidth, 10);
            final short wallGroupCountY = (short)MiscUtil.multiplyByRatioRoundUp(baseOverlayHeight, 2, 15);
            final int numWallGroups = (int)wallGroupCountX * wallGroupCountY;

            position(offset);

            for (int i = 0; i < numWallGroups; ++i)
            {
                final short startPolygonsIndexIndex = buffer.getShort();
                final short polygonIndexCount = buffer.getShort();

                final ArrayList<Short> polygonIndices = new ArrayList<>();

                // Read polygon indices
                mark();
                position(polygonIndicesLookupTableOffset + startPolygonsIndexIndex * 2);
                for (int j = 0; j < polygonIndexCount; ++j)
                {
                    final short polygonIndex = buffer.getShort();
                    polygonIndices.add(polygonIndex);
                }
                reset();

                wallGroups.add(new WallGroup(polygonIndices));
            }
        }

        private void parseDoors(
            final int offset, final int numDoors, final int doorTileCellIndicesOffset, final int verticesOffset)
        {
            position(offset);

            for (int i = 0; i < numDoors; ++i)
            {
                final String doorResref = BufferUtil.readLUTF8(buffer, 8);
                final short openOrClosed = buffer.getShort();
                final short doorTileCellsStartIndex = buffer.getShort();
                final short numDoorTileCells = buffer.getShort();
                final short numOpenPolygons = buffer.getShort();
                final short numClosedPolygons = buffer.getShort();
                final int openPolygonsOffset = buffer.getInt();
                final int closedPolygonsOffset = buffer.getInt();

                final ArrayList<Short> doorTileCellIndices = new ArrayList<>();
                final ArrayList<Polygon> openPolygons = new ArrayList<>();
                final ArrayList<Polygon> closedPolygons = new ArrayList<>();

                mark();

                // Read doorTileCellIndices
                position(doorTileCellIndicesOffset + doorTileCellsStartIndex * 2);
                for (int j = 0; j < numDoorTileCells; ++j)
                {
                    final short doorTileCellIndex = buffer.getShort();
                    doorTileCellIndices.add(doorTileCellIndex);
                }

                // Read openPolygons
                position(openPolygonsOffset);
                for (int j = 0; j < numOpenPolygons; ++j)
                {
                    openPolygons.add(readPolygon(verticesOffset));
                }

                // Read closedPolygons
                position(closedPolygonsOffset);
                for (int j = 0; j < numClosedPolygons; ++j)
                {
                    closedPolygons.add(readPolygon(verticesOffset));
                }

                doors.add(new Door(doorResref, openOrClosed, doorTileCellIndices, openPolygons, closedPolygons));
                reset();
            }
        }
    }

    private class LoadTISTask extends JavaFXUtil.TaskManager.ManagedTask<TIS>
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        final String tisResref;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public LoadTISTask(final String tisResref)
        {
            this.tisResref = tisResref;
        }

        ///////////////////////
        // Protected Methods //
        ///////////////////////

        @Override
        protected TIS call() throws Exception
        {
            return loadTIS(tisResref);
        }

        /////////////////////
        // Private Methods //
        /////////////////////

        private TIS loadTIS(final String tisResref) throws Exception
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
                subtask(tis.loadTISTask());

                tisCache.add(tisResref, tis);
            }

            return tis;
        }
    }

    private class RenderOverlaysTask extends JavaFXUtil.TaskManager.ManagedTask<BufferedImage>
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
        protected BufferedImage call() throws Exception
        {
            buffer = source.demandFileData();
            return renderOverlays();
        }

        /////////////////////
        // Private Methods //
        /////////////////////

        private BufferedImage renderOverlays() throws Exception
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

            final BufferedImage combined = new BufferedImage(
                baseOverlayWidthInPixels, baseOverlayHeightInPixels, BufferedImage.TYPE_INT_ARGB
            );
            final Graphics2D graphics = (Graphics2D)combined.getGraphics();

            final ArrayList<WED.TilemapEntry> baseOverlayTilemapEntries = baseOverlay.getTilemapEntries();
            final int nGameTime = 0; // TODO - animate

            for (int overlayIndex = 4; overlayIndex > 0; --overlayIndex)
            {
                if (!renderRequested(overlayIndex))
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
                final TIS overlayTIS = subtask(new LoadTISTask(overlay.getTilesetResref()));

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
                            % overlayTilemapEntry.tileIndexLookupArray.length
                        );
                        final int overlayTileIndex = overlayTilemapEntry.tileIndexLookupArray[overlayTileLookupIndex];
                        final IntBuffer tileData = overlayTIS.getPreRenderedTileData(overlayTileIndex);

                        TileUtil.drawTileData(graphics, 64, tileData, xPos, yPos);
                    }
                }
            }

            if (renderRequested(0))
            {
                final int dwRenderFlagsBase =
                    (
                        (baseOverlay.getMovementType() & 2) != 0
                        || (engineType != Game.Type.BG1 && engineType != Game.Type.BGEE)
                    )
                    ? 0x4000000 : 0;

                final TIS baseOverlayTIS = subtask(new LoadTISTask(baseOverlayTISResref));
                assert baseOverlayTIS != null;

                for (int yPos = 0, i = 0; yPos < baseOverlayHeightInPixels; yPos += 64)
                {
                    for (int xPos = 0; xPos < baseOverlayWidthInPixels; xPos += 64, ++i)
                    {
                        final WED.TilemapEntry tilemapEntry = baseOverlayTilemapEntries.get(i);

                        if ((tilemapEntry.getDrawFlags() & 1) == 0)
                        {
                            int nTile;

                            if ((tilemapEntry.getExtraFlags() & 2) == 0 || tilemapEntry.getTisIndexOfAlternateTile() == -1)
                            {
                                // Not using secondary tile
                                final byte nAnimSpeed = (byte)Math.max(1, tilemapEntry.getAnimationSpeed());
                                final int nTileLookupIndex = ((nGameTime / nAnimSpeed)
                                    % tilemapEntry.tileIndexLookupArray.length);

                                nTile = tilemapEntry.tileIndexLookupArray[nTileLookupIndex];
                            }
                            else
                            {
                                // Using secondary tile
                                nTile = tilemapEntry.getTisIndexOfAlternateTile();
                            }

                            // if ((baseOverlay.getMovementType() & 2) != 0)
                            // {
                            //     // dwRenderFlags |= 0x4000000;
                            // }

                            int nStencilTile = -1;
                            int dwRenderFlags = dwRenderFlagsBase;

                            if ((tilemapEntry.getDrawFlags() & 0x1E) != 0)
                            {
                                nStencilTile = tilemapEntry.getTisIndexOfAlternateTile();
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

                                final IntBuffer tileData = baseOverlayTIS.getPreRenderedTileData(nTile);
                                TileUtil.drawAlphaTo(64, xPos, yPos,
                                    dwAlpha, tileData, combined);

                                if (nStencilTile != -1)
                                {
                                    final IntBuffer stencilTileData = baseOverlayTIS
                                        .getPreRenderedTileData(nStencilTile);

                                    TileUtil.drawAlphaTo(64, xPos, yPos,
                                        TIS.WATER_ALPHA << 24, stencilTileData, combined);
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

            return combined;
        }

        private boolean renderRequested(final int overlayIndex)
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
}
