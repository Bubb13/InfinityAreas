
package com.github.bubb13.infinityareas.gui.region;

import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.util.ImageUtil;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;

/**
 * This class has been carefully designed to implement a custom scaling algorithm (nearest neighbor) while
 * avoiding constant memory allocations. The only way to draw pixels to a GraphicsContext object without
 * causing an (allocate + copy) of the buffer to be performed is to draw a WritableImage. <p>
 *
 * Some considerations: <p>
 *
 *   1) WritableImage instances hide their internal buffer. The only way to modify their pixels is via pixel writers.
 *      Writing pixels to a WritableImage instance one-by-one in this way is really slow. Thus, scaling operations
 *      save their result to an intermediate buffer, and this buffer is then written to a WritableImage instance. <p>
 *
 *   2) WritableImage instances always convert pixel data to the native format of the system. Thus, writing pixels
 *      that exist in a non-native format is also really slow. To mitigate this, the source image is converted to
 *      the correct format when it is initially set. <p>
 *
 *   3) Due to the asynchronous nature of the GraphicsContext object, multiple updates might be performed before a
 *      render pulse occurs. Therefore, multiple WritableImages might be created in order to queue multiple draws. <p>
 */
public class PartiallyRenderedImageLogic
{
    ////////////////////
    // Private Fields //
    ////////////////////

    /**
     * Since GraphicsContext::drawImage() executes some time in the future, the passed image cannot be
     * altered until after the next render pulse. This ArrayList, along with `toDrawImageIndex`, tracks
     * which cached WritableImage can be used to draw the next update. `toDrawImageIndex` is incremented
     * after every draw, and is reset to 0 on a render pulse. New WritableImage objects are allocated
     * as needed; they are never freed.
     */
    private final ArrayList<WritableImage> toDrawImages = new ArrayList<>();

    private BufferedImage sourceImage;

    private byte[] scaleBuffer;
    private int curToDrawImagesWidth = 0;
    private int curToDrawImagesHeight = 0;
    private int toDrawImageIndex = 0;

    private WritableImage latestCanvasBackground;

    private double srcScaleFactorX = 1;
    private double srcScaleFactorY = 1;
    private double opacity = 1;

