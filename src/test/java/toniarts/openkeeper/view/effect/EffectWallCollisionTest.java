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

import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EffectWallCollisionTest {
    @Test
    void reflectsVelocityAndRemainingMoveAtSolidTile() {
        Vector3f to = new Vector3f(1, 2, .2f);
        Vector3f velocity = new Vector3f(4, 0, 1);
        EffectWallCollision.reflect(new Vector3f(0, 2, 0), to, velocity, (x, z) -> x == 1);
        assertEquals(0, to.x, 1e-5);
        assertEquals(.2f, to.z, 1e-5);
        assertEquals(-4, velocity.x);
        assertEquals(2, to.y);
    }

    @Test
    void longMoveCannotTunnelThroughWallAndMayBounceMoreThanOnce() {
        Vector3f to = new Vector3f(5, 2, 0);
        Vector3f velocity = new Vector3f(10, 0, 0);
        EffectWallCollision.reflect(new Vector3f(0, 2, 0), to, velocity, (x, z) -> x < 0 || x > 1);
        assertEquals(1, to.x, 1e-4);
        assertEquals(10, velocity.x);
    }

    @Test
    void solidDiagonalCornerReflectsBothAxes() {
        Vector3f to = new Vector3f(1, 2, 1);
        Vector3f velocity = new Vector3f(4, 0, 4);
        EffectWallCollision.reflect(new Vector3f(0, 2, 0), to, velocity, (x, z) -> x == 1 && z == 1);
        assertEquals(0, to.x, 1e-5);
        assertEquals(0, to.z, 1e-5);
        assertEquals(-4, velocity.x);
        assertEquals(-4, velocity.z);
    }

    @Test
    void openMapPreservesTheMove() {
        Vector3f to = new Vector3f(3, 2, 4);
        Vector3f velocity = new Vector3f(3, 0, 4);
        EffectWallCollision.reflect(new Vector3f(0, 2, 0), to, velocity, (x, z) -> false);
        assertEquals(new Vector3f(3, 2, 4), to);
        assertEquals(new Vector3f(3, 0, 4), velocity);
    }
}
