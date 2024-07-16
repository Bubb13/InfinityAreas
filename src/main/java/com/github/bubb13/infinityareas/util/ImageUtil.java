
package com.github.bubb13.infinityareas.util;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;

public final class ImageUtil
{
    ///////////////////////////
    // Private Static Fields //
    ///////////////////////////

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

    ///////////////////////////
    // Public Static Methods //
    ///////////////////////////

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

    ////////////////////////////
    // Private Static Methods //
    ////////////////////////////

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

    //////////////////////////
    // Private Constructors //
    //////////////////////////

    private ImageUtil() {}
}