    private int[] setArgbBuffer = new int[4];

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public PartiallyRenderedImageLogic()
    {
        init();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void setSourceImage(final BufferedImage sourceImage)
    {
        this.sourceImage = switch (sourceImage.getType())
        {
            case BufferedImage.TYPE_BYTE_BINARY -> ImageUtil.convertByteBinaryToBgraPre(sourceImage);
            case BufferedImage.TYPE_INT_ARGB -> ImageUtil.convertArgbToBgraPre(sourceImage);
            default -> throw new UnsupportedOperationException();
        };
    }

    public BufferedImage getSourceImage()
    {
        return sourceImage;
    }

    public WritableImage getLatestCanvasBackgroundImage()
    {
        return latestCanvasBackground;
    }

    public void setSrcScaleFactor(final double srcScaleFactorX, final double srcScaleFactorY)
    {
        this.srcScaleFactorX = srcScaleFactorX;
        this.srcScaleFactorY = srcScaleFactorY;
    }

    public double getSrcScaleFactorX()
    {
        return srcScaleFactorX;
    }

    public double getSrcScaleFactorY()
    {
        return srcScaleFactorY;
    }

    public void setOpacity(final double opacity)
    {
        this.opacity = opacity;
    }

    public void draw(
        final GraphicsContext graphics,
        final double srcX, final double srcY,
        final double dstX, final double dstY, final double dstWidth, final double dstHeight)
    {
        if (dstWidth <= 0 || dstHeight <= 0)
        {
            return;
        }

        final int snappedDstX = (int)dstX;
        final int snappedDstY = (int)dstY;
        final int snappedDstWidth = (int)dstWidth;
        final int snappedDstHeight = (int)dstHeight;

        checkGrowCacheImages(snappedDstWidth, snappedDstHeight);

        switch (GlobalState.getNativePixelFormatType())
        {
            case INT_ARGB_PRE, INT_ARGB, BYTE_BGRA, BYTE_RGB, BYTE_INDEXED
                -> throw new UnsupportedOperationException();
            case BYTE_BGRA_PRE ->
            {
                /////////////////////////////////////////
                // Get WritableImage to draw to Canvas //
                /////////////////////////////////////////

                WritableImage toDrawImage;

                if (toDrawImages.size() == toDrawImageIndex)
                {
                    toDrawImage = new WritableImage(curToDrawImagesWidth, curToDrawImagesHeight);
                    toDrawImages.add(toDrawImage);
                }
                else
                {
                    toDrawImage = toDrawImages.get(toDrawImageIndex);
                }

                ++toDrawImageIndex;

                /////////////////////////////////////////////////////////////////////////////////////////
                // Scale source image into immediate buffer and write that buffer to the WritableImage //
                /////////////////////////////////////////////////////////////////////////////////////////

                scaleBgra((int)srcX, (int)srcY, snappedDstWidth, snappedDstHeight);
                toDrawImage.getPixelWriter().setPixels(0, 0, snappedDstWidth, snappedDstHeight,
                    PixelFormat.getByteBgraPreInstance(), scaleBuffer, 0, snappedDstWidth * 4);

                latestCanvasBackground = toDrawImage;

                //////////////////////////////////////////
                // Draw the WritableImage to the Canvas //
                //////////////////////////////////////////

                graphics.setGlobalAlpha(opacity);
                graphics.drawImage(toDrawImage,
                    0, 0, snappedDstWidth, snappedDstHeight,
                    snappedDstX, snappedDstY, snappedDstWidth, snappedDstHeight
                );
                graphics.setGlobalAlpha(1);
            }
        }
    }

    // 'B' being the lowest bit
    // BufferedImage::setRGB() doesn't function correctly (for some reason)
    public void setPixelARGB(final int x, final int y, final int argb)
    {
        if (GlobalState.getNativePixelFormatType() == PixelFormat.Type.BYTE_BGRA_PRE)
        {
            setArgbBuffer[0] = argb & 0xFF;
            setArgbBuffer[1] = (argb >>> 8) & 0xFF;
            setArgbBuffer[2] = (argb >>> 16) & 0xFF;
            setArgbBuffer[3] = (argb >>> 24) & 0xFF;
            getSourceImage().getRaster().setPixel(x, y, setArgbBuffer);
        }
        else
        {
            throw new UnsupportedOperationException();
        }
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init()
    {
        new AnimationTimer()
        {
            @Override
            public void handle(final long now)
            {
                onRenderPulse();
            }
        }.start();
    }

    /**
     * Resets `toDrawImageIndex` on each render pulse, signaling that all cached image objects can be used again.
     */
    private void onRenderPulse()
    {
        toDrawImageIndex = 0;
    }

    private void checkGrowCacheImages(final int dstWidth, final int dstHeight)
    {
        // If a viewport dimension is greater than the currently cached dimensions,
        // grow the scale buffer and clear the drawing images (to be reallocated later).
        if (dstWidth > curToDrawImagesWidth || dstHeight > curToDrawImagesHeight)
        {
            if (dstWidth > curToDrawImagesWidth) curToDrawImagesWidth = dstWidth;
            if (dstHeight > curToDrawImagesHeight) curToDrawImagesHeight = dstHeight;

            scaleBuffer = new byte[curToDrawImagesWidth * curToDrawImagesHeight * 4];
            toDrawImages.clear();
            toDrawImageIndex = 0;
        }
    }

    private void scaleBgra(final int srcX, final int srcY, int dstW, int dstH)
    {
        final byte[] src = ((DataBufferByte)sourceImage.getRaster().getDataBuffer()).getData();
        final int srcWidth = sourceImage.getWidth();

        for (int y = 0, curDstOffset = 0; y < dstH; ++y)
        {
            final int curSrcY = (int)((srcY + y) / srcScaleFactorY);
            for (int x = 0; x < dstW; ++x, curDstOffset += 4)
            {
                final int curSrcX = (int)((srcX + x) / srcScaleFactorX);
                final int srcOffset = (curSrcY * srcWidth + curSrcX) * 4;
                System.arraycopy(src, srcOffset, scaleBuffer, curDstOffset, 4);
            }
        }
    }
}
