/*
 * Copyright (C) 2026 OpenKeeper
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
package toniarts.openkeeper.view.effect;

import com.jme3.math.FastMath;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WhirlpoolMotionTest {
    @Test
    void nativeSquaredDistanceControlsClockwiseImpulse() {
        // At .25 tile: native offset 1024, >>5 = 32; denominator 32^2 + 128.
        float angle = (int) (32 * 16 * 128 / (32 * 32 + 128)) * FastMath.TWO_PI / 2048;
        var delta = EffectControl.whirlpoolDelta(32, .25f, 0, .05f);
        assertEquals(.25f * FastMath.cos(angle) - .25f, delta.x, 1e-6);
        assertEquals(-.25f * FastMath.sin(angle), delta.z, 1e-6);
        var reverse = EffectControl.whirlpoolDelta(224, .25f, 0, .05f); // signed -32
        assertEquals(delta.x, reverse.x, 1e-6);
        assertEquals(-delta.z, reverse.z, 1e-6);
    }

    @Test
    void zeroRateAndDistantParticlesReceiveNoImpulse() {
        assertEquals(0, EffectControl.whirlpoolDelta(0, .25f, 0, .05f).length(), 1e-6);
        assertEquals(0, EffectControl.whirlpoolDelta(1, 10, 0, .05f).length(), 1e-6);
    }
}
