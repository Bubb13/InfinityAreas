
package com.github.bubb13.infinityareas.util;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;

public final class ImageUtil
{
    ///////////////////////////
    // Private Static Fields //
    ///////////////////////////

    //////////
    // ARGB //
    //////////

    private static final ColorModel ARGB_COLOR_MODEL = new DirectColorModel(
        32,     // Pixel size
        0x00FF0000, // Red mask
        0x0000FF00, // Green mask
        0x000000FF, // Blue mask
        0xFF000000  // Alpha mask
    );

    private static final int[] ARGB_BIT_MASKS = {
        0x00FF0000, // Red mask
        0x0000FF00, // Green mask
        0x000000FF, // Blue mask
        0xFF000000  // Alpha mask
    };

    //////////////
    // BGRA Pre //
    //////////////

    private static final ColorModel BGRA_PRE_COLOR_MODEL = new ComponentColorModel(
        ColorSpace.getInstance(ColorSpace.CS_sRGB),
        new int[] { 8, 8, 8, 8 },
        true,
        true,
        Transparency.TRANSLUCENT,
        DataBuffer.TYPE_BYTE
    );

    private static final int[] BGRA_PRE_OFFSETS = {
        0, // Blue
        1, // Green
        2, // Red
        3  // Alpha
    };

    ///////////////////////////
    // Public Static Methods //
    ///////////////////////////

    //////////
    // ARGB //
    //////////

    public static BufferedImage wrapArgb(final int[] sourceData, final int width, final int height)
    {
        return createArgbBufferedImageFromRaster(createArgbRasterFromIntArray(sourceData, width, height));
    }

    public static BufferedImage copyArgb(final BufferedImage source)
    {
        if (source.getRaster().getDataBuffer() instanceof DataBufferInt sourceDataBufferInt)
        {
            // Copy source data
            final int[] sourceData = sourceDataBufferInt.getData();
            final int[] copyData = new int[sourceData.length];
            System.arraycopy(sourceData, 0, copyData, 0, sourceData.length);

            // Create new BufferedImage
            final WritableRaster raster = createArgbRasterFromIntArray(copyData, source.getWidth(), source.getHeight());
            return createArgbBufferedImageFromRaster(raster);
        }
        throw new UnsupportedOperationException("Unimplemented");
    }

    //////////////
    // BGRA Pre //
    //////////////

    public static BufferedImage convertArgbToBgraPre(final BufferedImage image)
    {
        if (image.getType() != BufferedImage.TYPE_INT_ARGB)
        {
            throw new UnsupportedOperationException();
        }

        final int imageWidth = image.getWidth();
        final int imageHeight = image.getHeight();

        final byte[] dstBuffer = new byte[imageWidth * imageHeight * 4];
        final int[] srcBuffer = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();

        for (int i = 0, dstIndex = 0; i < srcBuffer.length; ++i)
        {
            final int src = srcBuffer[i];

            final short srcAlpha = (short)((src >>> 24) & 0xFF);
            final double alphaRatio = (double)srcAlpha / 255;

            dstBuffer[dstIndex++] = (byte)(alphaRatio * (src & 0xFF));
            dstBuffer[dstIndex++] = (byte)(alphaRatio * ((src >>> 8) & 0xFF));
            dstBuffer[dstIndex++] = (byte)(alphaRatio * ((src >>> 16) & 0xFF));
            dstBuffer[dstIndex++] = (byte)srcAlpha;
        }

        return ImageUtil.wrapBgra(dstBuffer, imageWidth, imageHeight);
    }

    public static BufferedImage wrapBgra(final byte[] sourceData, final int width, final int height)
    {
        return createBgraBufferedImageFromRaster(createBgraRasterFromByteArray(sourceData, width, height));
    }

    ////////////////////////////
    // Private Static Methods //
    ////////////////////////////

    //////////
    // ARGB //
    //////////

    private static WritableRaster createArgbRasterFromIntArray(final int[] data, final int width, final int height)
    {
        final DataBufferInt dataBuffer = new DataBufferInt(data, data.length);
        final SampleModel sampleModel = new SinglePixelPackedSampleModel(
            DataBuffer.TYPE_INT, width, height, ARGB_BIT_MASKS
        );
        return WritableRaster.createWritableRaster(sampleModel, dataBuffer, null);
    }

    private static BufferedImage createArgbBufferedImageFromRaster(final WritableRaster raster)
    {
        return new BufferedImage(ARGB_COLOR_MODEL, raster, ARGB_COLOR_MODEL.isAlphaPremultiplied(), null);
    }

    //////////////
    // BGRA Pre //
    //////////////

    private static WritableRaster createBgraRasterFromByteArray(final byte[] data, final int width, final int height)
    {
        final DataBufferByte dataBuffer = new DataBufferByte(data, data.length);
        final SampleModel sampleModel = new PixelInterleavedSampleModel(
            DataBuffer.TYPE_BYTE, width, height, 4, 4 * width, BGRA_PRE_OFFSETS
        );
        return WritableRaster.createWritableRaster(sampleModel, dataBuffer, null);
    }

    private static BufferedImage createBgraBufferedImageFromRaster(final WritableRaster raster)
    {
        return new BufferedImage(BGRA_PRE_COLOR_MODEL, raster,
            BGRA_PRE_COLOR_MODEL.isAlphaPremultiplied(), null);
    }

    //////////////////////////
    // Private Constructors //
    //////////////////////////

    private ImageUtil() {}
}
