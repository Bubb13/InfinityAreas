
package com.github.bubb13.infinityareas.game.resource;

import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.misc.SimpleCache;
import com.github.bubb13.infinityareas.util.BufferUtil;
import com.github.bubb13.infinityareas.util.JavaFXUtil;
import com.github.bubb13.infinityareas.util.MiscUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

public class TIS
{
    //////////////////////////
    // Public Static Fields //
    //////////////////////////

    public static final int HEADER_SIZE = 0x18;

    ///////////////////////////
    // Private Static Fields //
    ///////////////////////////

    private static final int PVRZ_TILE_DATA_ENTRY_SIZE = 0xC;
    private static final int NUM_PALETTE_COLORS = 256;

    ////////////////////
    // Private Fields //
    ////////////////////

    private final String wedNumeric;
    private final Game.ResourceSource source;
    private final ResourceDataCache resourceDataCache;
    private final SimpleCache<String, PVRZ> pvrzCache;

    private ByteBuffer buffer;
    private int tileSideLength;
    private IntBuffer blackTile;
    private ArrayList<IntBuffer> tiles;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public TIS(
        final String wedNumeric, final Game.ResourceSource source,
        final ResourceDataCache resourceDataCache, final SimpleCache<String, PVRZ> pvrzCache)
    {
        this.wedNumeric = wedNumeric;
        this.source = source;
        this.resourceDataCache = resourceDataCache;
        this.pvrzCache = pvrzCache;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public JavaFXUtil.TaskManager.ManagedTask<Void> loadTISTask()
    {
        return new LoadTISTask();
    }

    public IntBuffer getTileData(final int index)
    {
        if (index < tiles.size())
        {
            return tiles.get(index);
        }
        else
        {
            System.out.println("Unable to fetch tile " + index);

            // TODO: Temp panic code
            final int numPixels = tileSideLength * tileSideLength;
            final IntBuffer magenta = IntBuffer.allocate(numPixels);
            for (int i = 0; i < numPixels; ++i)
            {
                magenta.put(0xFFFF00FF);
            }
            magenta.flip();
            return magenta;
        }
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void position(final int pos)
    {
        buffer.position(pos);
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private class LoadTISTask extends JavaFXUtil.TaskManager.ManagedTask<Void>
    {
        ///////////////////////
        // Protected Methods //
        ///////////////////////

        @Override
        protected Void call() throws Exception
        {
            buffer = resourceDataCache.demand(source);
            parse();
            return null;
        }

        /////////////////////
        // Private Methods //
        /////////////////////

        private void parse() throws Exception
        {
            updateProgress(0, 100);
            updateMessage("Processing TIS ...");

            position(0x0); final String signature = BufferUtil.readUTF8(buffer, 4);
            if (!signature.equals("TIS "))
            {
                throw new IllegalStateException("Invalid TIS signature: \"" + signature + "\"");
            }

            position(0x4); final String version = BufferUtil.readUTF8(buffer, 4);
            if (!version.equals("V1  "))
            {
                throw new IllegalStateException("Invalid TIS version: \"" + version + "\"");
            }

            position(0x8); final int numTiles = buffer.getInt();
            position(0xC); final int lengthOfTileBlockData = buffer.getInt();
            position(0x10); final int sizeOfHeader = buffer.getInt();
            position(0x14); tileSideLength = buffer.getInt();

            blackTile = allocateSingleColorTile(0xFF000000);

            if (lengthOfTileBlockData == 0xC)
            {
                // PVRZ-based
                tiles = parsePVRZTileData(sizeOfHeader, numTiles);
            }
            else if (lengthOfTileBlockData == 0x1400)
            {
                // Palette-based
                tiles = parsePaletteTileData(sizeOfHeader, numTiles);
            }
            else
            {
                throw new IllegalStateException("Unexpected TIS lengthOfTileBlockData: " + lengthOfTileBlockData);
            }
        }

        private IntBuffer allocateSingleColorTile(final int color)
        {
            final int numPixels = tileSideLength * tileSideLength;
            final IntBuffer tile = IntBuffer.allocate(numPixels);
            for (int i = 0; i < numPixels; ++i)
            {
                tile.put(color);
            }
            tile.flip();
            return tile;
        }

        private ArrayList<IntBuffer> parsePVRZTileData(final int offset, final int count) throws Exception
        {
            final char firstChar = source.getIdentifier().resref().charAt(0);
            final String pvrzPrefix = firstChar + wedNumeric;

            final ArrayList<IntBuffer> tiles = new ArrayList<>();
            int curBase = offset;

            for (int i = 0; i < count; ++i, curBase += PVRZ_TILE_DATA_ENTRY_SIZE)
            {
                position(curBase); final int pvrzPage = buffer.getInt();

                if (pvrzPage == -1)
                {
                    // Special: Completely black
                    tiles.add(blackTile);
                    continue;
                }

                position(curBase + 0x4); final int coordinateX = buffer.getInt();
                position(curBase + 0x8); final int coordinateY = buffer.getInt();

                final String pvrzResref = pvrzPrefix + String.format("%02d", pvrzPage);
                final PVRZ pvrz = loadPVRZ(pvrzResref);

                final IntBuffer tile = pvrz.cutout(coordinateX, coordinateY, tileSideLength, tileSideLength);
                tiles.add(tile);
            }

            return tiles;
        }

        private PVRZ loadPVRZ(final String pvrzResref) throws Exception
        {
            PVRZ pvrz = pvrzCache.get(pvrzResref);

            if (pvrz == null)
            {
                final Game.Resource pvrzResource = GlobalState.getGame().getResource(new ResourceIdentifier(
                    pvrzResref, KeyFile.NumericResourceType.PVRZ));

                if (pvrzResource == null)
                {
                    throw new IllegalStateException("Unable to find source for PVRZ resource \"" + pvrzResref + "\"");
                }

                pvrz = new PVRZ(pvrzResource.getPrimarySource(), resourceDataCache);
                subtask(pvrz.loadPVRZTask());
                pvrzCache.add(pvrzResref, pvrz);
            }

            return pvrz;
        }

        private ArrayList<IntBuffer> parsePaletteTileData(final int offset, final int count)
        {
            position(offset);

            final int numPixelsInTile = tileSideLength * tileSideLength;
            final ArrayList<IntBuffer> tiles = new ArrayList<>();

            for (int tileI = 0; tileI < count; ++tileI)
            {
                // Read palette
                final int[] palette = new int[NUM_PALETTE_COLORS];
                for (int i = 0; i < NUM_PALETTE_COLORS; ++i)
                {
                    final int argbColor = buffer.getInt() | 0xFF000000; // Automatically set alpha to 255
                    palette[i] = argbColor;
                }

                final int[] tileData = new int[numPixelsInTile];

                // Read tile data
                for (int i = 0; i < numPixelsInTile; ++i)
                {
                    final short paletteIndex = MiscUtil.toUnsignedByte(buffer.get());
                    tileData[i] = palette[paletteIndex];
                }

                tiles.add(IntBuffer.wrap(tileData));
            }

            return tiles;
        }

        private void debugWritePaletteTilesetToImage(final int offset, final int count) throws Exception
        {
            position(offset);

            final int numPixelsInTile = tileSideLength * tileSideLength;
            final int numPixelsInTIS = count * numPixelsInTile;

            final int[] result = new int[numPixelsInTIS];
            final int resultLineOffset = count * tileSideLength;

            for (int tileI = 0; tileI < count; ++tileI)
            {
                // Read palette
                final int[] palette = new int[NUM_PALETTE_COLORS];
                for (int i = 0; i < NUM_PALETTE_COLORS; ++i)
                {
                    int argbColor = buffer.getInt();
                    argbColor |= 0xFF000000;
                    palette[i] = argbColor;
                }

                // Read tile data
                for (int y = 0; y < tileSideLength; ++y)
                {
                    for (int x = 0; x < tileSideLength; ++x)
                    {
                        final short paletteIndex = MiscUtil.toUnsignedByte(buffer.get());

                        final int resultXOffset = tileI * tileSideLength + x;
                        final int resultYOffset = y * resultLineOffset;

                        result[resultXOffset + resultYOffset] = palette[paletteIndex];
                    }
                }
            }

            final int finalW = count * tileSideLength;
            final int finalH = tileSideLength;

            final BufferedImage image = new BufferedImage(finalW, finalH, BufferedImage.TYPE_INT_ARGB);
            image.getRaster().setDataElements(0, 0, finalW, finalH, result);

            ImageIO.write(image, "png", GlobalState.getGame()
                .getRoot().resolve("PALETTE_TILE.PNG").toFile());
        }
    }
}
