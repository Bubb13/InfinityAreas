
package com.github.bubb13.infinityareas;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

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
    private ByteBuffer buffer;
    private ArrayList<Overlay> overlays = new ArrayList<>();

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

    public JavaFXUtil.TaskManager.ManagedTask<Void> loadWEDTask()
    {
        return new LoadWEDTask();
    }

    public List<Overlay> getOverlays()
    {
        return this.overlays;
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void position(final int pos)
    {
        buffer.position(pos);
    }

    ////////////////////
    // Public Classes //
    ////////////////////

    public static class Overlay
    {
        private final short widthInTiles;
        private final short heightInTiles;
        private final String tilesetResref;
        private final short uniqueTileCount;
        private final short movementType;
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

        public short getHeightInTiles()
        {
            return heightInTiles;
        }

        public String getTilesetResref()
        {
            return tilesetResref;
        }

        public short getUniqueTileCount()
        {
            return uniqueTileCount;
        }

        public short getMovementType()
        {
            return movementType;
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

    public static class TilemapEntry
    {
        private final short tisIndexOfAlternateTile;
        private final short drawFlags;
        final short[] tileIndexLookupArray;

        public TilemapEntry(
            final short tisIndexOfAlternateTile, final short drawFlags, final short[] tileIndexLookupArray)
        {
            this.tisIndexOfAlternateTile = tisIndexOfAlternateTile;
            this.drawFlags = drawFlags;
            this.tileIndexLookupArray = tileIndexLookupArray;
        }

        public short getTisIndexOfAlternateTile()
        {
            return tisIndexOfAlternateTile;
        }

        public short getDrawFlags()
        {
            return drawFlags;
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
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            parse();

//            System.out.println("Dumping");
//            for (int i = 0; i < overlays.size(); ++i)
//            {
//                Overlay overlay = overlays.get(i);
//                System.out.println("Overlay " + i);
//                overlay.dump();
//            }

            return null;
        }

        /////////////////////
        // Private Methods //
        /////////////////////

        private void parse() throws Exception
        {
            updateProgress(0, 100);
            updateMessage("Processing WED ...");

            position(0x0); final String signature = BufferUtil.readUTF8(buffer, 4);
            if (!signature.equals("WED "))
            {
                throw new IllegalStateException("Invalid WED signature: \"" + signature + "\"");
            }

            position(0x4); final String version = BufferUtil.readUTF8(buffer, 4);
            if (!version.equals("V1.3"))
            {
                throw new IllegalStateException("Invalid WED version: \"" + version + "\"");
            }

            position(0x8); final int numOverlays = buffer.getInt();
            position(0x10); final int overlaysOffset = buffer.getInt();

            parseOverlays(overlaysOffset, numOverlays);
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
                position(curBase + 0x6); final short drawFlags = buffer.get();

                final short[] tileIndexLookupArray = new short[numTileIndexLookupTableEntries];

                // TODO: Suboptimal
                int curTileIndexLookupTableOffset = tileIndexLookupTableOffset + 2 * tileIndexLookupTableBaseIndex;
                for (int j = 0; j < numTileIndexLookupTableEntries; ++j)
                {
                    position(curTileIndexLookupTableOffset); tileIndexLookupArray[j] = buffer.getShort();
                    curTileIndexLookupTableOffset += 2;
                }

                tilemapEntries.add(new TilemapEntry(tisIndexOfAlternateTile, drawFlags, tileIndexLookupArray));

                curBase += WED_TILEMAP_ENTRY_SIZE;
            }

            return tilemapEntries;
        }
    }
}
