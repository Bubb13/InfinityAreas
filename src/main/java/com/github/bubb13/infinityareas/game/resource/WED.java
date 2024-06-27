
package com.github.bubb13.infinityareas.game.resource;

import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.misc.SimpleCache;
import com.github.bubb13.infinityareas.util.BufferUtil;
import com.github.bubb13.infinityareas.util.JavaFXUtil;
import com.github.bubb13.infinityareas.util.TileUtil;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
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

    private final ResourceDataCache resourceDataCache = new ResourceDataCache();
    private final SimpleCache<String, PVRZ> pvrzCache = new SimpleCache<>();
    private final SimpleCache<String, TIS> tisCache = new SimpleCache<>();

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

    public JavaFXUtil.TaskManager.ManagedTask<BufferedImage> renderOverlaysNewTask(final int... overlayIndexes)
    {
        return new RenderOverlaysNewTask(overlayIndexes);
    }

    public List<Overlay> getOverlays()
    {
        return overlays;
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
        private final byte animationSpeed;
        private final short extraFlags;
        final short[] tileIndexLookupArray;

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

        public short getDrawFlags()
        {
            return drawFlags;
        }

        public byte getAnimationSpeed()
        {
            return animationSpeed;
        }

        public short getExtraFlags()
        {
            return extraFlags;
        }

        public short[] getTileIndexLookupArray()
        {
            return tileIndexLookupArray;
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
    }

    private class LoadTISTask extends JavaFXUtil.TaskManager.ManagedTask<TIS>
    {
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
            final int numPixels = baseOverlayWidthInPixels * baseOverlayHeightInPixels;
            final int[] result = new int[numPixels];

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

                TileUtil.iterateOverlayTileOffsets(64, baseOverlayWidth, baseOverlayHeight,
                    (final int tileSideLength, final int dstPitch, final int dstOffset, final int i) ->
                    {
                        final WED.TilemapEntry baseOverlayTilemapEntry = baseOverlayTilemapEntries.get(i);

                        if ((baseOverlayTilemapEntry.getDrawFlags() & overlayRenderFlag) == 0)
                        {
                            return;
                        }

                        final int overlayTileLookupIndex = ((nGameTime / 2)
                            % overlayTilemapEntry.tileIndexLookupArray.length
                        );
                        final int overlayTileIndex = overlayTilemapEntry.tileIndexLookupArray[overlayTileLookupIndex];
                        final IntBuffer tileData = overlayTIS.getPreRenderedTileData(overlayTileIndex);

                        TileUtil.copyTo(tileSideLength, dstPitch, dstOffset, tileData, result);
                    });
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

                TileUtil.iterateOverlayTileOffsets(64, baseOverlayWidth, baseOverlayHeight,
                    (final int tileSideLength, final int dstPitch, final int dstOffset, final int i) ->
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

                            if (nStencilTile != -1 && baseOverlayTIS.getType() == TIS.Type.PALETTED)
                            {
                                final TIS.PalettedTileData tileData = baseOverlayTIS
                                    .getPalettedTileData(nTile);

                                final TIS.PalettedTileData stencilTileData = baseOverlayTIS
                                    .getPalettedTileData(nStencilTile);

//                            final BufferedImage image1 = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
//                            image1.getRaster().setDataElements(0, 0, 64, 64, tileData.getPreRenderedData().array());
//
//                            final BufferedImage image2 = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
//                            image2.getRaster().setDataElements(0, 0, 64, 64, stencilTileData.getPreRenderedData().array());
//
//                            try
//                            {
//                                ImageIO.write(image1, "png", game.getRoot().resolve("InfinityAreasTemp")
//                                    .resolve(String.format("TILE_%d.PNG", nTile)).toFile());
//
//                                ImageIO.write(image2, "png", game.getRoot().resolve("InfinityAreasTemp")
//                                    .resolve(String.format("TILE_%d_STENCIL_%d.PNG", nTile, nStencilTile)).toFile());
//                            }
//                            catch (IOException e)
//                            {
//                                throw new RuntimeException(e);
//                            }

                                if (eeStencil)
                                {
                                    final int dwAlpha = (dwRenderFlags & 0x4000000) != 0
                                        ? TIS.WATER_ALPHA
                                        : 0xFF;

                                    TileUtil.copyStenciledTo(
                                        tileSideLength, dstPitch, dstOffset,
                                        dwAlpha, dwRenderFlags,
                                        tileData.getPaletteData(),
                                        tileData.getPalettedData(),
                                        stencilTileData.getPalettedData(),
                                        result
                                    );
                                }
                                else
                                {
                                    TileUtil.classicCopyStenciledTo(
                                        tileSideLength, dstPitch, dstOffset,
                                        tileData.getPaletteData(),
                                        tileData.getPalettedData(),
                                        stencilTileData.getPalettedData(),
                                        result
                                    );
                                }
                            }
                            else
                            {
                                final IntBuffer tileData = baseOverlayTIS.getPreRenderedTileData(nTile);
                                TileUtil.copyTo(tileSideLength, dstPitch, dstOffset, tileData, result);
                            }
                        }
                        else
                        {
                            // TODO: All black
                        }
                    });
            }

            final BufferedImage image = new BufferedImage(
                baseOverlayWidthInPixels, baseOverlayHeightInPixels, BufferedImage.TYPE_INT_ARGB
            );
            image.getRaster().setDataElements(0, 0, baseOverlayWidthInPixels, baseOverlayHeightInPixels, result);

            return image;
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

    private class RenderOverlaysNewTask extends JavaFXUtil.TaskManager.ManagedTask<BufferedImage>
    {
        private final int[] overlayIndexes;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public RenderOverlaysNewTask(final int... overlayIndexes)
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
