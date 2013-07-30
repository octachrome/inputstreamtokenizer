import java.io.IOException;
import java.io.InputStream;

public class ByteReader {
    private InputStream inputStream;
    private final int blockSize;

    public ByteReader(InputStream inputStream) {
        this.inputStream = inputStream;
        this.blockSize = 1024;
    }

    public int readUntil(byte[] delimiter, byte[] buffer) throws IOException {
        byte[] block = new byte[blockSize];
        int count = inputStream.read(block);
        for (int offset = 0; offset < count - delimiter.length; offset++) {
            if (matches(block, delimiter, offset)) {
                System.arraycopy(block, 0, buffer, 0, offset);
                return offset;
            }
        }
        return 0;
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
