import java.io.IOException;
import java.io.InputStream;

public class ByteReader {
    private final InputStream inputStream;
    private final int blockSize;

    private int globalOffset;
    private byte[] block;
    private int count;
    private int offset;

    public ByteReader(InputStream inputStream, int blockSize) {
        this.inputStream = inputStream;
        this.blockSize = blockSize;
    }

    public int readUntil(byte[] delimiter, byte[] buffer) throws IOException {
        if (block == null) {
            block = new byte[blockSize];
            count = inputStream.read(block);
            globalOffset = 0;
            offset = 0;
        }
        int copyFrom = offset;
        int bytesRead = 0;
        int bufferOffset = 0;
        while (count >= 0) {
            for (; offset < count - delimiter.length + 1; offset++) {
                if (matches(block, delimiter, offset)) {
                    System.arraycopy(block, copyFrom, buffer, bufferOffset, offset);
                    offset += delimiter.length;
                    return bytesRead;
                }
                bytesRead++;
            }
            System.arraycopy(block, 0, buffer, bufferOffset, count);
            globalOffset += count;
            bufferOffset += count;
            count = inputStream.read(block);
            offset = 0;
            copyFrom = 0;
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
