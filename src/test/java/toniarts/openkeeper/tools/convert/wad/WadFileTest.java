package toniarts.openkeeper.tools.convert.wad;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WadFileTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void readsUncompressedEntriesAndRejectsDeclaredSizeMismatch() throws Exception {
        byte[] data = new byte[]{10, 20, 30, 40};
        Path validArchive = writeArchive("safe.bin", data, data.length);
        assertArrayEquals(data, new WadFile(validArchive).getFileData("safe.bin"));

        Path malformedArchive = writeArchive("wrong-size.bin", data, data.length + 1);
        assertThrows(RuntimeException.class, () -> new WadFile(malformedArchive).getFileData("wrong-size.bin"));
    }

    @Test
    void acceptsCompressedEntriesWithUnspecifiedTableSize() throws Exception {
        byte[] compressed = {
            0, 0, 0, 0, 7, (byte) 0xE0, 'a', 'b', 'c', 'd', (byte) 0xFF, 'e', 'f', 'g'
        };
        Path archive = writeArchive("compressed.bin", compressed, 0, 4);

        assertArrayEquals("abcdefg".getBytes(StandardCharsets.US_ASCII),
                new WadFile(archive).getFileData("compressed.bin"));
    }

    @Test
    void extractionRejectsEntriesThatEscapeTheDestination() throws Exception {
        Path archivePath = writeArchive("../escaped.bin", new byte[]{1}, 1);
        WadFile wad = new WadFile(archivePath);
        Path destination = temporaryDirectory.resolve("converted");

        assertThrows(RuntimeException.class,
                () -> wad.extractFileData(wad.getWadFileEntries().get(0), destination.toString()));
        assertFalse(Files.exists(temporaryDirectory.resolve("escaped.bin")));
    }

    private Path writeArchive(String entryName, byte[] data, int declaredSize) throws IOException {
        return writeArchive(entryName, data, declaredSize, 0);
    }

    private Path writeArchive(String entryName, byte[] data, int declaredSize, int type) throws IOException {
        byte[] name = entryName.getBytes(StandardCharsets.US_ASCII);
        int entryTableOffset = 0x58;
        int namesOffset = 0x80;
        int dataOffset = namesOffset + name.length;
        byte[] archive = new byte[dataOffset + data.length];
        archive[0] = 'D';
        archive[1] = 'W';
        archive[2] = 'F';
        archive[3] = 'B';

        ByteBuffer buffer = ByteBuffer.wrap(archive).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(4, 2);
        buffer.putInt(0x48, 1);
        buffer.putInt(0x4C, namesOffset);
        buffer.putInt(0x50, name.length);
        buffer.putInt(0x54, 0);

        buffer.putInt(entryTableOffset, 0);
        buffer.putInt(entryTableOffset + 4, namesOffset);
        buffer.putInt(entryTableOffset + 8, name.length);
        buffer.putInt(entryTableOffset + 12, dataOffset);
        buffer.putInt(entryTableOffset + 16, data.length);
        buffer.putInt(entryTableOffset + 20, type);
        buffer.putInt(entryTableOffset + 24, declaredSize);
        buffer.putInt(entryTableOffset + 28, 0);
        buffer.putInt(entryTableOffset + 32, 0);
        buffer.putInt(entryTableOffset + 36, 0);
        System.arraycopy(name, 0, archive, namesOffset, name.length);
        System.arraycopy(data, 0, archive, dataOffset, data.length);

        Path path = temporaryDirectory.resolve("fixture-" + Math.abs(entryName.hashCode()) + ".wad");
        Files.write(path, archive);
        return path;
    }
}
