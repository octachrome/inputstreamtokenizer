package net.thebrown.io;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamTokenizer {
    private final InputStream inputStream;
    private final int preferredReadSize;

    private byte[] block;
    private int blockLength;
    private int blockOffset;

    private byte[] prefetchBlock;
    private int prefetchLength;

    private boolean invalid;

    public InputStreamTokenizer(InputStream inputStream, int preferredReadSize) {
        this.inputStream = inputStream;
        this.preferredReadSize = preferredReadSize;
    }

    public int readUntil(byte[] delimiter, byte[] buffer) throws IOException {
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
