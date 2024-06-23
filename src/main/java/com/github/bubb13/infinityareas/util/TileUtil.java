
package com.github.bubb13.infinityareas.util;

import java.nio.IntBuffer;

public final class TileUtil
{
    public static void iterateOverlayTileOffsets(
        final int tileSideLength, final int overlayWidth, final int overlayHeight, final IterateCallback callback)
    {
        final int overlayWidthInPixels = overlayWidth * tileSideLength;
        final int overlayTileLineAdvanceFull = tileSideLength * overlayWidthInPixels;
        final int overlayTileLineAdvanceItr = overlayTileLineAdvanceFull - overlayWidthInPixels;
        final int maxDstOffsetY = overlayHeight * overlayTileLineAdvanceFull;
        int i = 0;

        for (int dstOffset = 0; dstOffset < maxDstOffsetY; dstOffset += overlayTileLineAdvanceItr)
        {
            final int maxDstOffsetX = dstOffset + overlayWidthInPixels;
            for (; dstOffset < maxDstOffsetX; dstOffset += tileSideLength)
            {
                callback.call(tileSideLength, overlayWidthInPixels, dstOffset, i++);
            }
        }
    }

    public static void copyTo(
        final int tileSideLength, final int dstPitch, int dstOffset, final IntBuffer src, final int[] dst)
    {
        final int maxSrcOffset = tileSideLength * tileSideLength;
        for (int srcOffset = 0; srcOffset < maxSrcOffset; srcOffset += tileSideLength, dstOffset += dstPitch)
        {
            src.get(srcOffset, dst, dstOffset, tileSideLength);
        }
    }

    public static void copyStenciledTo(
        // miscellaneous dimensions
        final int tileSideLength, final int dstPitch,
        // render specifics
        final int dwAlpha, final int dwFlags,
        // tile and stencil tile data
        final int[] tilePaletteData, final byte[] tilePalettedData, final byte[] stencilTilePalettedData,
        // destination
        final int[] dst)
    {
        final int dstLineAdvance = dstPitch - tileSideLength;
        int curDataIndex = 0;
        int curDstIndex = 0;

        for (int yCounter = 0; yCounter < tileSideLength; ++yCounter, curDstIndex += dstLineAdvance)
        {
            for (int xCounter = 0; xCounter < tileSideLength; ++xCounter, ++curDataIndex)
            {
                final byte paletteIndex = tilePalettedData[curDataIndex];
                int color = tilePaletteData[paletteIndex];

                if (stencilTilePalettedData[curDataIndex] == 0)
                {
                    // Incorporate alpha parameter if stencil palette is 0 at location
                    color &= (dwAlpha << 24) | 0xFFFFFF;
                }

                if ((color & 0xFFFFFF) == 0xFF00)
                {
                    // If special-green (255) make the location transparent
                    color = 0;
                }

                if ((dwFlags & 0x4000000) == 0 && (color & 0xFF000000) != 0)
                {
                    // If any alpha is present and render flag 0x4000000 is unset make location 100% opaque
                    color |= 0xFF000000;
                }

                dst[curDstIndex++] = color;
            }
        }
    }

    @FunctionalInterface
    public interface IterateCallback
    {
        void call(int tileSideLength, int dstPitch, int dstOffset, int i);
    }

    private TileUtil() {}
}
