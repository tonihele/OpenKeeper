/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.tools.hdtextures;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import toniarts.openkeeper.tools.hdtextures.HdTextures.Planes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the HD texture tool's own logic, without Real-ESRGAN, the DK II files
 * or the network
 */
class HdTexturesTest {

    private static final List<String> TEXTURES = List.of("Effects/Glow01.png", "GUI/Icons/Lair.png",
            "GUI/Map/MapColours.png", "GUI/Rooms/Lair.png", "GUI/Windows/Panel.png", "Lair_Wall.png",
            "Maps/Map00001.png", "Sub/T_Lava.png", "T_Lava.png", "cursor/Arrow.png");

    @TempDir
    private Path folder;

    @Test
    void selectLeavesOutTheExcludedTexturesUnlessAll() {
        assertEquals(List.of("Effects/Glow01.png", "GUI/Icons/Lair.png", "GUI/Rooms/Lair.png",
                "GUI/Windows/Panel.png", "Lair_Wall.png", "Sub/T_Lava.png", "T_Lava.png"),
                HdTextures.select(TEXTURES, false, null));
        assertEquals(TEXTURES, HdTextures.select(TEXTURES, true, null));
        assertTrue(HdTextures.isExcluded("CURSOR/arrow.png"));
    }

    @Test
    void selectPicksTheBatches() {
        assertEquals(List.of("Lair_Wall.png", "T_Lava.png"), HdTextures.select(TEXTURES, false, HdTextures.TEST_BATCH));
        assertEquals(List.of("GUI/Rooms/Lair.png", "GUI/Windows/Panel.png"),
                HdTextures.select(TEXTURES, false, HdTextures.GUI_TEST_BATCH));
        assertEquals(List.of("GUI/Icons/Lair.png"), HdTextures.select(TEXTURES, false, "(?i)^gui/icons/"));
    }

    @Test
    void parseOptionsChecksTheChoices() {
        Map<String, String> options = HdTextures.parseOptions(new String[]{"upscale", "--gpu", "-1", "--dry-run"});
        assertEquals("upscale", options.get("command"));
        assertEquals("-1", options.get("--gpu"));
        assertTrue(options.containsKey("--dry-run"));
        assertThrows(IllegalArgumentException.class, () -> HdTextures.parseOptions(new String[]{"upscale", "--model", "x"}));
        assertThrows(IllegalArgumentException.class, () -> HdTextures.parseOptions(new String[]{"upscale", "--test-batch", "--gui-test-batch"}));
        assertThrows(IllegalArgumentException.class, () -> HdTextures.parseOptions(new String[]{"upscale", "--nope"}));
    }

    @Test
    void animationFramesShareAKey() {
        assertEquals("effects/glow", HdTextures.animationKey("Effects/Glow01.png"));
        assertEquals("effects/glow", HdTextures.animationKey("effects/GLOW2.PNG"));
        assertEquals("t_lava", HdTextures.animationKey("T_Lava.png"));
    }

    @Test
    void softTexturesAreResizedSmoothly() throws IOException {
        write("Soft.png", smooth());
        write("Sharp.png", noise(1));
        write("Flat.png", new Planes(8, 8, new float[][]{new float[64], new float[64], new float[64]}));
        // Of the frames of an animation one is soft, the median is detailed
        write("Fire1.png", smooth());
        write("Fire2.png", noise(2));
        write("Fire3.png", noise(3));
        List<String> all = HdTextures.listTextures(folder);

        Map<String, Double> weights = HdTextures.detailWeights(folder, all, all);
        assertEquals(0.0, weights.get("Soft.png"));
        assertEquals(1.0, weights.get("Sharp.png"));
        assertEquals(0.0, weights.get("Flat.png"));
        assertEquals(1.0, weights.get("Fire1.png"));
        assertEquals(1.0, weights.get("Fire3.png"));
        assertNull(HdTextures.detailScore(read("Flat.png")));
    }

    @Test
    void padFillsLikeNumpy() {
        Planes row = new Planes(3, 1, new float[][]{{0, 1, 2}});
        assertArrayEquals(new float[]{1, 2, 0, 1, 2, 0, 1}, middleRow(HdTextures.pad(row, 2, "wrap")));
        assertArrayEquals(new float[]{0, 0, 0, 1, 2, 2, 2}, middleRow(HdTextures.pad(row, 2, "edge")));
        assertArrayEquals(new float[]{2, 1, 0, 1, 2, 1, 0}, middleRow(HdTextures.pad(row, 2, "reflect")));
    }

