
package com.github.bubb13.infinityareas.game.resource;

import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.misc.SimpleCache;
import com.github.bubb13.infinityareas.util.BufferUtil;
import com.github.bubb13.infinityareas.util.JavaFXUtil;
import com.github.bubb13.infinityareas.util.TileUtil;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class Area
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final Game.ResourceSource source;
    private final ResourceDataCache resourceDataCache = new ResourceDataCache();
    private final SimpleCache<String, PVRZ> pvrzCache = new SimpleCache<>();
    private final SimpleCache<String, TIS> tisCache = new SimpleCache<>();

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

    public JavaFXUtil.TaskManager.ManagedTask<Void> loadAreaTask()
    {
        return new LoadAreaTask();
    }

    public int getOverlayCount()
    {
        return wed.getOverlays().size();
    }

    public JavaFXUtil.TaskManager.ManagedTask<BufferedImage> renderOverlayTask(final int overlayIndex)
    {
        return new RenderOverlayTask(overlayIndex);
    }

    public JavaFXUtil.TaskManager.ManagedTask<BufferedImage> renderOverlaysTask(final int... overlayIndexes)
    {
        return new RenderOverlaysTask(overlayIndexes);
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

    private class LoadAreaTask extends JavaFXUtil.TaskManager.ManagedTask<Void>
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
            updateMessage("Processing area ...");

            position(8); final String wedResref = BufferUtil.readLUTF8(buffer, 8);

            final Game game = GlobalState.getGame();

            final ResourceIdentifier wedIdentifier = new ResourceIdentifier(wedResref, KeyFile.NumericResourceType.WED);
            final Game.Resource wedResource = game.getResource(wedIdentifier);

            if (wedResource == null)
            {
                throw new IllegalStateException("Unable to find source for WED resource \"" + wedResref + "\"");
            }

            final WED tempWed = new WED(wedResource.getPrimarySource());
            subtask(tempWed.loadWEDTask());
            wed = tempWed;
        }
    }

    private class RenderOverlayTask extends JavaFXUtil.TaskManager.ManagedTask<BufferedImage>
    {
        private final int overlayIndex;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public RenderOverlayTask(final int overlayIndex)
        {
            this.overlayIndex = overlayIndex;
        }

        ///////////////////////
        // Protected Methods //
        ///////////////////////

        @Override
        protected BufferedImage call() throws Exception
        {
            buffer = source.demandFileData();
            return renderOverlay(overlayIndex);
        }

        private BufferedImage renderOverlay(final int overlayIndex) throws Exception
        {
            final WED.Overlay overlay = wed.getOverlays().get(overlayIndex);
            final String tisResref = overlay.getTilesetResref();

            if (tisResref.isEmpty())
            {
                // Weirdly happens on real overlays(?)
                return null;
            }

            final TIS tis = subtask(new LoadTISTask(tisResref));
            final ArrayList<WED.TilemapEntry> tilemapEntries = overlay.getTilemapEntries();
            int tilemapEntryI = 0;

            final int overlayWidth = overlay.getWidthInTiles();
            final int overlayHeight = overlay.getHeightInTiles();
            final int overlayWidthInPixels = overlayWidth * 64;
            final int overlayHeightInPixels = overlayHeight * 64;
            final int numPixels = overlayWidthInPixels * overlayHeightInPixels;
            int[] result = new int[numPixels];

            for (int tileY = 0; tileY < overlayHeight; ++tileY)
            {
                final int dstBaseYOffset = tileY * 64 * overlayWidthInPixels;

                for (int tileX = 0; tileX < overlayWidth; ++tileX)
                {
                    final WED.TilemapEntry tilemapEntry = tilemapEntries.get(tilemapEntryI++);
                    final short primaryTileIndex = tilemapEntry.tileIndexLookupArray[0];
                    final IntBuffer tileData = tis.getPreRenderedTileData(primaryTileIndex);

                    final int dstBaseXOffset = tileX * 64;

                    int srcOffset = 0;
                    int dstOffset = dstBaseXOffset + dstBaseYOffset;

                    for (int y = 0; y < 64; ++y, srcOffset += 64, dstOffset += overlayWidthInPixels)
                    {
                        tileData.get(srcOffset, result, dstOffset, 64);
                    }
                }
            }

            final BufferedImage image = new BufferedImage(
                overlayWidthInPixels, overlayHeightInPixels, BufferedImage.TYPE_INT_ARGB
            );
            image.getRaster().setDataElements(0, 0, overlayWidthInPixels, overlayHeightInPixels, result);

            return image;
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

            final List<WED.Overlay> overlays = wed.getOverlays();
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
}
