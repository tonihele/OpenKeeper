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
package toniarts.openkeeper.view.minimap;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Pure world-unit &lt;-&gt; tile-unit and camera-yaw helpers shared by the
 * minimap. The original engine does this arithmetic in 16.16 fixed point
 * over sub-tile (4096/tile) coordinates and an integer 0..2047 yaw;
 */
public final class MinimapCoordinates {

    private MinimapCoordinates() {
    }

    /**
     * World position (y ignored) to fractional tile-space coordinates, e.g.
     * (12.5, 7.25) for a position a quarter-tile into tile (12, 7).
     */
    public static Vector2f worldToTile(Vector3f worldPosition) {
        return new Vector2f(worldPosition.x / WorldUtils.TILE_WIDTH, worldPosition.z / WorldUtils.TILE_WIDTH);
    }

    /**
     * @see #worldToTile(Vector3f)
     */
    public static Vector2f worldToTile(Vector2f worldPositionXz) {
        return new Vector2f(worldPositionXz.x / WorldUtils.TILE_WIDTH, worldPositionXz.y / WorldUtils.TILE_WIDTH);
    }

    /**
     * Continuous camera yaw in radians, standing in for the original
     * engine's integer 0..2047 turn value. Derived from the camera's XZ
     * forward direction.
     *
     * <p>
     * The {@code + PI} was added after confirming against the running game
     * (minimap_jmonkey.md Step 6) that the plain {@code atan2} value put
     * the minimap content 180 degrees off from the camera's actual facing -
     * this engine's default camera direction (a level's initial preset
     * angle) apparently sits opposite {@code atan2}'s own zero reference.
     * The turning *sense* (which way the octagon UVs move as the camera
     * turns) is still unverified against the original - design §9 item
     * 4/5 - only this static zero-point offset has been checked so far.
     */
    public static float cameraYawRadians(Camera camera) {
        Vector3f direction = camera.getDirection();
        return FastMath.atan2(direction.x, direction.z) + FastMath.PI;
    }

}
