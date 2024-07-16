
package com.github.bubb13.infinityareas.util;

import java.io.PrintWriter;

public class DrawUtil
{
    ///////////////////////////
    // Public Static Methods //
    ///////////////////////////

    public static void drawAlpha(
        // miscellaneous dimensions
        final int drawWidth, final int drawHeight,
        final int srcPitch, final int srcOffset,
        final int dstPitch, final int dstOffset,
        // render specifics
        final int dwAlpha,
        // tile source data
        final int[] src,
        // destination data
        final int[] dst,
        final PrintWriter debugWriter)
    {
        final int srcLineAdvance = srcPitch - drawWidth;
        final int dstLineAdvance = dstPitch - drawWidth;

        int curSrcIndex = srcOffset;
        int curDstIndex = dstOffset;

        for (int yCounter = 0; yCounter < drawHeight; ++yCounter, curSrcIndex += srcLineAdvance, curDstIndex += dstLineAdvance)
        {
            for (int xCounter = 0; xCounter < drawWidth; ++xCounter, ++curSrcIndex, ++curDstIndex)
            {
                int color = src[curSrcIndex];
                color = multAlpha(color, dwAlpha);
                int dstColor = dst[curDstIndex];
                color = blend_srcAlpha_OneMinusSrcAlpha(color, dstColor);
                color = temp(color, dstColor);
                dst[curDstIndex] = color;
            }
        }
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

    private static int multOnlyAlpha(final int src, final int alpha)
    {
        final short srcA = (short)((src >>> 24) & 0xFF);
        final byte srcR = (byte)((src >>> 16) & 0xFF);
        final byte srcG = (byte)((src >>> 8) & 0xFF);
        final byte srcB = (byte)(src & 0xFF);

        final double srcFactor = (double)((alpha >>> 24) & 0xFF) / 255;
        final byte finA = (byte)(srcFactor * srcA);

        return MiscUtil.packBytesIntoInt(finA, srcR, srcG, srcB);
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
        byte finA = (byte)(srcFactor * srcA + dstFactor * dstA);
        byte finR = (byte)(srcFactor * srcR + dstFactor * dstR);
        byte finG = (byte)(srcFactor * srcG + dstFactor * dstG);
        byte finB = (byte)(srcFactor * srcB + dstFactor * dstB);

        return MiscUtil.packBytesIntoInt(finA, finR, finG, finB);
    }

    private static int backup(final int src, final int dst)
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
        final short finA1 = (short)(srcFactor * srcA + dstFactor * dstA);
        final short finR1 = (short)(srcFactor * srcR + dstFactor * dstR);
        final short finG1 = (short)(srcFactor * srcG + dstFactor * dstG);
        final short finB1 = (short)(srcFactor * srcB + dstFactor * dstB);

        final double dstFactor2 = 1 - (double)finA1 / 255;
        final byte finA2 = (byte)(finA1 + dstFactor2 * dstA);
        final byte finR2 = (byte)(finR1 + dstFactor2 * dstR);
        final byte finG2 = (byte)(finG1 + dstFactor2 * dstG);
        final byte finB2 = (byte)(finB1 + dstFactor2 * dstB);

        return MiscUtil.packBytesIntoInt(finA2, finR2, finG2, finB2);
    }

    private static int temp(final int src, final int dst)
    {
        final short srcA = (short)((src >>> 24) & 0xFF);
        final short srcR = (short)((src >>> 16) & 0xFF);
        final short srcG = (short)((src >>> 8) & 0xFF);
        final short srcB = (short)(src & 0xFF);

        final short dstA = (short)((dst >>> 24) & 0xFF);
        final short dstR = (short)((dst >>> 16) & 0xFF);
        final short dstG = (short)((dst >>> 8) & 0xFF);
        final short dstB = (short)(dst & 0xFF);

        final double dstFactor = 1 - (double)srcA / 255;
        final byte finA = (byte)(srcA + dstFactor * dstA);
        final byte finR = (byte)(srcR + dstFactor * dstR);
        final byte finG = (byte)(srcG + dstFactor * dstG);
        final byte finB = (byte)(srcB + dstFactor * dstB);

        return MiscUtil.packBytesIntoInt(finA, finR, finG, finB);
    }

    //////////////////////////
    // Private Constructors //
    //////////////////////////

    private DrawUtil() {}
}
