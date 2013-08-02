package net.thebrown.io;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.fail;

public class InputStreamTokenizerTest {
    @Test
    public void should_find_sequence_mid_block() throws IOException {
        InputStream is = new ByteArrayInputStream("testxabc".getBytes());
        InputStreamTokenizer reader = new InputStreamTokenizer(is, 1024);
        byte[] buffer = new byte[20];
        int count = reader.readUntil("x".getBytes(), buffer);
        assertThat("expected to read 4 bytes", count, is(4));
        assertThat("expected buffer to contain 'test'", buffer, startsWith("test".getBytes()));
    }

    @Test
    public void should_find_sequence_at_block_end() throws IOException {
        InputStream is = new ByteArrayInputStream("tesxtabc".getBytes());
        InputStreamTokenizer reader = new InputStreamTokenizer(is, 4);
        byte[] buffer = new byte[20];
        int count = reader.readUntil("x".getBytes(), buffer);
        assertThat("expected to read 3 bytes", count, is(3));
        assertThat("expected buffer to contain 'tes'", buffer, startsWith("tes".getBytes()));
    }

    @Test
    public void should_find_sequence_at_second_block_start() throws IOException {
        InputStream is = new ByteArrayInputStream("testxabc".getBytes());
        InputStreamTokenizer reader = new InputStreamTokenizer(is, 4);
        byte[] buffer = new byte[20];
        int count = reader.readUntil("x".getBytes(), buffer);
        assertThat("expected to read 4 bytes", count, is(4));
        assertThat("expected buffer to contain 'test'", buffer, startsWith("test".getBytes()));
    }

    @Test
    public void should_find_sequence_at_second_block_end() throws IOException {
        InputStream is = new ByteArrayInputStream("testabcx".getBytes());
        InputStreamTokenizer reader = new InputStreamTokenizer(is, 4);
        byte[] buffer = new byte[20];
        int count = reader.readUntil("x".getBytes(), buffer);
        assertThat("expected to read 7 bytes", count, is(7));
        assertThat("expected buffer to contain 'testabc'", buffer, startsWith("testabc".getBytes()));
    }

    @Test
    public void should_find_sequence_twice_within_block() throws IOException {
        InputStream is = new ByteArrayInputStream("testxabcx".getBytes());
        InputStreamTokenizer reader = new InputStreamTokenizer(is, 1024);
        byte[] buffer = new byte[20];

        int count = reader.readUntil("x".getBytes(), buffer);
        assertThat("expected to read 4 bytes", count, is(4));
        assertThat("expected buffer to contain 'test'", buffer, startsWith("test".getBytes()));

        count = reader.readUntil("x".getBytes(), buffer);
        assertThat("expected to read 3 bytes", count, is(3));
        assertThat("expected buffer to contain 'abc'", buffer, startsWith("abc".getBytes()));
    }

    @Test
    public void should_find_sequence_overlapping_itself() throws IOException {
        InputStream is = new ByteArrayInputStream("xxxxxx".getBytes());
        InputStreamTokenizer reader = new InputStreamTokenizer(is, 1024);
        byte[] buffer = new byte[20];

        int count = reader.readUntil("xxx".getBytes(), buffer);
        assertThat("expected to read 0 bytes", count, is(0));

        count = reader.readUntil("xxx".getBytes(), buffer);
        assertThat("expected to read 0 bytes", count, is(0));

        count = reader.readUntil("xxx".getBytes(), buffer);
        assertThat("expected to read 0 bytes (until end of stream)", count, is(0));

        count = reader.readUntil("xxx".getBytes(), buffer);
        assertThat("expected eof", count, is(-1));
    }

    @Test
    public void should_find_sequence_overlapping_itself_and_block_boundary() throws IOException {
        InputStream is = new ByteArrayInputStream("xxxxxxxx".getBytes());
        InputStreamTokenizer reader = new InputStreamTokenizer(is, 4);
        byte[] buffer = new byte[20];

        int count = reader.readUntil("xxx".getBytes(), buffer);
        assertThat("expected to read 0 bytes", count, is(0));

        count = reader.readUntil("xxx".getBytes(), buffer);
        assertThat("expected to read 0 bytes", count, is(0));

        count = reader.readUntil("xxx".getBytes(), buffer);
        assertThat("expected to read 2 bytes (until end of stream)", count, is(2));
        assertThat("expected buffer to contain 'xx'", buffer, startsWith("xx".getBytes()));

        count = reader.readUntil("xxx".getBytes(), buffer);
        assertThat("expected eof", count, is(-1));
    }

    @Test
    public void should_find_sequence_overlapping_block_boundary() throws IOException {
        InputStream is = new ByteArrayInputStream("abcxyzdef".getBytes());
        InputStreamTokenizer reader = new InputStreamTokenizer(is, 4);
        byte[] buffer = new byte[20];

        int count = reader.readUntil("xyz".getBytes(), buffer);
        assertThat("expected to read 3 bytes", count, is(3));
        assertThat("expected buffer to contain 'abc'", buffer, startsWith("abc".getBytes()));

        count = reader.readUntil("xyz".getBytes(), buffer);
        assertThat("expected to read 3 bytes", count, is(3));
        assertThat("expected buffer to contain 'def'", buffer, startsWith("def".getBytes()));
    }

    @Test
    public void should_throw_if_buffer_too_small() throws IOException {
        InputStream is = new ByteArrayInputStream("abcxyzabc".getBytes());
        InputStreamTokenizer reader = new InputStreamTokenizer(is, 4);
        byte[] buffer = new byte[2];

        try {
            reader.readUntil("xyz".getBytes(), buffer);
            fail("expected an exception");
        } catch (IllegalArgumentException e) {
            // expected an illegal argument exception the first time - buffer not big enough
        }

        try {
            reader.readUntil("xyz".getBytes(), buffer);
            fail("expected another exception");
        } catch (IllegalStateException e) {
            // expected an illegal argument exception the second time - cannot recover from earlier exception
        }
    }

    @Test
    public void should_read_entire_buffer_if_delimiter_not_found() throws IOException {
        InputStream is = new ByteArrayInputStream("abcdef".getBytes());
        InputStreamTokenizer reader = new InputStreamTokenizer(is, 4);
        byte[] buffer = new byte[20];

        int count = reader.readUntil("123".getBytes(), buffer);
        assertThat("expected to read 6 bytes", count, is(6));
        assertThat("expected buffer to contain 'abcdef'", buffer, startsWith("abcdef".getBytes()));

        count = reader.readUntil("123".getBytes(), buffer);
        assertThat("expected eof", count, is(-1));
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
