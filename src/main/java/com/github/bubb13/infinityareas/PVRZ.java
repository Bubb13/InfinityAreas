
package com.github.bubb13.infinityareas;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
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

    private static int HEADER_SIZE = 0x34;

    ////////////////////
    // Private Fields //
    ////////////////////

    private final Game.ResourceSource source;
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

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public PVRZ(final Game.ResourceSource source)
    {
        this.source = source;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public JavaFXUtil.TaskManager.ManagedTask<Void> loadPVRZTask()
    {
        return new LoadPVRZTask();
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

    /////////////////////
    // Private Classes //
    /////////////////////

    private class LoadPVRZTask extends JavaFXUtil.TaskManager.ManagedTask<Void>
    {
        ///////////////////////
        // Protected Methods //
        ///////////////////////

        @Override
        protected Void call() throws Exception
        {
            buffer = source.demandFileData();
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            parse();
            return null;
        }

        /////////////////////
        // Private Methods //
        /////////////////////

        private void parse() throws Exception
        {
            updateProgress(0, 100);
            updateMessage("Processing PVRZ ...");

            buffer = decompressPVR();
            //Files.write(GlobalState.getGame().getRoot().resolve("DECOMPRESSED.PVRZ"), buffer.array());

            parsePVRHeader();
            final IntBuffer imageData = parseTextureData(0, 0, 0, 0);

            final BufferedImage image = new BufferedImage(PVRZ.this.width, PVRZ.this.height, BufferedImage.TYPE_INT_BGR);
            final WritableRaster raster = image.getRaster();
            final DataBufferInt buffer = new DataBufferInt(
                imageData.array(), imageData.arrayOffset() + imageData.position(), imageData.limit()
            );
            raster.setDataElements(0, 0, PVRZ.this.width, PVRZ.this.height, buffer.getData());

            ImageIO.write(image, "png", GlobalState.getGame().getRoot().resolve("TILE.PNG").toFile());
        }

        private void parsePVR_v2_Header()
        {
            position(0x0); final int headerSize = buffer.getInt();
            position(0x4); final int height = buffer.getInt();
            position(0x8); final int width = buffer.getInt();
            position(0xC); final int mipMapCount = buffer.getInt();
            position(0x10); final byte pixelFormat = buffer.get();
            position(0x11); final int flags = buffer.get() | (buffer.get() << 0x8) | (buffer.get() << 0x10);
            position(0x14); final int surfaceSize = buffer.getInt();
            position(0x18); final int bitsPerPixel = buffer.getInt();
            position(0x1C); final int redMask = buffer.getInt();
            position(0x20); final int greenMask = buffer.getInt();
            position(0x24); final int blueMask = buffer.getInt();
            position(0x28); final int alphaMask = buffer.getInt();
            position(0x2C); final String pvrIdentifier = BufferUtil.readUTF8(buffer, 4);
            position(0x30); final int numSurfaces = buffer.getInt();

            System.out.printf("headerSize: %d\n", headerSize);
            System.out.printf("height: %d\n", height);
            System.out.printf("width: %d\n", width);
            System.out.printf("mipMapCount: %d\n", mipMapCount);
            System.out.printf("pixelFormat: %d\n", pixelFormat);
            System.out.printf("flags: 0x%X\n", flags);
            System.out.printf("surfaceSize: %d\n", surfaceSize);
            System.out.printf("bitsPerPixel: %d\n", bitsPerPixel);
            System.out.printf("redMask: %d\n", redMask);
            System.out.printf("greenMask: %d\n", greenMask);
            System.out.printf("blueMask: %d\n", blueMask);
            System.out.printf("alphaMask: %d\n", alphaMask);
            System.out.printf("pvrIdentifier: \"%s\"\n", pvrIdentifier);
            System.out.printf("numSurfaces: %d\n", numSurfaces);
        }

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
            PVRZ.this.textureDataOffset = PVRZ.HEADER_SIZE + metaDataSize;

            if (pixelFormat == 7)
            {
                PVRZ.this.pixelDataDecompressor = this::decompressBC1;
            }

            PVRZ.this.pixelDataBlockSize = PVRZ.this.width * PVRZ.this.height;
            PVRZ.this.sliceBlockSize = PVRZ.this.pixelDataBlockSize * depth;
            PVRZ.this.faceBlockSize = PVRZ.this.sliceBlockSize * numFaces;
            PVRZ.this.surfaceBlockSize = PVRZ.this.faceBlockSize * numSurfaces;
            PVRZ.this.mipMapBlockSize = PVRZ.this.surfaceBlockSize * mipMapCount;

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

            if (mipMapIndex >= PVRZ.this.mipMapCount)
            {
                throw new IllegalArgumentException(String.format(
                    "Attempted to access invalid mipMapIndex: %d", mipMapIndex));
            }

            if (surfaceIndex >= PVRZ.this.numSurfaces)
            {
                throw new IllegalArgumentException(String.format(
                    "Attempted to access invalid surfaceIndex: %d", surfaceIndex));
            }

            if (faceIndex >= PVRZ.this.numFaces)
            {
                throw new IllegalArgumentException(String.format(
                    "Attempted to access invalid faceIndex: %d", faceIndex));
            }

            if (sliceIndex >= PVRZ.this.depth)
            {
                throw new IllegalArgumentException(String.format(
                    "Attempted to access invalid sliceIndex: %d", sliceIndex));
            }

            final int pixelDataOffset =
                PVRZ.this.textureDataOffset
                + mipMapIndex * PVRZ.this.mipMapBlockSize
                + surfaceIndex * PVRZ.this.surfaceBlockSize
                + faceIndex * PVRZ.this.faceBlockSize
                + sliceIndex * PVRZ.this.sliceBlockSize;

            return PVRZ.this.pixelDataDecompressor.apply(pixelDataOffset);
        }

        private IntBuffer decompressBC1(final int pixelDataOffset)
        {
            final int numBlocksHorizontal = PVRZ.this.width / 4;
            if (PVRZ.this.width % 4 != 0)
            {
                throw new IllegalStateException(String.format(
                    "BC1 expects width to be a multiple of 4; got: %d", PVRZ.this.width));
            }

            final int numBlocksVertical = PVRZ.this.height / 4;
            if (PVRZ.this.height % 4 != 0)
            {
                throw new IllegalStateException(String.format(
                    "BC1 expects height to be a multiple of 4; got: %d", PVRZ.this.height));
            }

            final int[] result = new int[pixelDataBlockSize];

            int curTexelBlockOffset = pixelDataOffset;

            for (int texelBlockY = 0; texelBlockY < numBlocksVertical; ++texelBlockY)
            {
                for (int texelBlockX = 0; texelBlockX < numBlocksHorizontal; ++texelBlockX, curTexelBlockOffset += 8)
                {
                    position(curTexelBlockOffset);       final int color0 = unpackBC1Color(buffer.getShort());
                    position(curTexelBlockOffset + 0x2); final int color1 = unpackBC1Color(buffer.getShort());
                    final int color2 = bc1Interpolate(1, 3, color0, color1);
                    final int color3 = bc1Interpolate(2, 3, color0, color1);

                    //final int color2 = bc1Interpolate(1, 2, color0, color1);
                    //final int color3 = 0;

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
            final byte blue = (byte)((packedColor & 0x1F) * 255 / 0x1F);
            final byte green = (byte)(((packedColor >>> 5) & 0x3F) * 255 / 0x3F);
            final byte red = (byte)(((packedColor >>> 11) & 0x1F) * 255 / 0x1F);
            final byte alpha = (byte)255;
            final int result = MiscUtil.packBytesIntoInt(alpha, blue, green, red);

            return result;
        }

        private int bc1Interpolate(
            final int fracColor2Upper, final int fracColorLower, final int color1, final int color2)
        {
            final int fracColor1Upper = fracColorLower - fracColor2Upper;

            final byte r1 = (byte)((color1 & 0xFF) * fracColor1Upper / fracColorLower);
            final byte g1 = (byte)(((color1 >>> 8) & 0xFF) * fracColor1Upper / fracColorLower);
            final byte b1 = (byte)(((color1 >>> 16) & 0xFF) * fracColor1Upper / fracColorLower);
            final byte a1 = (byte)(((color1 >>> 24) & 0xFF) * fracColor1Upper / fracColorLower);

            final byte r2 = (byte)((color2 & 0xF) * fracColor2Upper / fracColorLower);
            final byte g2 = (byte)(((color2 >>> 8) & 0xFF) * fracColor2Upper / fracColorLower);
            final byte b2 = (byte)(((color2 >>> 16) & 0xFF) * fracColor2Upper / fracColorLower);
            final byte a2 = (byte)(((color2 >>> 24) & 0xFF) * fracColor2Upper / fracColorLower);

//            final byte r1 = (byte)(color1 & 0xFF);
//            final byte g1 = (byte)((color1 >>> 8) & 0xFF);
//            final byte b1 = (byte)((color1 >>> 16) & 0xFF);
//            final byte a1 = (byte)((color1 >>> 24) & 0xFF);
//
//            final byte r2 = (byte)(color2 & 0xF);
//            final byte g2 = (byte)((color2 >>> 8) & 0xFF);
//            final byte b2 = (byte)((color2 >>> 16) & 0xFF);
//            final byte a2 = (byte)((color2 >>> 24) & 0xFF);
//
//            final byte rFinal = (byte)((r2 - r1) * fracColor2Upper / fracColorLower + r1);
//            final byte gFinal = (byte)((g2 - g1) * fracColor2Upper / fracColorLower + g1);
//            final byte bFinal = (byte)((b2 - b1) * fracColor2Upper / fracColorLower + b1);
//            //final byte aFinal = (byte)(a1 + a2);
//            final byte aFinal = (byte)255;

//            final byte rFinal = (byte)255;
//            final byte gFinal = (byte)0;
//            final byte bFinal = (byte)255;
//            final byte aFinal = (byte)255;


            final byte rFinal = (byte)(r1 + r2);
            final byte gFinal = (byte)(g1 + g2);
            final byte bFinal = (byte)(b1 + b2);
            final byte aFinal = (byte)(a1 + a2);

            return MiscUtil.packBytesIntoInt(aFinal, bFinal, gFinal, rFinal);
        }
    }
}