    @Test
    void resizeMatchesPillow() {
        // Expected values from Pillow's Image.resize
        float[] bicubic = HdTextures.resize(new float[]{0, 255, 30, 100, 200, 10, 90, 255}, 4, 2, true);
        assertArrayEquals(new float[]{0, 0, 14, 89, 181, 254, 255, 202, 118, 46, 22, 34, 61, 85, 96, 100},
                Arrays.copyOfRange(bicubic, 16, 32));
        float[] bilinear = HdTextures.resize(new float[]{0.5f, -2, 3.25f, 1, 4, -1.5f}, 3, 2, false);
        assertArrayEquals(new float[]{0.5625f, 0.5625f, 0.3359375f, -0.1171875f, -0.5703125f, -1.0234375f,
            -0.76171875f, 0.21484375f, 1.19140625f, 2.16796875f, 2.65625f, 2.65625f},
                Arrays.copyOfRange(bilinear, 24, 36));
    }

    @Test
    void bleedSpreadsTheVisibleColour() {
        float[][] rgb = HdTextures.bleed(new float[][]{{10, 0, 0, 0}, {20, 0, 0, 0}, {30, 0, 0, 0}},
                new float[]{255, 0, 0, 0}, 4, 1);
        assertArrayEquals(new float[]{10, 10, 10, 10}, rgb[0]);
        assertArrayEquals(new float[]{30, 30, 30, 30}, rgb[2]);
    }

    @Test
    void cutOutAlphaStaysCutOut() {
        float[] alpha = new float[16];
        for (int i = 0; i < 16; i++) {
            alpha[i] = i % 3 == 0 ? 255 : 0;
        }
        Planes original = new Planes(4, 4, new float[][]{new float[16], new float[16], new float[16], alpha});
        Planes hd = HdTextures.finish(original, HdTextures.prepare(original, 2, "wrap"), null, 0, 2, true);

        assertEquals(16, hd.width());
        assertEquals(16, hd.height());
        for (float value : hd.bands()[3]) {
            assertTrue(value == 0 || value == 255);
        }
    }

    @Test
    void constantAlphaIsKept() {
        float[] alpha = new float[16];
        Arrays.fill(alpha, 128);
        Planes original = new Planes(4, 4, new float[][]{new float[16], new float[16], new float[16], alpha});
        Planes[] padded = HdTextures.prepare(original, 2, "wrap");
        assertNull(padded[1]);

        Planes hd = HdTextures.finish(original, padded, null, 0, 2, true);
        for (float value : hd.bands()[3]) {
            assertEquals(128, value);
        }
    }

    @Test
    void cleanDeletesOnlyWhatTheManifestLists() throws IOException, InterruptedException {
        Path ours = folder.resolve("GUI/Rooms/Lair.png");
        Path theirs = folder.resolve("ModelViewer/marble.jpg");
        Files.createDirectories(ours.getParent());
        Files.createDirectories(theirs.getParent());
        Files.writeString(ours, "x");
        Files.writeString(theirs, "x");
        Properties manifest = new Properties();
        manifest.setProperty("GUI/Rooms/Lair.png", "settings");
        HdTextures.saveManifest(folder, manifest);
        assertEquals("settings", HdTextures.loadManifest(folder).getProperty("GUI/Rooms/Lair.png"));

        assertEquals(1, HdTextures.clean(folder));
        assertFalse(Files.exists(folder.resolve("GUI")));
        assertTrue(Files.exists(theirs));
        assertTrue(HdTextures.loadManifest(folder).isEmpty());
    }

    @Test
    void unzipKeepsEntriesInsideTheTarget() throws IOException {
        Path archive = folder.resolve("evil.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("../evil.txt"));
            zip.write(1);
        }
        assertThrows(IOException.class, () -> HdTextures.unzip(archive, folder.resolve("out")));
        assertFalse(Files.exists(folder.resolve("evil.txt")));
    }

    private static float[] middleRow(Planes padded) {
        return Arrays.copyOfRange(padded.bands()[0], 2 * padded.width(), 3 * padded.width());
    }

    private static Planes smooth() {
        float[] grey = new float[32 * 32];
        for (int i = 0; i < grey.length; i++) {
            int x = i % 32 - 16;
            int y = i / 32 - 16;
            grey[i] = (float) (255 * Math.exp(-(x * x + y * y) / 100.0));
        }
        return new Planes(32, 32, new float[][]{grey, grey, grey});
    }

    private static Planes noise(long seed) {
        Random random = new Random(seed);
        float[] grey = new float[32 * 32];
        for (int i = 0; i < grey.length; i++) {
            grey[i] = random.nextInt(256);
        }
        return new Planes(32, 32, new float[][]{grey, grey, grey});
    }

    private void write(String name, Planes image) throws IOException {
        HdTextures.write(image, folder.resolve(name));
    }

    private Planes read(String name) throws IOException {
        return HdTextures.read(folder.resolve(name));
    }
}
