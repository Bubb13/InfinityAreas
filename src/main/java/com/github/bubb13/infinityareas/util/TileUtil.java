
package com.github.bubb13.infinityareas.util;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
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
        final int tileSideLength, final int dstPitch, final int dstOffset,
        // render specifics
        final int dwAlpha, final int dwFlags,
        // tile and stencil tile data
        final int[] tilePaletteData, final byte[] tilePalettedData, final byte[] stencilTilePalettedData,
        // destination
        final int[] dst)
    {
        final int dstLineAdvance = dstPitch - tileSideLength;
        int curPalettedIndex = 0;
        int curDstIndex = dstOffset;

        for (int yCounter = 0; yCounter < tileSideLength; ++yCounter, curDstIndex += dstLineAdvance)
        {
            for (int xCounter = 0; xCounter < tileSideLength; ++xCounter, ++curPalettedIndex, ++curDstIndex)
            {
                final short paletteIndex = MiscUtil.toUnsignedByte(tilePalettedData[curPalettedIndex]);
                int color = tilePaletteData[paletteIndex];

                if (stencilTilePalettedData[curPalettedIndex] == 0)
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

                dst[curDstIndex] = color;
            }
        }
    }

    public static void drawStenciledTo(
        // miscellaneous dimensions
        final int tileSideLength, final int x, final int y,
        // render specifics
        final int dwAlpha, final int dwFlags,
        // tile and stencil tile data
        final int[] tilePaletteData, final byte[] tilePalettedData, final byte[] stencilTilePalettedData,
        // destination
        final Graphics2D graphics)
    {
        final int[] realized = new int[tileSideLength * tileSideLength];

        copyStenciledTo(tileSideLength, tileSideLength, 0, dwAlpha, dwFlags,
            tilePaletteData, tilePalettedData, stencilTilePalettedData, realized);

        drawTileData(graphics, tileSideLength, realized, x, y);
    }

    public static void drawTileData(
        final Graphics2D graphics, final int tileSideLength,
        final int[] tileData, final int x, final int y)
    {
        final BufferedImage image = new BufferedImage(tileSideLength, tileSideLength, BufferedImage.TYPE_INT_ARGB);
        image.getRaster().setDataElements(0, 0, tileSideLength, tileSideLength, tileData);
        graphics.drawImage(image, x, y, null);
    }

    public static void drawTileData(
        final Graphics2D graphics, final int tileSideLength,
        final IntBuffer tileData, final int x, final int y)
    {
        drawTileData(graphics, tileSideLength, tileData.array(), x, y);
    }

    public static void drawClassicStenciledTo(
        // miscellaneous dimensions
        final int tileSideLength, final int x, final int y,
        // tile and stencil tile data
        final int[] tilePaletteData, final byte[] tilePalettedData, final byte[] stencilTilePalettedData,
        // destination
        final Graphics2D graphics)
    {
        final int[] realized = new int[tileSideLength * tileSideLength];

        classicCopyStenciledTo(tileSideLength, tileSideLength, 0,
            tilePaletteData, tilePalettedData, stencilTilePalettedData, realized);

        drawTileData(graphics, tileSideLength, realized, x, y);
    }

    public static void copyAlphaTo(
        // miscellaneous dimensions
        final int tileSideLength, final int dstPitch, final int dstOffset,
        // render specifics
        final int dwAlpha,
        // tile and stencil tile data
        final int[] tileData,
        // destination
        final int[] dst,
        final PrintWriter debugWriter)
    {
        final int dstLineAdvance = dstPitch - tileSideLength;
        int curPalettedIndex = 0;
        int curDstIndex = dstOffset;

        for (int yCounter = 0; yCounter < tileSideLength; ++yCounter, curDstIndex += dstLineAdvance)
        {
            for (int xCounter = 0; xCounter < tileSideLength; ++xCounter, ++curPalettedIndex, ++curDstIndex)
            {
                int color = multAlpha(tileData[curPalettedIndex], dwAlpha);
                color = blend_srcAlpha_OneMinusSrcAlpha(color, dst[curDstIndex]);
                if (debugWriter != null)
                {
                    debugWriter.printf("final color: 0x%X\n", color);
                }
                dst[curDstIndex] = color;
            }
        }
    }

    public static void drawAlphaTo(
        // miscellaneous dimensions
        final int tileSideLength, final int x, final int y,
        // render specifics
        final int dwAlpha,
        // tile and stencil tile data
        final int[] tileData,
        // destination
        final BufferedImage image,
        final PrintWriter debugWriter)
    {
        final int[] realized = new int[tileSideLength * tileSideLength];
        image.getRaster().getDataElements(x, y, tileSideLength, tileSideLength, realized);
        copyAlphaTo(tileSideLength, tileSideLength, 0, dwAlpha, tileData, realized, debugWriter);
        drawTileData(image.createGraphics(), tileSideLength, realized, x, y);
    }

    private static int blend_srcAlpha_OneMinusSrcAlpha(final int src, final int dst)
    {
        final short srcA = (short)((src >>> 24) & 0xFF);
        final short srcR = (short)((src >>> 16) & 0xFF);
        final short srcG = (short)((src >>> 8) & 0xFF);
        final short srcB = (short)(src & 0xFF);

        final short dstA = (short)((dst >>> 24) & 0xFF);
        final short dstR = (short)((dst >>> 16) & 0xFF);
        final short dstG = (short)((dst >>> 8) & 0xFF);
        final short dstB = (short)(dst & 0xFF);

        final double srcFactor = (double)srcA / 255;
        final double dstFactor = 1 - (double)srcA / 255;
        final byte finA = (byte)(srcFactor * srcA + dstFactor * dstA);
        final byte finR = (byte)(srcFactor * srcR + dstFactor * dstR);
        final byte finG = (byte)(srcFactor * srcG + dstFactor * dstG);
        final byte finB = (byte)(srcFactor * srcB + dstFactor * dstB);

        return MiscUtil.packBytesIntoInt(finA, finR, finG, finB);
    }

    private static int multAlpha(final int src, final int alpha)
    {
        final short srcA = (short)((src >>> 24) & 0xFF);
        final short srcR = (short)((src >>> 16) & 0xFF);
        final short srcG = (short)((src >>> 8) & 0xFF);
        final short srcB = (short)(src & 0xFF);

        final double srcFactor = (double)((alpha >>> 24) & 0xFF) / 255;
        final byte finA = (byte)(srcFactor * srcA);
        final byte finR = (byte)(srcFactor * srcR);
        final byte finG = (byte)(srcFactor * srcG);
        final byte finB = (byte)(srcFactor * srcB);

        return MiscUtil.packBytesIntoInt(finA, finR, finG, finB);
    }

    public static void drawAlphaTo(
        // miscellaneous dimensions
        final int tileSideLength, final int x, final int y,
        // render specifics
        final int dwAlpha,
        // tile and stencil tile data
        final IntBuffer tileData,
        // destination
        final BufferedImage image,
        final PrintWriter debugWriter)
    {
        drawAlphaTo(tileSideLength, x, y, dwAlpha, tileData.array(), image, debugWriter);
    }

    public static void classicCopyStenciledTo(
        // miscellaneous dimensions
        final int tileSideLength, final int dstPitch, final int dstOffset,
        // tile and stencil tile data
        final int[] tilePaletteData, final byte[] tilePalettedData, final byte[] stencilTilePalettedData,
        // destination
        final int[] dst)
    {
        final int dstLineAdvance = dstPitch - tileSideLength;
        int curPalettedIndex = 0;
        int curDstIndex = dstOffset;

        for (int yCounter = 0; yCounter < tileSideLength; ++yCounter, curDstIndex += dstLineAdvance)
        {
            for (int xCounter = 0; xCounter < tileSideLength; ++xCounter, ++curPalettedIndex, ++curDstIndex)
            {
                final short paletteIndex = MiscUtil.toUnsignedByte(tilePalettedData[curPalettedIndex]);

                if (stencilTilePalettedData[curPalettedIndex] != 0)
                {
                    dst[curDstIndex] = tilePaletteData[paletteIndex] |= 0xFF000000;
                }
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
