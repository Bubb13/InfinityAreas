
package com.github.bubb13.infinityareas;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class BufferUtil
{
    public static String readUTF8(final ByteBuffer buffer, final int length)
    {
        return new String(buffer.array(),buffer.arrayOffset() + buffer.position(),
            length, StandardCharsets.UTF_8);
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

        return readUTF8(buffer, actualLength);
    }

    public static ByteBuffer readAtOffset(final Path path, final long offset, final int readSize) throws IOException
    {
        try (final FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ))
        {
            final ByteBuffer buffer = ByteBuffer.allocate(readSize);

            fileChannel.position(offset);
            fileChannel.read(buffer);

            buffer.flip();
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

    public static String toHexString(final ByteBuffer buffer)
    {
        final byte[] array = buffer.array();

        return IntStream.range(buffer.arrayOffset(), array.length)
            .mapToObj(i -> String.format("%02X", array[i]))
            .collect(Collectors.joining(" "));
    }

    private BufferUtil() {}
}
