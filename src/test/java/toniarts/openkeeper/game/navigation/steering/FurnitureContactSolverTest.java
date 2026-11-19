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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper. If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.game.navigation.steering;

import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FurnitureContactSolverTest {

    @Test
    void slowlyOutwardMotionStillReceivesContactSteering() {
        Vector2 velocity = new Vector2(.02f, 0);
        Vector2 force = new Vector2();
        solve(velocity, force, new Vector2(1, 0));
        assertEquals(new Vector2(), velocity);
        assertEquals(new Vector2(0, 1.5f), force);
        velocity.set(.06f, 0);
        force.setZero();
        solve(velocity, force, new Vector2(1, 0));
        assertEquals(.06f, velocity.x);
        assertTrue(force.isZero());
    }

    @Test
    void diagonalContactUsesSequentialComponentUpdates() {
        Vector2 velocity = new Vector2(-.1f, -.1f);
        Vector2 force = new Vector2(-1, -.5f);
        solve(velocity, force, new Vector2(.6f, .8f));
        assertEquals(-.016f, velocity.x, 1e-6f);
        assertEquals(-.02832f, velocity.y, 1e-6f);
        assertEquals(-1.6f, force.x, 1e-6f);
        assertEquals(.912f, force.y, 1e-6f);
    }

    @Test
    void reverseTraversalAndSignedRankingAffectMultipleContacts() {
        Vector2 velocity = new Vector2(-.1f, .1f);
        Vector2 force = new Vector2(-1, .5f);
        solve(velocity, force, new Vector2(1, 0), new Vector2(0, -1));
        assertEquals(new Vector2(-.1f, 0), velocity);
        assertEquals(new Vector2(-2.5f, 0), force);
        velocity.set(-.1f, .1f);
        force.set(-1, .5f);
        solve(velocity, force, new Vector2(0, -1), new Vector2(1, 0));
        assertTrue(velocity.isZero());
        assertEquals(new Vector2(1.5f, 1.5f), force);
    }

    private static void solve(Vector2 velocity, Vector2 force, Vector2... normals) {
        FurnitureContactSolver.solve(normals, normals.length, velocity, force, 1.5f, new Vector2());
    }
}
