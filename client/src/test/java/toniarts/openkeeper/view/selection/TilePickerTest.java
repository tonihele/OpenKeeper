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
package toniarts.openkeeper.view.selection;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TilePickerTest {

    /**
     * The game camera looks down 51.3 degrees, at a diagonal like the
     * original
     */
    private static final float PITCH = 51.3f * FastMath.DEG_TO_RAD;
    private static final float YAW = 45f * FastMath.DEG_TO_RAD;
    private static final float CAMERA_HEIGHT = 10f;
    private static final float EPSILON = 1e-4f;

    private static final Predicate<Point> ALL_FLOOR = tile -> false;

    @Test
    void floorTileUnderTheCursorIsPicked() {
        Vector3f target = new Vector3f(5.3f, WorldUtils.FLOOR_HEIGHT, 7.2f);

        TilePicker.Pick pick = pickAt(target, ALL_FLOOR);

        assertEquals(new Point(5, 7), pick.tile());
        assertEquals(target.x, pick.position().x, EPSILON);
        assertEquals(target.z, pick.position().y, EPSILON);
    }

    @Test
    void wallTopPlaneWouldHavePickedTheNeighbour() {

        // The old way: cut the ray at wall height, also over the floor. Even the very centre of a tile
        // came out as the neighbour towards the camera
        Vector3f target = new Vector3f(5f, WorldUtils.FLOOR_HEIGHT, 7f);
        Vector3f direction = viewDirection();
        Vector3f origin = target.subtract(direction.mult((CAMERA_HEIGHT - target.y) / -direction.y));
        Vector3f atWallHeight = origin.add(direction.mult((WorldUtils.TOP_HEIGHT - origin.y) / direction.y));

        assertNotEquals(new Point(5, 7), WorldUtils.vectorToPoint(atWallHeight));
        assertEquals(new Point(5, 7), pickAt(target, ALL_FLOOR).tile());
    }

    @Test
    void wallTopIsPicked() {
        Point wall = new Point(5, 7);
        Vector3f target = new Vector3f(5.1f, WorldUtils.TOP_HEIGHT, 6.9f);

        TilePicker.Pick pick = pickAt(target, Set.of(wall)::contains);

        assertEquals(wall, pick.tile());
        assertEquals(target.x, pick.position().x, EPSILON);
        assertEquals(target.z, pick.position().y, EPSILON);
    }

    @Test
    void wallInFrontOfTheFloorIsPickedOnItsSide() {
        Vector3f target = new Vector3f(5.3f, WorldUtils.FLOOR_HEIGHT, 7.2f);
        Vector3f direction = viewDirection();

        // The tile the ray passes over at wall height, on its way down to the target
        Vector3f origin = target.subtract(direction.mult((CAMERA_HEIGHT - target.y) / -direction.y));
        Point inFront = WorldUtils.vectorToPoint(origin.add(direction.mult((WorldUtils.TOP_HEIGHT - origin.y) / direction.y)));

        TilePicker.Pick pick = pickAt(target, Set.of(inFront)::contains);

        assertEquals(inFront, pick.tile());
    }

    @Test
    void wallBehindTheFloorDoesNotMatter() {
        Vector3f target = new Vector3f(5.3f, WorldUtils.FLOOR_HEIGHT, 7.2f);

        TilePicker.Pick pick = pickAt(target, Set.of(new Point(6, 8), new Point(7, 9))::contains);

        assertEquals(new Point(5, 7), pick.tile());
    }

    @Test
    void lookingStraightDownPicksTheTileBelow() {
        Vector3f origin = new Vector3f(3.4f, CAMERA_HEIGHT, -2.4f);
        Vector3f down = new Vector3f(0, -1, 0);

        assertEquals(new Point(3, -2), TilePicker.pick(origin, down, ALL_FLOOR).tile());
        assertEquals(new Point(3, -2), TilePicker.pick(origin, down, tile -> true).tile());
    }

    @Test
    void rayThatNeverComesDownPicksNothing() {
        assertNull(TilePicker.pick(new Vector3f(0, CAMERA_HEIGHT, 0), new Vector3f(1, 0.1f, 0), ALL_FLOOR));
        assertNull(TilePicker.pick(new Vector3f(0, CAMERA_HEIGHT, 0), new Vector3f(1, 0, 0), ALL_FLOOR));
    }

    @Test
    void everyFloorPointPicksItsOwnTile() {
        for (float x = 0.05f; x < 3; x += 0.1f) {
            for (float z = 0.05f; z < 3; z += 0.1f) {
                Vector3f target = new Vector3f(x, WorldUtils.FLOOR_HEIGHT, z);

                assertEquals(WorldUtils.vectorToPoint(target), pickAt(target, ALL_FLOOR).tile(), "At " + target);
            }
        }
    }

    private static TilePicker.Pick pickAt(Vector3f target, Predicate<Point> isBlock) {
        Vector3f direction = viewDirection();
        Vector3f origin = target.subtract(direction.mult((CAMERA_HEIGHT - target.y) / -direction.y));
        return TilePicker.pick(origin, direction, isBlock);
    }

    private static Vector3f viewDirection() {
        float horizontal = FastMath.cos(PITCH);
        return new Vector3f(horizontal * FastMath.cos(YAW), -FastMath.sin(PITCH), horizontal * FastMath.sin(YAW));
    }

}
