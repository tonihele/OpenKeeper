package toniarts.openkeeper.tools.convert.conversion.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConvertTexturesEnhancedLiquidsTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void copiesOnlyKnownLiquidResourcesAndNormalizesFrameCase() throws IOException {
        Path source = Files.createDirectory(temporaryDirectory.resolve("source"));
        Path destination = temporaryDirectory.resolve("converted");
        Files.createFile(source.resolve("BumpLava0.png"));
        Files.createFile(source.resolve("BumpWaterBumpMap1.png"));
        Files.createFile(source.resolve("Unrelated.png"));

        ConvertTextures.copyEnhancedLiquidTextures(source, destination);

        assertTrue(Files.exists(destination.resolve("BumpLava0.png")));
        assertTrue(Files.exists(destination.resolve("BumpWaterBumpmap1.png")));
        assertEquals(2, countFiles(destination));
    }

    @Test
    void copiesTheCompleteWaterBumpFrameSetWhenPresent() throws IOException {
        Path source = Files.createDirectory(temporaryDirectory.resolve("water"));
        Path destination = temporaryDirectory.resolve("converted-water");
        for (int frame = 0; frame < 32; frame++) {
            String name = "BumpWaterBumpmap" + frame + ".png";
            if (frame == 1) {
                name = "BumpWaterBumpMap1.png";
            }
            Files.createFile(source.resolve(name));
        }

        ConvertTextures.copyEnhancedLiquidTextures(source, destination);

        for (int frame = 0; frame < 32; frame++) {
            assertTrue(Files.exists(destination.resolve("BumpWaterBumpmap" + frame + ".png")),
                    "Missing water bump frame " + frame);
        }
        assertEquals(32, countFiles(destination));
    }

    private static long countFiles(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files.count();
        }
    }
}
