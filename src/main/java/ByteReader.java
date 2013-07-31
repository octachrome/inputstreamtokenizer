import java.io.IOException;
import java.io.InputStream;

public class ByteReader {
    private final InputStream inputStream;
    private final int preferredReadSize;

    private byte[] block;
    private int blockLength;
    private int blockOffset;

    public ByteReader(InputStream inputStream, int preferredReadSize) {
        this.inputStream = inputStream;
        this.preferredReadSize = preferredReadSize;
    }

    public int readUntil(byte[] delimiter, byte[] buffer) throws IOException {
        if (block == null) {
            block = new byte[preferredReadSize];
            blockLength = inputStream.read(block);
            blockOffset = 0;
        }
        int bytesRead = 0;
        while (blockLength >= 0) {
            while (blockOffset < blockLength - delimiter.length + 1) {
                if (matches(block, delimiter, blockOffset)) {
                    blockOffset += delimiter.length;
                    return bytesRead;
                }
                buffer[bytesRead] = block[blockOffset];
                bytesRead++;
                blockOffset++;
            }
            blockLength = inputStream.read(block);
            blockOffset = 0;
        }
        return -1;
    }

    private boolean matches(byte[] block, byte[] delimiter, int offset) {
        for (int i = 0; i < delimiter.length; i++) {
            if (block[offset + i] != delimiter[i]) {
                return false;
            }
        }
        return true;
    }
}
