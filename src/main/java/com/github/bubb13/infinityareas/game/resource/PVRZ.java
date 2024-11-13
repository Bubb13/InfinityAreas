
package com.github.bubb13.infinityareas.game.resource;

import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.misc.tasktracking.TaskTracker;
import com.github.bubb13.infinityareas.misc.tasktracking.TaskTrackerI;
import com.github.bubb13.infinityareas.misc.tasktracking.TrackedTask;
import com.github.bubb13.infinityareas.util.MiscUtil;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.function.Function;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class PVRZ
{
    ///////////////////////////
    // Private Static Fields //
    ///////////////////////////

    private static final int HEADER_SIZE = 0x34;

    ////////////////////
    // Private Fields //
    ////////////////////

    private final Game.ResourceSource source;
    private final ResourceDataCache resourceDataCache;
    private ByteBuffer buffer;

    private int mipMapCount;
    private int numSurfaces;
    private int numFaces;
    private int depth;
    private int width;
    private int height;
    private int textureDataOffset;
    private Function<Integer, IntBuffer> pixelDataDecompressor;

    private int pixelDataBlockSize;
    private int sliceBlockSize;
    private int faceBlockSize;
    private int surfaceBlockSize;
    private int mipMapBlockSize;

    private IntBuffer decompressedData;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public PVRZ(final Game.ResourceSource source, final ResourceDataCache resourceDataCache)
    {
        this.source = source;
        this.resourceDataCache = resourceDataCache;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void load(final TaskTrackerI tracker) throws Exception
    {
        tracker.subtask(this::loadInternal);
    }

    public void load() throws Exception
    {
        loadInternal(TaskTracker.DUMMY);
    }

    public TrackedTask<Void> loadTask()
    {
        return new TrackedTask<>()
        {
            @Override
            protected Void doTask(final TaskTrackerI tracker) throws Exception
            {
                subtask(PVRZ.this::loadInternal);
                return null;
            }
        };
    }

    public IntBuffer cutout(
        final int[] dst, int dstOffset,
        final int cutoutX, final int cutoutY, final int cutoutW, final int cutoutH)
    {
        final int dstSize = cutoutW * cutoutH;
        for (
            int srcOffset = cutoutY * this.width + cutoutX;
            dstOffset < dstSize;
            srcOffset += this.width, dstOffset += cutoutW)
        {
            decompressedData.get(srcOffset, dst, dstOffset, cutoutW);
        }
        return IntBuffer.wrap(dst);
    }

    public IntBuffer cutout(final int cutoutX, final int cutoutY, final int cutoutW, final int cutoutH)
    {
        return cutout(new int[cutoutW * cutoutH], 0, cutoutX, cutoutY, cutoutW, cutoutH);
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void position(final int pos)
    {
        buffer.position(pos);
    }

    ////////////////////////
    // START Loading PVRZ //
    ////////////////////////

    private void loadInternal(final TaskTrackerI tracker) throws Exception
    {
        buffer = resourceDataCache.demand(source);

        tracker.updateProgress(0, 100);
        tracker.updateMessage("Processing PVRZ ...");

        buffer = decompressPVR();
        parsePVRHeader();
        decompressedData = parseTextureData(0, 0, 0, 0);
    }

//    private void parsePVR_v2_Header()
//    {
//        position(0x0); final int headerSize = buffer.getInt();
//        position(0x4); final int height = buffer.getInt();
//        position(0x8); final int width = buffer.getInt();
//        position(0xC); final int mipMapCount = buffer.getInt();
//        position(0x10); final byte pixelFormat = buffer.get();
//        position(0x11); final int flags = buffer.get() | (buffer.get() << 0x8) | (buffer.get() << 0x10);
//        position(0x14); final int surfaceSize = buffer.getInt();
//        position(0x18); final int bitsPerPixel = buffer.getInt();
//        position(0x1C); final int redMask = buffer.getInt();
//        position(0x20); final int greenMask = buffer.getInt();
//        position(0x24); final int blueMask = buffer.getInt();
//        position(0x28); final int alphaMask = buffer.getInt();
//        position(0x2C); final String pvrIdentifier = BufferUtil.readUTF8(buffer, 4);
//        position(0x30); final int numSurfaces = buffer.getInt();
//
//        System.out.printf("headerSize: %d\n", headerSize);
//        System.out.printf("height: %d\n", height);
//        System.out.printf("width: %d\n", width);
//        System.out.printf("mipMapCount: %d\n", mipMapCount);
//        System.out.printf("pixelFormat: %d\n", pixelFormat);
//        System.out.printf("flags: 0x%X\n", flags);
//        System.out.printf("surfaceSize: %d\n", surfaceSize);
//        System.out.printf("bitsPerPixel: %d\n", bitsPerPixel);
//        System.out.printf("redMask: %d\n", redMask);
//        System.out.printf("greenMask: %d\n", greenMask);
//        System.out.printf("blueMask: %d\n", blueMask);
//        System.out.printf("alphaMask: %d\n", alphaMask);
//        System.out.printf("pvrIdentifier: \"%s\"\n", pvrIdentifier);
//        System.out.printf("numSurfaces: %d\n", numSurfaces);
//    }

    private void parsePVRHeader()
    {
        position(0x0); final int version = buffer.getInt();
        if (version != 0x3525650)
        {
            throw new IllegalStateException(String.format("Unexpected PVR version: 0x%X", version));
        }

        position(0x4); final int flags = buffer.getInt();
        if (flags != 0x0)
        {
            throw new IllegalStateException(String.format("Unexpected PVR flags: 0x%X", version));
        }

        position(0x8); final long pixelFormat = buffer.getLong();
        if (pixelFormat != 7)
        {
            throw new IllegalStateException(String.format("Unexpected PVR pixelFormat: %d", pixelFormat));
        }

        position(0x10); final int colorSpace = buffer.getInt();
        if (colorSpace != 0)
        {
            throw new IllegalStateException(String.format("Unexpected PVR colorSpace: %d", colorSpace));
        }

        position(0x14); final int channelType = buffer.getInt();
        if (channelType != 0)
        {
            throw new IllegalStateException(String.format("Unexpected PVR channelType: %d", channelType));
        }

        position(0x18); final int height = buffer.getInt();
        position(0x1C); final int width = buffer.getInt();

        position(0x20); final int depth = buffer.getInt();
        if (depth != 1)
        {
            throw new IllegalStateException(String.format("Unexpected PVR depth: %d", depth));
        }

        position(0x24); final int numSurfaces = buffer.getInt();
        if (numSurfaces != 1)
        {
            throw new IllegalStateException(String.format("Unexpected PVR numSurfaces: %d", numSurfaces));
        }

        position(0x28); final int numFaces = buffer.getInt();
        if (numFaces != 1)
        {
            throw new IllegalStateException(String.format("Unexpected PVR numFaces: %d", numFaces));
        }

        position(0x2C); final int mipMapCount = buffer.getInt();
        if (mipMapCount != 1)
        {
            throw new IllegalStateException(String.format("Unexpected PVR mipMapCount: %d", mipMapCount));
        }

        position(0x30); final int metaDataSize = buffer.getInt();
        if (metaDataSize != 0)
        {
            throw new IllegalStateException(String.format("Unexpected PVR metaDataSize: %d", metaDataSize));
        }

        PVRZ.this.mipMapCount = mipMapCount;
        PVRZ.this.numSurfaces = numSurfaces;
        PVRZ.this.numFaces = numFaces;
        PVRZ.this.depth = depth;
        PVRZ.this.width = width;
        PVRZ.this.height = height;
        textureDataOffset = PVRZ.HEADER_SIZE + metaDataSize;

        if (pixelFormat == 7)
        {
            pixelDataDecompressor = this::decompressBC1;
            pixelDataBlockSize = width * height / 2;
        }
        else
        {
            throw new IllegalStateException("Unimplemented");
        }

        sliceBlockSize = pixelDataBlockSize * depth;
        faceBlockSize = sliceBlockSize * numFaces;
        surfaceBlockSize = faceBlockSize * numSurfaces;
        mipMapBlockSize = surfaceBlockSize * mipMapCount;

//            System.out.printf("version: 0x%X\n", version);
//            System.out.printf("flags: 0x%X\n", flags);
//            System.out.printf("pixelFormat: %d\n", pixelFormat);
//            System.out.printf("colorSpace: %d\n", colorSpace);
//            System.out.printf("channelType: %d\n", channelType);
//            System.out.printf("height: %d\n", height);
//            System.out.printf("width: %d\n", width);
//            System.out.printf("depth: %d\n", depth);
//            System.out.printf("numSurfaces: %d\n", numSurfaces);
//            System.out.printf("numFaces: %d\n", numFaces);
//            System.out.printf("mipMapCount: %d\n", mipMapCount);
//            System.out.printf("metaDataSize: %d\n", metaDataSize);
    }

    private ByteBuffer decompressPVR() throws Exception
    {
        try (
            final ByteArrayInputStream inputStream = new ByteArrayInputStream(
                buffer.array(), 4, buffer.limit() - 4
            );
            final InflaterInputStream inflaterInputStream = new InflaterInputStream(inputStream, new Inflater()))
        {
            final ByteBuffer decompressedBuffer = ByteBuffer.wrap(inflaterInputStream.readAllBytes());
            decompressedBuffer.order(ByteOrder.LITTLE_ENDIAN);
            return decompressedBuffer;
        }
    }

    private IntBuffer parseTextureData(
        final int mipMapIndex, final int surfaceIndex, final int faceIndex, final int sliceIndex)
    {
        // for each MIP-Map Level in MIP-Map Count
        //     for each Surface in Num. Surfaces
        //         for each Face in Num. Faces
        //             for each Slice in Depth
        //                 for each Row in Height
        //                     for each Pixel in Width
        //                         Byte data[Size_Based_On_PixelFormat]
        //                     end
        //                 end
        //             end
        //         end
        //     end
        // end

        if (mipMapIndex >= mipMapCount)
        {
            throw new IllegalArgumentException(String.format(
                "Attempted to access invalid mipMapIndex: %d", mipMapIndex));
        }

        if (surfaceIndex >= numSurfaces)
        {
            throw new IllegalArgumentException(String.format(
                "Attempted to access invalid surfaceIndex: %d", surfaceIndex));
        }

        if (faceIndex >= numFaces)
        {
            throw new IllegalArgumentException(String.format(
                "Attempted to access invalid faceIndex: %d", faceIndex));
        }

        if (sliceIndex >= depth)
        {
            throw new IllegalArgumentException(String.format(
                "Attempted to access invalid sliceIndex: %d", sliceIndex));
        }

        final int pixelDataOffset =
            textureDataOffset
            + mipMapIndex * mipMapBlockSize
            + surfaceIndex * surfaceBlockSize
            + faceIndex * faceBlockSize
            + sliceIndex * sliceBlockSize;

        return pixelDataDecompressor.apply(pixelDataOffset);
    }

    private IntBuffer decompressBC1(final int pixelDataOffset)
    {
        final int numBlocksHorizontal = width / 4;
        if (width % 4 != 0)
        {
            throw new IllegalStateException(String.format(
                "BC1 expects width to be a multiple of 4; got: %d", width));
        }

        final int numBlocksVertical = height / 4;
        if (height % 4 != 0)
        {
            throw new IllegalStateException(String.format(
                "BC1 expects height to be a multiple of 4; got: %d", height));
        }

        final int[] result = new int[width * height];

        int curTexelBlockOffset = pixelDataOffset;

        for (int texelBlockY = 0; texelBlockY < numBlocksVertical; ++texelBlockY)
        {
            for (int texelBlockX = 0; texelBlockX < numBlocksHorizontal; ++texelBlockX, curTexelBlockOffset += 8)
            {
                position(curTexelBlockOffset);       final int color0 = unpackBC1Color(buffer.getShort());
                position(curTexelBlockOffset + 0x2); final int color1 = unpackBC1Color(buffer.getShort());

                // See: https://learn.microsoft.com/en-us/windows/uwp/graphics-concepts/opaque-and-1-bit-alpha-textures
                //  and https://learn.microsoft.com/en-us/windows/win32/direct3d10/d3d10-graphics-programming-guide-resources-block-compression#bc1
                final int color2;
                final int color3;
                if (color0 > color1)
                {
                    color2 = bc1Interpolate(1, 3, color0, color1);
                    color3 = bc1Interpolate(2, 3, color0, color1);
                }
                else
                {
                    color2 = bc1Interpolate(1, 2, color0, color1);
                    color3 = 0x00000000;
                }

                final int[] colorTable = new int[]{color0, color1, color2, color3};

                for (int blockLineIndex = 0; blockLineIndex < 4; ++blockLineIndex)
                {
                    position(curTexelBlockOffset + 0x4 + blockLineIndex); final byte fourTexels = buffer.get();
                    final byte texel0 = (byte)(fourTexels & 0x3);
                    final byte texel1 = (byte)((fourTexels >>> 2) & 0x3);
                    final byte texel2 = (byte)((fourTexels >>> 4) & 0x3);
                    final byte texel3 = (byte)((fourTexels >>> 6) & 0x3);

                    final int verticalBlockAlign = texelBlockY * 4 * width;
                    final int horizontalBlockAlign = texelBlockX * 4;
                    final int pixelLineAlign = blockLineIndex * width;
                    final int resultLineStart = verticalBlockAlign + horizontalBlockAlign + pixelLineAlign;

                    result[resultLineStart] = colorTable[texel0];
                    result[resultLineStart + 1] = colorTable[texel1];
                    result[resultLineStart + 2] = colorTable[texel2];
                    result[resultLineStart + 3] = colorTable[texel3];
                }
            }
        }

        return IntBuffer.wrap(result);
    }

    private int unpackBC1Color(final short packedColor)
    {
        // BC1 pixel data is in the format:
        //     MSB               LSB
        //     r[15-11]g[10-5]b[4-0]
        final byte b = (byte)((packedColor & 0x1F) * 255 / 0x1F);
        final byte g = (byte)(((packedColor >>> 5) & 0x3F) * 255 / 0x3F);
        final byte r = (byte)(((packedColor >>> 11) & 0x1F) * 255 / 0x1F);
        final byte a = (byte)255;
        return MiscUtil.packBytesIntoInt(a, r, g, b);
    }

    private int bc1Interpolate(
        final int fracColor2Upper, final int fracColorLower, final int color1, final int color2)
    {
        final int fracColor1Upper = fracColorLower - fracColor2Upper;

        final short b1 = (short)(color1 & 0xFF);
        final short g1 = (short)((color1 >>> 8) & 0xFF);
        final short r1 = (short)((color1 >>> 16) & 0xFF);
        final short a1 = (short)((color1 >>> 24) & 0xFF);

        final short b2 = (short)(color2 & 0xFF);
        final short g2 = (short)((color2 >>> 8) & 0xFF);
        final short r2 = (short)((color2 >>> 16) & 0xFF);
        final short a2 = (short)((color2 >>> 24) & 0xFF);

        final byte aFinal = (byte)((fracColor1Upper * a1 + fracColor2Upper * a2 + 1) / fracColorLower);
        final byte rFinal = (byte)((fracColor1Upper * r1 + fracColor2Upper * r2 + 1) / fracColorLower);
        final byte gFinal = (byte)((fracColor1Upper * g1 + fracColor2Upper * g2 + 1) / fracColorLower);
        final byte bFinal = (byte)((fracColor1Upper * b1 + fracColor2Upper * b2 + 1) / fracColorLower);
        return MiscUtil.packBytesIntoInt(aFinal, rFinal, gFinal, bFinal);
    }
}
