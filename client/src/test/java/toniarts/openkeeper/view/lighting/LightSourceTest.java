/*
 * Copyright (C) 2014-2025 OpenKeeper
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
package toniarts.openkeeper.view.lighting;

import com.jme3.math.Vector3f;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link LightSource}.
 */
class LightSourceTest {

    @Test
    void radiusIsCappedAtNineAndSevenEighthsTiles() {
        LightSource source = new LightSource(LightSource.Kind.PORTAL, null, Vector3f.ZERO,
                255, 33, 26, 1.0f, 50f);

        assertEquals(LightConstants.MAX_LIGHT_RADIUS_TILES, source.getRadius());
    }

    @Test
    void flickerScaleStaysClampedBetweenHalfAndOne() {
        LightSource source = new LightSource(LightSource.Kind.TORCH, null, Vector3f.ZERO,
                300, 200, 100, 1.41f, 1.5f);
        source.setFlickers(true);
        Random random = new Random(42);

        for (int i = 0; i < 10_000; i++) {
            source.flickerStep(random);
            float effective = source.getEffectiveIntensity();
            assertTrue(effective >= source.getIntensity() * LightConstants.FLICKER_MIN);
            assertTrue(effective <= source.getIntensity() * LightConstants.FLICKER_MAX);
        }
    }

    @Test
    void nonFlickeringSourceIgnoresFlickerStep() {
        LightSource source = new LightSource(LightSource.Kind.TERRAIN, null, Vector3f.ZERO,
                200, 80, 35, 1.0f, 4.5f);
        Random random = new Random(1);

        source.flickerStep(random);

        assertEquals(source.getIntensity(), source.getEffectiveIntensity());
    }

    @Test
    void tickCountsLifetimeDownAndSignalsExpiry() {
        LightSource source = new LightSource(LightSource.Kind.THING, null, Vector3f.ZERO,
                255, 255, 255, 1.0f, 5f);
        source.setLifetime(3);

        assertTrue(source.tick());
        assertTrue(source.tick());
        assertFalse(source.tick());
    }

    @Test
    void infiniteLifetimeNeverExpires() {
        LightSource source = new LightSource(LightSource.Kind.CURSOR, null, Vector3f.ZERO,
                511, 511, 511, 2.0f, 7f);

        for (int i = 0; i < 1000; i++) {
            assertTrue(source.tick());
        }
    }
}
