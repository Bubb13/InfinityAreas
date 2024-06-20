
package com.github.bubb13.infinityareas.game.resource;

import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.misc.SimpleCache;
import com.github.bubb13.infinityareas.util.BufferUtil;
import com.github.bubb13.infinityareas.util.JavaFXUtil;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

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

            final TIS tis = subtask(new LoadTISTask(wed.getSource().getIdentifier().resref(), tisResref));
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
                    final IntBuffer tileData = tis.getTileData(primaryTileIndex);

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

    private class LoadTISTask extends JavaFXUtil.TaskManager.ManagedTask<TIS>
    {
        final String wedResref;
        final String tisResref;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public LoadTISTask(final String wedResref, final String tisResref)
        {
            this.wedResref = wedResref;
            this.tisResref = tisResref;
        }

        ///////////////////////
        // Protected Methods //
        ///////////////////////

        @Override
        protected TIS call() throws Exception
        {
            return loadTIS(wedResref, tisResref);
        }

        private TIS loadTIS(final String wedResref, final String tisResref) throws Exception
        {
            TIS tis = tisCache.get(tisResref);

            if (tis == null)
            {
                final Game.Resource tisResource = GlobalState.getGame().getResource(new ResourceIdentifier(
                    tisResref, KeyFile.NumericResourceType.TIS));

                if (tisResource == null)
                {
                    throw new IllegalStateException("Unable to find source for TIS resource \"" + wedResref + "\"");
                }

                final String wedNumeric = wedResref.substring(2);
                tis = new TIS(wedNumeric, tisResource.getPrimarySource(), resourceDataCache, pvrzCache);
                subtask(tis.loadTISTask());

                tisCache.add(tisResref, tis);
            }

            return tis;
        }
    }
}
