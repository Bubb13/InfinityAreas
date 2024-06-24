
package com.github.bubb13.infinityareas.game.resource;

import com.github.bubb13.infinityareas.util.BufferUtil;
import com.github.bubb13.infinityareas.util.FileUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class BifFileBackup
{
    ///////////////////////////
    // Private Static Fields //
    ///////////////////////////

    // The size of a BIF file entry
    private static final int FILE_ENTRY_SIZE = 0x10;
    // The number of file entries to read at once
    private static final int FILE_ENTRY_READ_GROUP = 512;
    // The resulting read size. A `FILE_ENTRY_READ_GROUP` of 512 results in reads of 8192 bytes.
    private static final int FILE_ENTRY_READ_CHUNK = FILE_ENTRY_SIZE * FILE_ENTRY_READ_GROUP;

    private static final int TILESET_ENTRY_SIZE = 0x14;
    private static final int TILESET_ENTRY_READ_GROUP = 512;
    private static final int TILESET_ENTRY_READ_CHUNK = TILESET_ENTRY_SIZE * TILESET_ENTRY_READ_GROUP;

    ////////////////////
    // Private Fields //
    ////////////////////

    private final String relativePathStr;
    private final Path path;
    private final HashMap<Short, FileEntry> fileEntriesByResourceIndex = new HashMap<>();
    private final HashMap<Byte, TilesetEntry> tilesetEntriesByTilesetIndex = new HashMap<>();
    private final ByteBuffer entryBuffer = ByteBuffer.allocate(
        Math.max(FILE_ENTRY_READ_CHUNK, TILESET_ENTRY_READ_CHUNK));
    {
        entryBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public BifFileBackup(final String rootName, final Path root, final Path path) throws IOException
    {
        this.path = path.toAbsolutePath();
        relativePathStr = rootName + File.separator + FileUtil.getRelativePath(root, path);

        if (!Files.isRegularFile(path))
        {
            throw new FileNotFoundException("Bif file does not exist: \"" + path + "\"");
        }

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

    public ByteBuffer demandResourceData(final short resourceIndex) throws IOException
    {
        final FileEntry fileEntry = fileEntriesByResourceIndex.get(resourceIndex);
        if (fileEntry == null)
        {
            throw new IllegalStateException(path.toString() + " does not contain resource index " + resourceIndex);
        }

        return BufferUtil.readAtOffset(path, fileEntry.dataOffset(), fileEntry.dataSize());
    }

    public ByteBuffer demandResourceData(final KeyFile.ResourceLocator resourceLocator) throws IOException
    {
        return demandResourceData(resourceLocator.getResourceIndex());
    }

    public ByteBuffer demandTilesetData(final byte tilesetIndex) throws IOException
    {
        final TilesetEntry tilesetEntry = tilesetEntriesByTilesetIndex.get(tilesetIndex);
        if (tilesetEntry == null)
        {
            throw new IllegalStateException(path.toString() + " does not contain tileset index " + tilesetIndex);
        }

        final ByteBuffer buffer = ByteBuffer.allocate(TIS.HEADER_SIZE + tilesetEntry.dataSize());
        BufferUtil.readAtOffset(path, buffer, TIS.HEADER_SIZE, tilesetEntry.dataOffset(), tilesetEntry.dataSize());

        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(0x0); buffer.put("TIS ".getBytes(StandardCharsets.UTF_8));
        buffer.position(0x4); buffer.put("V1  ".getBytes(StandardCharsets.UTF_8));
        buffer.position(0x8); buffer.putInt(tilesetEntry.numTiles());
        buffer.position(0xC); buffer.putInt(tilesetEntry.tileSize());
        buffer.position(0x10); buffer.putInt(TIS.HEADER_SIZE);
        buffer.position(0x14); buffer.putInt(64);

        buffer.position(0);
        return buffer;
    }

    public ByteBuffer demandTilesetData(final KeyFile.ResourceLocator resourceLocator) throws IOException
    {
        return demandTilesetData(resourceLocator.getTilesetIndex());
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void parse() throws IOException
    {
        final ByteBuffer headerBuffer = BufferUtil.readAtOffset(path, 0, 0x14);
        headerBuffer.order(ByteOrder.LITTLE_ENDIAN);

        headerBuffer.position(0x0); final String signature = BufferUtil.readUTF8(headerBuffer, 4);
        if (signature.equals("BIFC"))
        {
            // TODO: Handle BIFC
            throw new UnsupportedOperationException("BIFC file support currently unimplemented");
        }
        else if (!signature.equals("BIFF"))
        {
            throw new IllegalStateException("Invalid bif signature: \"" + signature + "\"");
        }

        headerBuffer.position(0x4); final String version = BufferUtil.readUTF8(headerBuffer, 4);
        if (!version.equals("V1  "))
        {
            throw new IllegalStateException("Invalid bif version: \"" + version + "\"");
        }

        headerBuffer.position(0x8);  final int numFileEntries = headerBuffer.getInt();
        headerBuffer.position(0xC);  final int numTilesetEntries = headerBuffer.getInt();
        headerBuffer.position(0x10); final int fileEntriesOffset = headerBuffer.getInt();

        parseFileEntries(fileEntriesOffset, numFileEntries);

        final int tilesetEntriesOffset = fileEntriesOffset + FILE_ENTRY_SIZE * numFileEntries;
        parseTilesetEntries(tilesetEntriesOffset, numTilesetEntries);
    }

    private ByteBuffer decompress() throws Exception
    {
        // TODO: Handle BIFC

//        try (
//            final ByteArrayInputStream inputStream = new ByteArrayInputStream(
//                buffer.array(), 4, buffer.limit() - 4
//            );
//            final InflaterInputStream inflaterInputStream = new InflaterInputStream(inputStream, new Inflater()))
//        {
//            final ByteBuffer decompressedBuffer = ByteBuffer.wrap(inflaterInputStream.readAllBytes());
//            decompressedBuffer.order(ByteOrder.LITTLE_ENDIAN);
//            return decompressedBuffer;
//        }

        return null;
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
