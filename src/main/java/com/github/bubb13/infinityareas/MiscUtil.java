
package com.github.bubb13.infinityareas;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MiscUtil
{
    public static Path findInfinityAreasRoot() throws URISyntaxException
    {
        final Path codePath = Paths.get(MiscUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI());

        if (codePath.toString().endsWith(".jar"))
        {
            // .jar file parent directory
            return codePath.getParent();
        }
        else
        {
            // Working directory
            return Paths.get("").toAbsolutePath();
        }
    }

    public static <T> Iterable<T> readOnlyIterable(final Iterator<? extends T> iterator)
    {
        return new IteratorToIterable<>(new ReadOnlyIterator<>(iterator));
    }

    public static <T> Iterable<T> readOnlyIterable(final Iterable<? extends T> iterable)
    {
        return readOnlyIterable(iterable.iterator());
    }

    public static String formatStackTrace(final Throwable e)
    {
        final StringBuilder builder = new StringBuilder(e.toString()).append("\n\n");
        final String stackTrace = Arrays.toString(e.getStackTrace());
        builder.append(stackTrace, 1, stackTrace.length() - 1);
        return builder.toString();
    }

    public static int packBytesIntoInt(final byte b3, final byte b2, final byte b1, final byte b0)
    {
        return ((int) b3 & 0xFF) << 24 | ((int) b2 & 0xFF) << 16 | ((int) b1 & 0xFF) << 8 | ((int) b0 & 0xFF);
    }

    public static short toUnsignedByte(final byte b)
    {
        return (short)(b & 0xFF);
    }

    private MiscUtil() {}
}
