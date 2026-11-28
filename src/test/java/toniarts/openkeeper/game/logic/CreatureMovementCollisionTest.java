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
package toniarts.openkeeper.game.logic;

import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.utils.Point;
import static org.junit.jupiter.api.Assertions.*;

class CreatureMovementCollisionTest {
    private final CreatureMovementCollision collision = new CreatureMovementCollision();
    private final Vector2 position = new Vector2(10.4f, 10.4f);
    private final Vector2 velocity = new Vector2(.2f, .3f);

    @Test
    void unobstructedMovementKeepsBothVelocityComponents() {
        collision.move(position, velocity, 1, ignored -> true);
        assertVector(10.6f, 10.7f, position);
        assertVector(.2f, .3f, velocity);
    }

    @Test
    void acceptsTheFirstAccessibleAxisWithoutASecondMove() {
        List<Vector2> queried = new ArrayList<>();
        collision.move(position, velocity, 1, candidate -> {
            queried.add(candidate.cpy());
            return queried.size() > 1;
        });
        assertEquals(2, queried.size());
        assertVector(10.6f, 10.4f, position);
        assertVector(.2f, 0, velocity);
    }

    @Test
    void triesTheOtherAxisWhenTheFirstIsBlocked() {
        collision.move(position, velocity, 1, candidate -> Math.round(candidate.x) == 10);
        assertVector(10.4f, 10.7f, position);
        assertVector(0, .3f, velocity);
    }

    @Test
    void recoversPerpendicularlyAndRetainsTheOriginalVelocity() {
        position.set(10.49f, 10.1f);
        velocity.set(.02f, .5f);
        collision.move(position, velocity, 1, candidate -> !blockedCorner().contains(tile(candidate)));
        assertVector(9.99f, 10.12f, position);
        assertVector(.02f, .5f, velocity);
    }

    @Test
    void triesTheSecondPerpendicularWhenTheFirstProbeIsBlocked() {
        position.set(10.1f, 10.49f);
        velocity.set(.5f, .02f);
        collision.move(position, velocity, 1, candidate -> !blockedCorner().contains(tile(candidate))
                && !tile(candidate).equals(new Point(9, 11)));
        assertVector(10.12f, 9.99f, position);
        assertVector(.5f, .02f, velocity);
    }

    @Test
    void openPerpendicularProbeDoesNotPermitABlockedLanding() {
        collision.move(position, velocity, 1, candidate -> !blockedCorner().contains(tile(candidate)));
        // Both native tile probes are open, but their rotated displacements
        // would land in one of the blocked adjacent tiles.
        assertVector(10.4f, 10.4f, position);
        assertVector(.2f, .3f, velocity);
    }

    @Test
    void enclosureKeepsPositionAndVelocityForTheNextSteeringUpdate() {
        collision.move(position, velocity, 1, candidate -> tile(candidate).equals(new Point(10, 10)));
        assertVector(10.4f, 10.4f, position);
        assertVector(.2f, .3f, velocity);
    }

    private static Set<Point> blockedCorner() {
        return Set.of(new Point(11, 11), new Point(11, 10), new Point(10, 11));
    }

    private static Point tile(Vector2 position) {
        return new Point(Math.round(position.x), Math.round(position.y));
    }

    private static void assertVector(float x, float y, Vector2 actual) {
        assertEquals(x, actual.x, 1e-5f);
        assertEquals(y, actual.y, 1e-5f);
    }
}
