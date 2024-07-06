
package com.github.bubb13.infinityareas.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class BufferUtil
{
    ///////////////////////////
    // Public Static Methods //
    ///////////////////////////

    public static String readUTF8(final ByteBuffer buffer, final int length)
    {
        final String toReturn = readUTF8NoAdvance(buffer, length);
        buffer.position(buffer.position() + length);
        return toReturn;
    }

    public static String readLUTF8(final ByteBuffer buffer, final int length)
    {
        final byte[] array = buffer.array();
        int curOffset = buffer.arrayOffset() + buffer.position();
        int actualLength = length;

        for (int i = 0; i < length; ++i)
        {
            if (array[curOffset++] == 0)
            {
                actualLength = i;
                break;
            }
        }

        final String toReturn = readUTF8NoAdvance(buffer, actualLength);
        buffer.position(buffer.position() + length);
        return toReturn;
    }

    public static void writeLUTF8(final ByteBuffer buffer, final int length, String str)
    {
        if (str.length() > length) str = str.substring(0, length);
        buffer.put(str.getBytes(StandardCharsets.UTF_8));
        for (int i = str.length(); i < length; ++i)
        {
            buffer.put((byte)0);
        }
    }

    public static ByteBuffer readAtOffset(final Path path, final long offset, final int readSize) throws IOException
    {
        try (final FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ))
        {
            final ByteBuffer buffer = ByteBuffer.allocate(readSize);

            fileChannel.position(offset);
            fileChannel.read(buffer);

            buffer.flip();
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            return buffer;
        }
    }

    public static ByteBuffer readAtOffset(
        final Path path, final ByteBuffer buffer,
        final long offset, final int readSize) throws IOException
    {
        buffer.rewind();
        buffer.limit(readSize);

        try (final FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ))
        {
            fileChannel.position(offset);
            fileChannel.read(buffer);

            buffer.flip();
            return buffer;
        }
    }

    public static ByteBuffer readAtOffset(
        final Path path, final ByteBuffer buffer, final int bufferOffset,
        final long fileOffset, final int readSize) throws IOException
    {
        buffer.rewind();
        buffer.position(bufferOffset);
        buffer.limit(bufferOffset + readSize);

        try (final FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ))
        {
            fileChannel.position(fileOffset);
            fileChannel.read(buffer);

            buffer.flip();
            return buffer;
        }
    }

    public static int channelRead(
        final FileChannel channel, final ByteBuffer buffer, final int limit) throws Exception
    {
        buffer.rewind();
        buffer.limit(limit);
        final int numRead = channel.read(buffer);
        buffer.flip();
        return numRead;
    }

    public static String toHexString(final ByteBuffer buffer)
    {
        final byte[] array = buffer.array();

        return IntStream.range(buffer.arrayOffset(), array.length)
            .mapToObj(i -> String.format("%02X", array[i]))
            .collect(Collectors.joining(" "));
    }

    ////////////////////////////
    // Private Static Methods //
    ////////////////////////////

    private static String readUTF8NoAdvance(final ByteBuffer buffer, final int length)
    {
        return new String(buffer.array(), buffer.arrayOffset() + buffer.position(),
            length, StandardCharsets.UTF_8);
    }

    //////////////////////////
    // Private Constructors //
    //////////////////////////

    private BufferUtil() {}
}
