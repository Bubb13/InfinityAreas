
package com.github.bubb13.infinityareas.game.resource;

import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.misc.LimitableInputStream;
import com.github.bubb13.infinityareas.misc.ThrowingRunnable;
import com.github.bubb13.infinityareas.util.BufferUtil;
import com.github.bubb13.infinityareas.util.FileUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class BifFile
{
    ///////////////////////////
    // Private Static Fields //
    ///////////////////////////

    /** The size of a BIF file entry */
    private static final int FILE_ENTRY_SIZE = 0x10;
    /** The number of file entries to read at once */
    private static final int FILE_ENTRY_READ_GROUP = 512;
    /** The resulting read size. A `FILE_ENTRY_READ_GROUP` of 512 results in reads of 8192 bytes. */
    private static final int FILE_ENTRY_READ_CHUNK = FILE_ENTRY_SIZE * FILE_ENTRY_READ_GROUP;

    /** The size of a TIS entry */
    private static final int TILESET_ENTRY_SIZE = 0x14;
    /** The number of TIS entries to read at once */
    private static final int TILESET_ENTRY_READ_GROUP = 512;
    /** The resulting read size. A `TILESET_ENTRY_READ_GROUP` of 512 results in reads of 10240 bytes. */
    private static final int TILESET_ENTRY_READ_CHUNK = TILESET_ENTRY_SIZE * TILESET_ENTRY_READ_GROUP;

    /** The buffer size used to store BIFC metadata / decompressed data. Must be at least 0x8 bytes. */
    private static final int BIFC_BUFFER_SIZE = 0x2000;

    ////////////////////
    // Private Fields //
    ////////////////////

    private final Path originalPath;
    private final String relativePathStr;

    private final HashMap<Short, FileEntry> fileEntriesByResourceIndex = new HashMap<>();
    private final HashMap<Byte, TilesetEntry> tilesetEntriesByTilesetIndex = new HashMap<>();
    private final ByteBuffer entryBuffer = ByteBuffer.allocate(
        Math.max(FILE_ENTRY_READ_CHUNK, TILESET_ENTRY_READ_CHUNK));
    {
        entryBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    private Path path;
    private ByteBuffer signatureAndVersionBuffer;
    private ThrowingRunnable<Exception> pendingDecompression;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public BifFile(final String rootName, final Path root, final Path path) throws Exception
    {
        if (!Files.isRegularFile(path))
        {
            throw new FileNotFoundException("Bif file does not exist: \"" + path + "\"");
        }

        originalPath = path.toAbsolutePath();
        relativePathStr = rootName + File.separator + FileUtil.getRelativePath(root, path);

        this.path = originalPath;
        parse();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public Path getPath()
    {
        return path;
    }

    public String getRelativePathStr()
    {
        return relativePathStr;
    }

    private void checkDecompression() throws Exception
    {
        if (pendingDecompression != null)
        {
            pendingDecompression.run();
            pendingDecompression = null;
        }
    }

    public ByteBuffer demandResourceData(final short resourceIndex) throws Exception
    {
        checkDecompression();

        final FileEntry fileEntry = fileEntriesByResourceIndex.get(resourceIndex);
        if (fileEntry == null)
        {
            throw new IllegalStateException(path.toString() + " does not contain resource index " + resourceIndex);
        }

        final ByteBuffer buffer = BufferUtil.readAtOffset(path, fileEntry.dataOffset(), fileEntry.dataSize());
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer;
    }

    public ByteBuffer demandResourceData(final KeyFile.ResourceLocator resourceLocator) throws Exception
    {
        return demandResourceData(resourceLocator.getResourceIndex());
    }

    public ByteBuffer demandTilesetData(final byte tilesetIndex) throws Exception
    {
        checkDecompression();

        final TilesetEntry tilesetEntry = tilesetEntriesByTilesetIndex.get(tilesetIndex);
        if (tilesetEntry == null)
        {
            throw new IllegalStateException(path.toString() + " does not contain tileset index " + tilesetIndex);
        }

        final ByteBuffer buffer = ByteBuffer.allocate(TIS.HEADER_SIZE + tilesetEntry.dataSize());
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Read in-bif tileset data (missing header) to the correct offset
        BufferUtil.readAtOffset(path, buffer, TIS.HEADER_SIZE, tilesetEntry.dataOffset(), tilesetEntry.dataSize());

        // Write a fake header as if this TIS was a loose file
        buffer.position(0x0); buffer.put("TIS ".getBytes(StandardCharsets.UTF_8));
        buffer.position(0x4); buffer.put("V1  ".getBytes(StandardCharsets.UTF_8));
        buffer.position(0x8); buffer.putInt(tilesetEntry.numTiles());
        buffer.position(0xC); buffer.putInt(tilesetEntry.tileSize());
        buffer.position(0x10); buffer.putInt(TIS.HEADER_SIZE);
        buffer.position(0x14); buffer.putInt(64);

        buffer.position(0);
        return buffer;
    }

    public ByteBuffer demandTilesetData(final KeyFile.ResourceLocator resourceLocator) throws Exception
    {
        return demandTilesetData(resourceLocator.getTilesetIndex());
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void readSignatureAndVersion() throws Exception
    {
        signatureAndVersionBuffer = BufferUtil.readAtOffset(path, 0, 8);
    }

    private void parse() throws Exception
    {
        readSignatureAndVersion();
        final String signature = BufferUtil.readUTF8(signatureAndVersionBuffer, 4);

        switch (signature)
        {
            case "BIFF" -> parseUncompressedBIFF(signature);
            case "BIF " -> parseCompressedBIF(signature);
            case "BIFC" -> parseCompressedBIFC(signature);
            default -> throw new IllegalStateException(String.format("Invalid bif signature: \"%s\"", signature));
        }
    }

    private void parseUncompressedBIFF(final String signature) throws Exception
    {
        verifyVersion(signature, "V1  ");

        final ByteBuffer headerBuffer = BufferUtil.readAtOffset(path, 0x8, 0xC);

        final int numFileEntries = headerBuffer.getInt();
        final int numTilesetEntries = headerBuffer.getInt();
        final int fileEntriesOffset = headerBuffer.getInt();

        parseFileEntries(fileEntriesOffset, numFileEntries);

        final int tilesetEntriesOffset = fileEntriesOffset + FILE_ENTRY_SIZE * numFileEntries;
        parseTilesetEntries(tilesetEntriesOffset, numTilesetEntries);
    }

    private void parseCompressedBIF(final String signature) throws Exception
    {
        verifyVersion(signature, "V1.0");

        final int fileNameLen = BufferUtil.readAtOffset(path, 0x8, 4).getInt();
        //final String fileName = BufferUtil.readUTF8(
        //    BufferUtil.readAtOffset(path, 0xC, fileNameLen),
        //    fileNameLen - 1);

        final int afterFileNameOffset = 0xC + fileNameLen;
        //final ByteBuffer buffer = BufferUtil.readAtOffset(path, afterFileNameOffset, 0x8);
        //final int uncompressedDataLength = buffer.getInt();
        //final int compressedDataLength = buffer.getInt();

        //System.out.printf("fileName: \"%s\", uncompressedDataLength: 0x%X, compressedDataLength: 0x%X\n",
        //    fileName, uncompressedDataLength, compressedDataLength);

        pendingDecompression = () ->
        {
            path = decompress(afterFileNameOffset + 0x8);
            parse();
        };
    }

    private Path decompress(final int fromOffset) throws Exception
    {
        final Path tempFolderPath = GlobalState.getInfinityAreasTemp();
        final Path tempFilePath = Files.createTempFile(tempFolderPath, null, null);

        try (
            final InflaterInputStream inputStream = new InflaterInputStream(
                inputStreamFromPathOffset(path, fromOffset),
                new Inflater());
            final BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(tempFilePath)))
        {
            byte[] buffer = new byte[1024];

            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1)
            {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        return tempFilePath;
    }

    private static InputStream inputStreamFromPathOffset(
        final Path path, final long fromOffset) throws IOException
    {
        return Channels.newInputStream(FileChannel.open(path, StandardOpenOption.READ).position(fromOffset));
    }

    private void parseCompressedBIFC(final String signature) throws Exception
    {
        verifyVersion(signature, "V1.0");

        pendingDecompression = () ->
        {
            path = decompressBIFC();
            parse();
        };
    }

    private Path decompressBIFC() throws Exception
    {
        final Path outputPath = Files.createTempFile(GlobalState.getInfinityAreasTemp(), null, null);
        final ByteBuffer buffer = ByteBuffer.allocate(BIFC_BUFFER_SIZE);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        final byte[] bufferArray = buffer.array();

        try (
            final FileChannel inputChannel = FileChannel.open(path, StandardOpenOption.READ);
            final LimitableInputStream limitableInputStream = new LimitableInputStream(
                Channels.newInputStream(inputChannel)
            );
            final FileChannel outputChannel = FileChannel.open(outputPath, StandardOpenOption.WRITE);
        )
        {
            // Read BIFC metadata
            inputChannel.position(0x8);
            BufferUtil.channelRead(inputChannel, buffer, 4);
            final int uncompressedSize = buffer.getInt();

            final Inflater inflater = new Inflater();
            int totalBytesDecompressed = 0;

            // Decompress blocks until the noted target size is reached
            while (totalBytesDecompressed < uncompressedSize)
            {
                // Read the current block's metadata
                BufferUtil.channelRead(inputChannel, buffer, 8);
                final int blockDecompressedSize = buffer.getInt();
                final int blockCompressedSize = buffer.getInt();

                // Limit the source input stream so that the inflater doesn't read past the compressed data
                limitableInputStream.limit(blockCompressedSize);
                final InflaterInputStream inflaterInputStream = new InflaterInputStream(limitableInputStream, inflater);

                int bytesDecompressedInBlock = 0;
                int bytesRead;

                // Read the decompressed data from the inflater input stream
                while ((bytesRead = inflaterInputStream.read(bufferArray, 0, BIFC_BUFFER_SIZE)) != -1)
                {
                    // Reset the buffer to only see what was read into bufferArray
                    buffer.rewind();
                    buffer.limit(bytesRead);

                    // Write to the output channel (the temp file)
                    int bytesWritten = 0;
                    while (bytesWritten < bytesRead)
                    {
                        bytesWritten += outputChannel.write(buffer);
                    }

                    // Track the number of bytes decompressed from this block
                    bytesDecompressedInBlock += bytesRead;
                }

                // Check that the block decompression resulted in the expected number of bytes
                if (bytesDecompressedInBlock != blockDecompressedSize)
                {
                    throw new IllegalStateException(String.format(
                        "Expected BIFC block to decompress to 0x%X bytes, got 0x%X",
                        blockDecompressedSize, bytesDecompressedInBlock));
                }

                // Track the total number of bytes decompressed so far
                totalBytesDecompressed += bytesDecompressedInBlock;

                // Reset the inflater, so it can be reused for subsequent blocks
                inflater.reset();
            }
        }

        return outputPath;
    }

    private void verifyVersion(final String signature, final String versionToMatch)
    {
        final String version = BufferUtil.readUTF8(signatureAndVersionBuffer, 4);
        if (!version.equals(versionToMatch))
        {
            throw new IllegalStateException(String.format(
                "Invalid bif signature (\"%s\") + version (\"%s\") combination", signature, versionToMatch));
        }
    }

    private void parseFileEntries(final int offset, final int count) throws IOException
    {
        final int numFullChunks = count / FILE_ENTRY_READ_GROUP;
        int curFileBase = offset;

        for (int i = 0; i < numFullChunks; ++i)
        {
            parseFileEntryChunk(curFileBase, FILE_ENTRY_READ_CHUNK, FILE_ENTRY_READ_GROUP);
            curFileBase += FILE_ENTRY_READ_CHUNK;
        }

        final int numRemainingEntries = count % FILE_ENTRY_READ_GROUP;
        final int remainingChunkSize = FILE_ENTRY_SIZE * numRemainingEntries;
        parseFileEntryChunk(curFileBase, remainingChunkSize, numRemainingEntries);
    }

    private void parseFileEntryChunk(
        final int fileOffset, final int fileReadSize, final int entryCount) throws IOException
    {
        BufferUtil.readAtOffset(path, entryBuffer, fileOffset, fileReadSize);
        int curBufferBase = 0;

        for (int i = 0; i < entryCount; ++i)
        {
            position(curBufferBase);       final int resourceLocatorInt = entryBuffer.getInt();
            position(curBufferBase + 0x4); final int dataOffset = entryBuffer.getInt();
            position(curBufferBase + 0x8); final int dataSize = entryBuffer.getInt();
            position(curBufferBase + 0xC); final int resourceType = entryBuffer.getShort();

            final KeyFile.ResourceLocator resourceLocator = new KeyFile.ResourceLocator(resourceLocatorInt);
            final FileEntry fileEntry = new FileEntry(resourceLocatorInt, dataOffset, dataSize, resourceType);
            fileEntriesByResourceIndex.put(resourceLocator.getResourceIndex(), fileEntry);

            curBufferBase += FILE_ENTRY_SIZE; // Next entry
        }
    }

    private void parseTilesetEntries(final int offset, final int count) throws IOException
    {
        final int numFullChunks = count / TILESET_ENTRY_READ_GROUP;
        int curFileBase = offset;

        for (int i = 0; i < numFullChunks; ++i)
        {
            parseFileEntryChunk(curFileBase, TILESET_ENTRY_READ_CHUNK, TILESET_ENTRY_READ_GROUP);
            curFileBase += TILESET_ENTRY_READ_CHUNK;
        }

        final int numRemainingEntries = count % TILESET_ENTRY_READ_GROUP;
        final int remainingChunkSize = TILESET_ENTRY_SIZE * numRemainingEntries;
        parseTilesetEntryChunk(curFileBase, remainingChunkSize, numRemainingEntries);
    }

    private void parseTilesetEntryChunk(
        final int fileOffset, final int fileReadSize, final int entryCount) throws IOException
    {
        BufferUtil.readAtOffset(path, entryBuffer, fileOffset, fileReadSize);
        int curBufferBase = 0;

        for (int i = 0; i < entryCount; ++i)
        {
            position(curBufferBase);        final int resourceLocatorInt = entryBuffer.getInt();
            position(curBufferBase + 0x4);  final int dataOffset = entryBuffer.getInt();
            position(curBufferBase + 0x8);  final int numTiles = entryBuffer.getInt();
            position(curBufferBase + 0xC);  final int tileSize = entryBuffer.getInt();
            position(curBufferBase + 0x10); final short resourceType = entryBuffer.getShort();
            //position(curBufferBase + 0x12); final short unknown = entryBuffer.getShort();

            final KeyFile.ResourceLocator resourceLocator = new KeyFile.ResourceLocator(resourceLocatorInt);
            final TilesetEntry fileEntry = new TilesetEntry(
                resourceLocatorInt, dataOffset, numTiles, tileSize, resourceType
            );
            tilesetEntriesByTilesetIndex.put(resourceLocator.getTilesetIndex(), fileEntry);

            curBufferBase += TILESET_ENTRY_SIZE; // Next entry
        }
    }

    private void position(final int pos)
    {
        entryBuffer.position(pos);
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private record FileEntry(int resourceLocator, int dataOffset, int dataSize, int resourceType) {}

    private record TilesetEntry(
        int resourceLocator, int dataOffset, int numTiles, int tileSize, short resourceType, int dataSize)
    {
        public TilesetEntry(int resourceLocator, int dataOffset, int numTiles, int tileSize, short resourceType)
        {
            this(resourceLocator, dataOffset, numTiles, tileSize, resourceType, numTiles * tileSize);
        }
    }
}
