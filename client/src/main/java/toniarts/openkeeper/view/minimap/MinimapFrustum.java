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

import com.jme3.math.Plane;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

/**
 * The camera's view-frustum corners on the world ground plane
 */
public final class MinimapFrustum {

    private static final Plane GROUND_PLANE = new Plane(Vector3f.UNIT_Y, 0f);

    private MinimapFrustum() {
    }

    /**
     * @return the 4 ground-plane intersection points for the camera's
     * near/far ray through each screen corner, in screen order
     * (bottom-left, bottom-right, top-right, top-left - jME's pixel-space
     * Y convention). An entry is {@code null} if that corner's ray doesn't
     * hit the ground plane (e.g. the camera looking above the horizon).
     */
    public static Vector3f[] groundCorners(Camera camera) {
        Vector2f[] screenCorners = {
            new Vector2f(0, 0),
            new Vector2f(camera.getWidth(), 0),
            new Vector2f(camera.getWidth(), camera.getHeight()),
            new Vector2f(0, camera.getHeight())
        };

        Vector3f[] corners = new Vector3f[4];
        for (int i = 0; i < screenCorners.length; i++) {
            Vector3f near = camera.getWorldCoordinates(screenCorners[i], 0f);
            Vector3f far = camera.getWorldCoordinates(screenCorners[i], 1f);
            Ray ray = new Ray(near, far.subtractLocal(near).normalizeLocal());
            Vector3f hit = new Vector3f();
            corners[i] = ray.intersectsWherePlane(GROUND_PLANE, hit) ? hit : null;
        }
        return corners;
    }

}
