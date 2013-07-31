import java.io.IOException;
import java.io.InputStream;

public class ByteReader {
    private final InputStream inputStream;
    private final int preferredReadSize;

    private byte[] block;
    private int blockLength;
    private int blockOffset;

    private byte[] prefetch;
    private int prefetchLength;

    private boolean invalid;

    public ByteReader(InputStream inputStream, int preferredReadSize) {
        this.inputStream = inputStream;
        this.preferredReadSize = preferredReadSize;
    }

    public int readUntil(byte[] delimiter, byte[] buffer) throws IOException {
        if (invalid) {
            throw new IllegalStateException("Cannot recover from earlier exception while reading");
        }
        if (block == null) {
            block = new byte[preferredReadSize];
            blockLength = inputStream.read(block);
            blockOffset = 0;

            prefetch = new byte[preferredReadSize];
            prefetchLength = inputStream.read(prefetch);
            if (prefetchLength < 0) {
                prefetchLength = 0;
            }
        }
        int bytesRead = 0;
        while (blockLength > 0) {
            while (blockOffset < blockLength) {
                if (matches(delimiter, blockOffset)) {
                    blockOffset += delimiter.length;
                    return bytesRead;
                }
                if (bytesRead >= buffer.length) {
                    invalid = true;
                    throw new IllegalArgumentException("Buffer is not big enough");
                }
                buffer[bytesRead] = block[blockOffset];
                bytesRead++;
                blockOffset++;
            }
            blockLength = prefetchLength;
            byte[] temp = block;
            block = prefetch;
            prefetch = temp;
            prefetchLength = inputStream.read(prefetch);
            if (prefetchLength < 0) {
                prefetchLength = 0;
            }
            if (blockLength == 0) {
                return bytesRead;
            }
            blockOffset = 0;
        }
        return -1;
    }

    private boolean matches(byte[] delimiter, int offset) {
        for (int i = 0; i < delimiter.length; i++) {
            byte b = offset + i < blockLength ? block[offset + i] : prefetch[offset + i - blockLength];
            if (b != delimiter[i]) {
                return false;
            }
        }
        return true;
    }
}
