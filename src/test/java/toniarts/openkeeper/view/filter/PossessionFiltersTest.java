/* Copyright (C) 2026 OpenKeeper. Licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.filter;

import java.nio.FloatBuffer;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.Creature.GammaEffect;
import static org.junit.jupiter.api.Assertions.*;

class PossessionFiltersTest {
    @Test void generatedCurvesPreserveCreatureColourResponses() {
        var pixels = PossessionGamma.createRamps().asFloatBuffer();
        assertArrayEquals(new int[]{0, 0, 0}, sample(pixels, 3, 0));
        assertArrayEquals(new int[]{16383, 16383, 49151}, sample(pixels, 3, 128));
        assertArrayEquals(new int[]{32639, 32639, 65535}, sample(pixels, 3, 255));
        assertArrayEquals(new int[]{32767, 10922, 10922}, sample(pixels, 1, 128));
        assertArrayEquals(new int[]{32767, 10922, 32767}, sample(pixels, 2, 128));
        assertArrayEquals(new int[]{65535, 32767, 32767}, sample(pixels, 4, 128));
        assertArrayEquals(new int[]{32767, 32767, 65535}, sample(pixels, 5, 128));
        assertArrayEquals(new int[]{0, 13107, 0}, sample(pixels, 15, 0));
        assertArrayEquals(new int[]{0, 0, 49151}, sample(pixels, 14, 128));
        assertArrayEquals(new int[]{16383, 0, 0}, sample(pixels, 9, 128));
    }

    @Test void everyPresetHasFiniteNormalizedChannelsAndOpaqueAlpha() {
        var pixels = PossessionGamma.createRamps().asFloatBuffer();
        assertEquals(16 * 256 * 4, pixels.remaining());
        for (int e = 0; e < 16; e++) for (int i = 0; i < 256; i++) {
            for (int c = 0; c < 3; c++) {
                float value = pixels.get((e * 256 + i) * 4 + c);
                assertTrue(Float.isFinite(value) && value >= 0 && value <= 1);
            }
            assertEquals(1f, pixels.get((e * 256 + i) * 4 + 3));
            assertArrayEquals(new int[]{65535, 65535, 65535}, sample(pixels, 6, i));
            assertArrayEquals(new int[]{0, 0, 0}, sample(pixels, 7, i));
            assertArrayEquals(new int[]{65535, 0, 0}, sample(pixels, 8, i));
        }
    }

    @Test void experimentalPresetsKeepTheirDistinctChannelOrder() {
        var pixels = PossessionGamma.createRamps().asFloatBuffer();
        assertArrayEquals(new int[]{0, 0, 46340}, sample(pixels, 12, 16));
        assertArrayEquals(new int[]{46340, 0, 0}, sample(pixels, 12, 80));
        assertArrayEquals(new int[]{0, 46340, 0}, sample(pixels, 12, 144));
        assertArrayEquals(new int[]{0, 46340, 0}, sample(pixels, 13, 80));
        assertArrayEquals(new int[]{46340, 0, 0}, sample(pixels, 13, 144));
        int[] grey = sample(pixels, 10, 208);
        assertTrue(grey[0] > 0);
        assertEquals(grey[0], grey[1]);
        assertEquals(grey[0], grey[2]);
    }

    private static int[] sample(FloatBuffer pixels, int effect, int input) {
        int offset = (effect * 256 + input) * 4;
        return new int[]{Math.round(pixels.get(offset) * 65535),
            Math.round(pixels.get(offset + 1) * 65535), Math.round(pixels.get(offset + 2) * 65535)};
    }

    @Test void doesNotInventGhostColours() {
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
