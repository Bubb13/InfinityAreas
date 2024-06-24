
package com.github.bubb13.infinityareas.misc;

import java.io.IOException;
import java.io.InputStream;

public class LimitableInputStream extends InputStream
{
    private final InputStream inputStream;
    private long limit;
    private long totalBytesRead;
    private boolean limitEnabled = false;

    public LimitableInputStream(final InputStream inputStream, final long initialLimit)
    {
        this.inputStream = inputStream;
        limit(initialLimit);
    }

    public LimitableInputStream(final InputStream inputStream)
    {
        this.inputStream = inputStream;
    }

    public void limit(final long newLimit)
    {
        limit = newLimit;
        totalBytesRead = 0;
        limitEnabled = true;
    }

    public void disableLimit()
    {
        limitEnabled = false;
    }

    @Override
    public int read() throws IOException
    {
        if (!limitEnabled)
        {
            return inputStream.read();
        }

        if (totalBytesRead >= limit)
        {
            return -1; // end of stream
        }

        final int byteRead = inputStream.read();
        if (byteRead != -1)
        {
            ++totalBytesRead;
        }

        return byteRead;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException
    {
        if (!limitEnabled)
        {
            return inputStream.read(b, off, len);
        }

        if (totalBytesRead >= limit)
        {
            return -1; // end of stream
        }

        final int numBytesToRead = (int)Math.min(limit - totalBytesRead, len);
        final int numBytesRead = inputStream.read(b, off, numBytesToRead);

        if (numBytesRead > 0)
        {
            totalBytesRead += numBytesRead;
        }

        return numBytesRead;
    }

    @Override
    public void close() throws IOException
    {
        inputStream.close();
    }
}
