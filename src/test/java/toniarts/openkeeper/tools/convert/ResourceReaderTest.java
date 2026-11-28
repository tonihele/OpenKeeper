package toniarts.openkeeper.tools.convert;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.nio.ReadOnlyBufferException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceReaderTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void bufferedReaderCompletesReadsAcrossShortUnderlyingReads() throws Exception {
        try (BufferedResourceReader reader = new BufferedResourceReader(
                new ThrottledInputStream(new byte[]{1, 2, 3, 4, 5}, 2, 1))) {
            assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, reader.read(5));
        }
    }

    @Test
    void bufferedReaderFillsRequestedChunkAndReturnsOnlyTheEofRemainder() throws Exception {
        try (BufferedResourceReader reader = new BufferedResourceReader(
                new ThrottledInputStream(new byte[]{1, 2, 3, 4, 5, 6}, 2, 1))) {
            IResourceChunkReader first = reader.readChunk(5);
            assertNotNull(first);
            assertEquals(5, first.length());
            assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, first.read(5));
            assertEquals(5, first.length());

            IResourceChunkReader remainder = reader.readChunk(5);
            assertNotNull(remainder);
            assertEquals(1, remainder.length());
            assertEquals(6, remainder.readUnsignedByte());
            assertNull(reader.readChunk(5));
        }
    }

    @Test
    void bufferedSkipConsumesTheExactLengthAndRejectsTruncation() throws Exception {
        try (BufferedResourceReader reader = new BufferedResourceReader(
                new ThrottledInputStream(new byte[]{1, 2, 3, 4, 5, 6}, 2, 1))) {
            reader.skipBytes(4);
            assertArrayEquals(new byte[]{5, 6}, reader.read(2));
            assertThrows(EOFException.class, () -> reader.skipBytes(1));
        }
    }

    @Test
    void byteArrayReaderReturnsPartialReadOnlyChunkAtEof() throws Exception {
        try (ByteArrayResourceReader reader = new ByteArrayResourceReader(new byte[]{1, 2, 3})) {
            IResourceChunkReader chunk = reader.readChunk(5);
            assertNotNull(chunk);
            assertEquals(3, chunk.length());
            assertArrayEquals(new byte[]{1, 2, 3}, chunk.read(3));
            assertThrows(ReadOnlyBufferException.class, () -> chunk.getByteBuffer().put((byte) 4));
            assertNull(reader.readChunk(1));
            assertThrows(EOFException.class, () -> reader.skipBytes(1));
        }
    }

    @Test
    void fileReaderReadsAndSkipsExactLengths() throws Exception {
        Path path = temporaryDirectory.resolve("reader.bin");
        Files.write(path, new byte[]{1, 2, 3, 4, 5, 6});
        try (FileResourceReader reader = new FileResourceReader(path)) {
            assertArrayEquals(new byte[]{1, 2}, reader.read(2));
            reader.skipBytes(2);
            assertArrayEquals(new byte[]{5, 6}, reader.read(2));
            assertThrows(EOFException.class, () -> reader.read(1));
            assertThrows(EOFException.class, () -> reader.skipBytes(1));
            reader.seek(10);
            assertEquals(0, reader.readAll().length());
        }
    }

    @Test
    void negativeLengthsAreRejectedConsistently() throws Exception {
        try (BufferedResourceReader reader = new BufferedResourceReader(new ByteArrayInputStream(new byte[0]));
                ByteArrayResourceReader byteArrayReader = new ByteArrayResourceReader(new byte[0])) {
            assertThrows(IllegalArgumentException.class, () -> reader.readChunk(-1));
            assertThrows(IllegalArgumentException.class, () -> reader.skipBytes(-1));
            assertThrows(IllegalArgumentException.class, () -> byteArrayReader.read(-1));
        }
    }

    private static final class ThrottledInputStream extends InputStream {

        private final byte[] data;
        private final int maxRead;
        private final int maxSkip;
        private int position;

        private ThrottledInputStream(byte[] data, int maxRead, int maxSkip) {
            this.data = data;
            this.maxRead = maxRead;
            this.maxSkip = maxSkip;
        }

        @Override
        public int read() {
            return position == data.length ? -1 : data[position++] & 0xFF;
        }

        @Override
        public int read(byte[] destination, int offset, int length) {
            if (position == data.length) {
                return -1;
            }
            int count = Math.min(Math.min(length, maxRead), data.length - position);
            System.arraycopy(data, position, destination, offset, count);
            position += count;
            return count;
        }

        @Override
        public long skip(long amount) {
            int count = (int) Math.min(Math.min(amount, maxSkip), data.length - position);
            position += count;
            return count;
        }
    }
}
