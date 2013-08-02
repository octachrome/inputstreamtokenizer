package net.thebrown.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * Efficiently scans an input stream for a series of delimiters, extracting the bytes in between. It uses two buffers:
 * one which is actively being scanned, and another which allows the search to look ahead into the next block, in case
 * the delimiter spans the boundary between two blocks.
 */
public class InputStreamTokenizer {
    private static final int DEFAULT_READ_SIZE = 1024;

    private final InputStream inputStream;
    private final int preferredReadSize;

    /**
     * The 'active' buffer which is being searched for the delimiter.
     */
    private byte[] block;
    private int blockLength;
    private int blockOffset;

    /**
     * The next buffer, used to search ahead, in case the delimiter spans two blocks.
     */
    private byte[] prefetchBlock;
    private int prefetchLength;

    /**
     * If true, we have encountered an unrecoverable error and we cannot continue to tokenize the stream.
     */
    private boolean invalid;

    /**
     * Construct an InputStreamTokenizer which reads from the given InputStream.
     * @param inputStream an InputStream
     */
    public InputStreamTokenizer(InputStream inputStream) {
        this(inputStream, DEFAULT_READ_SIZE);
    }

    /**
     * Construct an InputStreamTokenizer which reads from the given InputStream using the given block size.
     * @param inputStream an InputStream
     * @param preferredReadSize the number of bytes to read from the stream at one time
     */
    public InputStreamTokenizer(InputStream inputStream, int preferredReadSize) {
        this.inputStream = inputStream;
        this.preferredReadSize = preferredReadSize;
    }

    /**
     * Read the next block of data, up to the delimiter. The given buffer is populated with the block, unless null. The
     * delimiter is not written to the buffer.
     * @param delimiter    the string of bytes which should terminate the block to be read
     * @param buffer       an optional buffer to populate with the read bytes (can be null)
     * @return the number of bytes which were read (not including the delimiter)
     * @throws IOException if the underlying stream raises an error
     */
    public int readNext(byte[] delimiter, byte[] buffer) throws IOException {
        if (invalid) {
            throw new IllegalStateException("Cannot recover from earlier exception while reading");
        }
        if (block == null) {
            block = new byte[preferredReadSize];
            prefetchBlock = new byte[preferredReadSize];
            // load the prefetch block
            nextBlock();
            // make the prefetch block the active block
            nextBlock();
        }
        int bytesRead = 0;
        while (blockLength > 0) {
            while (blockOffset < blockLength) {
                if (matches(delimiter, blockOffset)) {
                    // advance past the delimiter
                    blockOffset += delimiter.length;
                    if (blockOffset > blockLength) {
                        int offset = blockOffset - blockLength;
                        nextBlock();
                        blockOffset = offset;
                    }
                    return bytesRead;
                }
                if (buffer != null) {
                    if (bytesRead >= buffer.length) {
                        invalid = true;
                        throw new IllegalArgumentException("Buffer is not big enough");
                    }
                    buffer[bytesRead] = block[blockOffset];
                }
                bytesRead++;
                blockOffset++;
            }
            nextBlock();
            if (blockLength == 0) {
                return bytesRead;
            }
        }
        return -1;
    }

    /**
     * Assign {@link #block} to the next block from the input stream. The method really moves the prefetch buffer into
     * the active buffer and then reads from the InputStream into the prefetch buffer.
     * @throws IOException if the underlying stream raises an error
     */
    private void nextBlock() throws IOException {
        byte[] temp = block;
        block = prefetchBlock;
        prefetchBlock = temp;

        blockLength = prefetchLength;
        prefetchLength = inputStream.read(prefetchBlock);
        if (prefetchLength < 0) {
            prefetchLength = 0;
        }

        blockOffset = 0;
    }

    /**
     * Check whether the delimiter matches the current block at the given offset.
     * @param delimiter    the delimiter to look for
     * @param offset       the starting offset within the current block
     * @return true if the delimiter was found
     */
    private boolean matches(byte[] delimiter, int offset) {
        for (int i = 0; i < delimiter.length; i++) {
            byte b;
            if (offset + i < blockLength) {
                b = block[offset + i];
            } else if (offset + i - blockLength < prefetchLength) {
                b = prefetchBlock[offset + i - blockLength];
            } else {
                // buffers exhausted
                return false;
            }
            if (b != delimiter[i]) {
                return false;
            }
        }
        return true;
    }
}
