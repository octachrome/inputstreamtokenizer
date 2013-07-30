import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.*;

public class ByteReaderTest {
    @Test
    public void test_stuff() throws IOException {
        InputStream is = new ByteArrayInputStream("testxabc".getBytes());
        ByteReader reader = new ByteReader(is);
        byte[] buffer = new byte[20];
        int count = reader.readUntil("x".getBytes(), buffer);
        assertThat("expected to read 4 bytes", count, is(4));
        assertThat("expected buffer to contain 'test'", buffer, startsWith("test".getBytes()));
    }

    public static Matcher<byte[]> startsWith(final byte[] prefix) {
        return new BaseMatcher<byte[]>() {
            @Override
            public boolean matches(Object o) {
                byte[] bytes = (byte[]) o;
                for (int i = 0; i < prefix.length; i++) {
                    if (i >= bytes.length) {
                        return false;
                    }
                    if (bytes[i] != prefix[i]) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("has prefix " + toHexString(prefix));
            }

            private String toHexString(byte[] bytes) {
                StringBuilder stringBuilder = new StringBuilder();
                for (byte b : bytes) {
                    stringBuilder.append(String.format("%02x", b));
                }
                return stringBuilder.toString();
            }
        };
    }
}
