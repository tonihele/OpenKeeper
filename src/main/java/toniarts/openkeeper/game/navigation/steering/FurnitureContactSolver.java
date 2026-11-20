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

/** Contact stage, expressed in displacement and acceleration per movement update. */
final class FurnitureContactSolver {

    static final int MAX_CONTACTS = 12;
    private static final float OUTWARD_CUTOFF = 0xccc / 65536f;

    private FurnitureContactSolver() {
    }

    static void solve(Vector2[] normals, int count, Vector2 velocity, Vector2 force,
            float maxAcceleration, Vector2 sideways) {
        sideways.setZero();
        int bestRank = 0;
        // DKII consumes its bounded contact buffer from the end.
        for (int i = count - 1; i >= 0; i--) {
            Vector2 normal = normals[i];
            float nx = normal.x;
            float ny = normal.y;
            // Preserve the native signed >> 18 ranking of 16.16 normals. For
            // unit normals this is the number of negative components, not len2.
            int rank = (nx < 0 ? 1 : 0) + (ny < 0 ? 1 : 0);
            if (nx * (velocity.x + force.x) + ny * (velocity.y + force.y) >= OUTWARD_CUTOFF
                    || rank < bestRank) {
                continue;
            }
            bestRank = rank;
            float xx = 1 - nx * nx;
            float yy = 1 - ny * ny;
            float cross = nx * ny;
            // The second component uses the already updated first component.
            // A simultaneous orthogonal projection changes diagonal passages.
            force.x = xx * force.x - cross * force.y;
            force.y = yy * force.y - cross * force.x;
            velocity.x = xx * velocity.x - cross * velocity.y;
            velocity.y = yy * velocity.y - cross * velocity.x;
            float tx = -ny;
            float ty = nx;
            if (tx * (velocity.x + force.x) + ty * (velocity.y + force.y) < 0) {
                tx = -tx;
                ty = -ty;
            }
            sideways.add(tx * maxAcceleration, ty * maxAcceleration);
        }
        // Tangents do not affect the choice made for subsequent contacts.
        force.add(sideways);
    }
}
