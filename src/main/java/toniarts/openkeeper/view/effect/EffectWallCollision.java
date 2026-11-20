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
import java.util.function.BiPredicate;

/** Sweeps point-like effect elements across tile boundaries and reflects at walls (DKII 0x4472F0). */
public final class EffectWallCollision {
    private EffectWallCollision() { }

    public static void reflect(Vector3f from, Vector3f to, Vector3f velocity,
            BiPredicate<Integer, Integer> solid) {
        boolean collided = false;
        float x = from.x;
        float z = from.z;
        float dx = to.x - x;
        float dz = to.z - z;
        // A move cannot cross more grid lines than its Manhattan length plus its two initial partial tiles.
        int crossings = 2 + (int) Math.ceil(Math.abs(dx)) + (int) Math.ceil(Math.abs(dz));
        for (int i = 0; i < crossings && (dx != 0 || dz != 0); i++) {
            int tileX = (int) Math.floor((double) x + .5);
            int tileZ = (int) Math.floor((double) z + .5);
            int sx = dx > 0 ? 1 : -1;
            int sz = dz > 0 ? 1 : -1;
            float tx = dx == 0 ? Float.POSITIVE_INFINITY : (tileX + sx * .5f - x) / dx;
            float tz = dz == 0 ? Float.POSITIVE_INFINITY : (tileZ + sz * .5f - z) / dz;
            float t = Math.min(tx, tz);
            if (t >= 1) {
                x += dx;
                z += dz;
                dx = dz = 0;
                break;
            }
            boolean crossX = tx <= tz;
            boolean crossZ = tz <= tx;
            boolean hitX = crossX && solid.test(tileX + sx, tileZ);
            boolean hitZ = crossZ && solid.test(tileX, tileZ + sz);
            if (crossX && crossZ && !hitX && !hitZ && solid.test(tileX + sx, tileZ + sz)) {
                hitX = hitZ = true;
            }
            x += dx * t;
            z += dz * t;
            dx *= 1 - t;
            dz *= 1 - t;
            collided |= hitX || hitZ;
            if (hitX) { dx = -dx; velocity.x = -velocity.x; }
            if (hitZ) { dz = -dz; velocity.z = -velocity.z; }
            // Enter the selected side of the boundary so the next sweep makes progress.
            if (crossX) x = Math.nextAfter(x, hitX ? x - sx : x + sx);
            if (crossZ) z = Math.nextAfter(z, hitZ ? z - sz : z + sz);
        }
        if (collided) {
            to.x = x + dx;
            to.z = z + dz;
        }
    }
}
