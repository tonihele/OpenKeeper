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

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.util.function.Predicate;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Finds the map tile under a ray, i.e. under the cursor. The map has two
 * heights: blocks (rock, walls, and whatever the fog hides) reach up to
 * {@link WorldUtils#TOP_HEIGHT}, everything else is floor at
 * {@link WorldUtils#FLOOR_HEIGHT}. The ray picks the top of a block, the
 * side of a block, or the floor, whichever it reaches first.
 * <p>
 * Cutting the ray at a single height is not enough. At block height, a floor
 * tile resolves almost a whole tile too close to the camera, so the cursor on
 * a claimed tile picks the unclaimed one next to it.
 */
public final class TilePicker {

    private TilePicker() {
        // Nope
    }

    /**
     * What the ray hit
     *
     * @param tile the tile
     * @param position where on the map the ray hit it, in 2D (x, z)
     */
    public record Pick(Point tile, Vector2f position) {

    }

    /**
     * Picks the tile the ray hits first
     *
     * @param origin the ray origin, the camera
     * @param direction the ray direction, does not need to be normalized
     * @param isBlock whether a tile stands up to block height
     * @return the pick, {@code null} if the ray never comes down to the map
     */
    public static Pick pick(Vector3f origin, Vector3f direction, Predicate<Point> isBlock) {
        if (direction.y >= 0) {
            return null;
        }

        // Where the ray passes block height, and where it reaches the floor
        float tTop = Math.max(0, (WorldUtils.TOP_HEIGHT - origin.y) / direction.y);
        float tFloor = (WorldUtils.FLOOR_HEIGHT - origin.y) / direction.y;
        if (tFloor <= tTop) {
            return null;
        }
        float topX = origin.x + direction.x * tTop;
        float topZ = origin.z + direction.z * tTop;
        float floorX = origin.x + direction.x * tFloor;
        float floorZ = origin.z + direction.z * tFloor;

        // Walk the tiles the ray crosses on its way down. In tile space a tile spans [index, index + 1)
        float startU = topX / WorldUtils.TILE_WIDTH + 0.5f;
        float startV = topZ / WorldUtils.TILE_WIDTH + 0.5f;
        float deltaU = floorX / WorldUtils.TILE_WIDTH + 0.5f - startU;
        float deltaV = floorZ / WorldUtils.TILE_WIDTH + 0.5f - startV;

        int tileU = (int) Math.floor(startU);
        int tileV = (int) Math.floor(startV);
        int lastU = (int) Math.floor(startU + deltaU);
        int lastV = (int) Math.floor(startV + deltaV);
        int stepU = deltaU > 0 ? 1 : -1;
        int stepV = deltaV > 0 ? 1 : -1;

        // Ray fraction at which the next tile border is crossed, and between two borders
        float nextU = deltaU != 0 ? ((stepU > 0 ? tileU + 1 : tileU) - startU) / deltaU : Float.POSITIVE_INFINITY;
        float nextV = deltaV != 0 ? ((stepV > 0 ? tileV + 1 : tileV) - startV) / deltaV : Float.POSITIVE_INFINITY;
        float borderU = deltaU != 0 ? Math.abs(1 / deltaU) : Float.POSITIVE_INFINITY;
        float borderV = deltaV != 0 ? Math.abs(1 / deltaV) : Float.POSITIVE_INFINITY;

        float fraction = 0;
        int tilesLeft = Math.abs(lastU - tileU) + Math.abs(lastV - tileV);
        while (true) {
            Point tile = new Point(tileU, tileV);
            if (isBlock.test(tile)) {

                // The top of the block where the walk starts, otherwise the side the ray came in through
                return new Pick(tile, new Vector2f(topX + (floorX - topX) * fraction, topZ + (floorZ - topZ) * fraction));
            }
            if (tilesLeft-- <= 0) {
                return new Pick(tile, new Vector2f(floorX, floorZ));
            }

            if (nextU < nextV) {
                fraction = nextU;
                nextU += borderU;
                tileU += stepU;
            } else {
                fraction = nextV;
                nextV += borderV;
                tileV += stepV;
            }
        }
    }

}
