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
import java.util.function.Predicate;

/** Tile access and recovery after regular creature steering has been integrated. */
final class CreatureMovementCollision {

    private final Vector2 candidate = new Vector2();

    void move(Vector2 position, Vector2 velocity, float tpf, Predicate<Vector2> accessible) {
        float x = position.x;
        float y = position.y;
        float dx = velocity.x * tpf;
        float dy = velocity.y * tpf;
        if (accessible.test(candidate.set(x + dx, y + dy))) {
            position.set(candidate);
            return;
        }
        // DKII accepts the first successful axis, rather than trying a second
        // move from the newly changed position in the same update.
        if (accessible.test(candidate.set(x + dx, y))) {
            position.set(candidate);
            velocity.y = 0;
            return;
        }
        if (accessible.test(candidate.set(x, y + dy))) {
            position.set(candidate);
            velocity.x = 0;
            return;
        }
        int tileX = tile(x);
        int tileY = tile(y);
        int tileDx = tile(x + dx) - tileX;
        int tileDy = tile(y + dy) - tileY;
        // Native recovery probes rotated tile offsets before rotating the
        // displacement. Those can identify different tiles near a corner, so
        // also validate the actual landing position with the same access rules.
        if (accessible.test(candidate.set(tileX - tileDy, tileY + tileDx))
                && accessible.test(candidate.set(x - dy, y + dx))) {
            position.set(candidate);
            return;
        }
        if (accessible.test(candidate.set(tileX + tileDy, tileY - tileDx))
                && accessible.test(candidate.set(x + dy, y - dx))) {
            position.set(candidate);
        }
        // As in DKII, a perpendicular recovery or fully blocked move retains
        // velocity. The next steering update decides how to proceed.
    }

    private static int tile(float coordinate) {
        return (int) Math.floor(coordinate + 0.5f);
    }
}
