/* Copyright (C) 2026 OpenKeeper. Licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.filter;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.Creature.GammaEffect;
import static org.junit.jupiter.api.Assertions.*;

class PossessionFiltersTest {
    @Test void nativeTablesPreserveAuthoredColourCurvesRatherThanAddingTint() throws Exception {
        byte[] bytes = Files.readAllBytes(Path.of("assets", PossessionGamma.RAMPS));
        ByteBuffer raw = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        assertArrayEquals(new int[]{0, 0, 0}, sample(raw, 3, 0));
        assertArrayEquals(new int[]{16383, 16383, 49151}, sample(raw, 3, 128));
        assertArrayEquals(new int[]{32639, 32639, 65535}, sample(raw, 3, 255));
        assertArrayEquals(new int[]{32767, 10922, 10922}, sample(raw, 1, 128));
        assertArrayEquals(new int[]{32767, 10922, 32767}, sample(raw, 2, 128));
        assertArrayEquals(new int[]{65535, 32767, 32767}, sample(raw, 4, 128));
        assertArrayEquals(new int[]{32767, 32767, 65535}, sample(raw, 5, 128));
        assertArrayEquals(new int[]{0, 13107, 0}, sample(raw, 15, 0));
        var pixels = PossessionGamma.readRamps(new ByteArrayInputStream(bytes)).asFloatBuffer();
        for (int e = 0; e < 16; e++) for (int i = 0; i < 256; i++) {
            int[] expected = sample(raw, e, i);
            for (int c = 0; c < 3; c++) assertEquals(expected[c] / 65535f, pixels.get((e * 256 + i) * 4 + c));
            assertEquals(1f, pixels.get((e * 256 + i) * 4 + 3));
        }
    }

    private static int[] sample(ByteBuffer bytes, int effect, int input) {
        int offset = (effect * 256 + input) * 6;
        return new int[]{Short.toUnsignedInt(bytes.getShort(offset)),
            Short.toUnsignedInt(bytes.getShort(offset + 2)), Short.toUnsignedInt(bytes.getShort(offset + 4))};
    }

    @Test void rejectsTruncatedTablesAndDoesNotInventGhostColours() {
        assertThrows(java.io.IOException.class, () -> PossessionGamma.readRamps(new ByteArrayInputStream(new byte[6])));
        assertTrue(PossessionGamma.isSupported(GammaEffect.MAIDEN));
        assertFalse(PossessionGamma.isSupported(GammaEffect.GHOST));
        assertFalse(PossessionGamma.isSupported(null));
    }

    @Test void resolvesStockMasksAndAuthoredOverrides() {
        assertEquals("Textures/GUI/Filters/F-FireFly.png", PossessionMasks.resolve(12, null));
        assertEquals("Textures/GUI/Filters/F-Knight.png", PossessionMasks.resolve(13, null));
        assertEquals("Textures/GUI/Filters/F-Black_Knight.png", PossessionMasks.resolve(22, null));
        assertEquals("Textures/GUI/Filters/F-Guard.png", PossessionMasks.resolve(25, null));
        assertNull(PossessionMasks.resolve(9, null));
        assertEquals("Textures/GUI/Filters/F-Sniper.png", PossessionMasks.resolve(12, "F-Sniper"));
        assertEquals("Textures/GUI/Filters/F-Frozen.png", PossessionMasks.resolve(12, "GUI\\Filters\\F-Frozen"));
        assertEquals("Textures/GUI/Filters/custom.png", PossessionMasks.resolve(9, "Textures/GUI/Filters/custom.png"));
    }
}
