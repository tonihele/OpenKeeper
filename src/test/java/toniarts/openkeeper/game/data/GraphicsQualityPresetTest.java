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
package toniarts.openkeeper.game.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class GraphicsQualityPresetTest {

    @Test
    void lowDisablesSmaaAndEnablesCheapLighting() {
        assertEquals(AntiAliasingMode.OFF, GraphicsQualityPreset.LOW.getAntiAliasing());
        assertFalse(GraphicsQualityPreset.LOW.isSsao());
        assertEquals(0, GraphicsQualityPreset.LOW.getAnisotropy());
        assertTrue(GraphicsQualityPreset.LOW.isCheapLighting());
    }

    @Test
    void highMatchesHistoricalShippingDefaults() {
        assertEquals(AntiAliasingMode.SMAA_ULTRA, GraphicsQualityPreset.HIGH.getAntiAliasing());
        assertFalse(GraphicsQualityPreset.HIGH.isSsao());
        assertEquals(0, GraphicsQualityPreset.HIGH.getAnisotropy());
        assertFalse(GraphicsQualityPreset.HIGH.isCheapLighting());
    }

    @Test
    void fromStoredNameAcceptsLabelAndEnum() {
        assertEquals(GraphicsQualityPreset.LOW, GraphicsQualityPreset.fromStoredName("LOW"));
        assertEquals(GraphicsQualityPreset.MEDIUM, GraphicsQualityPreset.fromStoredName("Medium"));
        assertEquals(GraphicsQualityPreset.HIGH, GraphicsQualityPreset.fromStoredName("nope"));
    }
}
