package toniarts.openkeeper.tools.convert.wad;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WadEntryPathResolverTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void resolvesNormalEntriesInsideDestination() throws Exception {
        Path destination = temporaryDirectory.resolve("converted");
        Path expected = destination.toAbsolutePath().normalize().resolve("Textures/wall.png");
        assertEquals(expected, WadEntryPathResolver.resolve(destination, "Textures/wall.png"));
    }

    @Test
    void rejectsParentTraversalAndAbsoluteEntries() {
        Path destination = temporaryDirectory.resolve("converted");
        assertThrows(IOException.class, () -> WadEntryPathResolver.resolve(destination, "../outside.bin"));
        assertThrows(IOException.class, () -> WadEntryPathResolver.resolve(destination, "."));
        assertThrows(IOException.class, () -> WadEntryPathResolver.resolve(
                destination, temporaryDirectory.resolve("outside.bin").toString()));
    }
}
