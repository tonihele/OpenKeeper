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
package toniarts.openkeeper.view.shadow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ShadowMath} and {@link ShadowRange}.
 */
class ShadowMathTest {

    @Test
    void opacityIsFullStrengthUpToAboutHalfTheRangeThenFadesToNothing() {
        assertEquals(0.7f, ShadowMath.opacity(2.5f, 0f), 1e-6f);
        assertEquals(0.7f, ShadowMath.opacity(2.5f, 0.5f * 2.5f), 1e-6f);
        assertEquals(1.5f * 0.25f, ShadowMath.opacity(2.5f, 0.75f * 2.5f), 1e-6f);
        assertEquals(0f, ShadowMath.opacity(2.5f, 2.5f), 1e-6f);
        assertTrue(ShadowMath.opacity(2.5f, 3f) < 0f);
    }

    @Test
    void tiltIsSixtySevenAndAHalfDegreesAtTheFullRange() {
        assertEquals(0f, ShadowMath.tilt(0f));
        assertEquals(Math.toRadians(67.5), ShadowMath.tilt(2.5f), 1e-3);
    }

    @Test
    void lowLevelQuadIsAPointLightProjectionFromAVirtualHeightOfTwo() {
        ShadowMath.Extent extent = ShadowMath.extent(2, 2f, 0.5f, 0.4f);

        assertEquals(2f * (0.5f - 0.4f) / 2f, extent.near(), 1e-6f);
        assertEquals(2f * (0.5f + 0.4f) / 2f, extent.far(), 1e-6f);
        assertEquals(2f * 0.4f, extent.far() - extent.near(), 1e-6f); // d * s
    }

    @Test
    void tiltedQuadMatchesTheTiltedSquareProjection() {
        float distance = 1.8f;
        float height = 0.6f;
        float radius = 0.45f;
        float theta = ShadowMath.tilt(distance);

        ShadowMath.Extent extent = ShadowMath.extent(3, distance, height, radius);

        // far = (h + s sin) tan + s cos, near = (h - s sin) tan - s cos
        assertEquals((height + radius * Math.sin(theta)) * Math.tan(theta) + radius * Math.cos(theta), extent.far(), 1e-4);
        assertEquals((height - radius * Math.sin(theta)) * Math.tan(theta) - radius * Math.cos(theta), extent.near(), 1e-4);
    }

    @Test
    void tiltedQuadUnderTheLightIsTheFootprint() {
        ShadowMath.Extent extent = ShadowMath.extent(3, 0f, 0.6f, 0.45f);

        assertEquals(-0.45f, extent.near(), 1e-6f);
        assertEquals(0.45f, extent.far(), 1e-6f);
    }

    @Test
    void rangeStaysAtItsCapAtTwentyFramesPerSecond() {
        ShadowRange range = new ShadowRange();
        for (int i = 0; i < 100; i++) {
            range.update(1f / 20f);
        }

        assertEquals(2.5f, range.get(), 1e-6f);
    }

    @Test
    void rangeFallsStepwiseToNearlyNothingAtTenFramesPerSecondAndIsGoneBelow() {
        ShadowRange range = new ShadowRange();
        range.update(1f / 10f);
        assertEquals(2.4f, range.get(), 1e-5f);

        // The target is 0, the dead band leaves the range half a tile at most
        for (int i = 0; i < 100; i++) {
            range.update(1f / 10f);
        }
        assertTrue(range.get() <= 0.5f + 1e-5f);

        for (int i = 0; i < 100; i++) {
            range.update(1f / 5f);
        }
        assertEquals(0f, range.get(), 1e-6f);
    }

    @Test
    void rangeRecoversGraduallyAndKeepsAnHalfTileDeadBand() {
        ShadowRange range = new ShadowRange();
        for (int i = 0; i < 100; i++) {
            range.update(1f / 5f);
        }

        range.update(1f / 20f);
        assertEquals(0.1f, range.get(), 1e-5f);

        // 14 fps targets 1.0: inside the dead band of 0.5 around it nothing moves
        ShadowRange settled = new ShadowRange();
        for (int i = 0; i < 100; i++) {
            settled.update(1f / 14f);
        }
        float value = settled.get();
        settled.update(1f / 14f);
        assertEquals(value, settled.get(), 1e-6f);
        assertTrue(value >= 0.5f && value <= 1.5f);
    }
}
