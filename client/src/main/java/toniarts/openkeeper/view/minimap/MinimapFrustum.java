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
import com.jme3.math.Plane;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * Clips a polygon's outline to a circle: every edge is cut off where it
     * crosses the circle's rim, and nothing is drawn outside it.
     *
     * @param xy the polygon's vertices as x,y pairs
     * @return line segments as x1,y1,x2,y2 groups (empty if the outline lies
     * entirely outside the circle)
     */
    static float[] clipToCircle(float[] xy, float cx, float cy, float radius) {
        int n = xy.length / 2;
        List<Float> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            float ax = xy[i * 2];
            float ay = xy[i * 2 + 1];
            float dx = xy[j * 2] - ax;
            float dy = xy[j * 2 + 1] - ay;

            // Solve |a + t*d - c|^2 = r^2 and keep the part of t in [0, 1] inside
            float fx = ax - cx;
            float fy = ay - cy;
            float qa = dx * dx + dy * dy;
            if (qa == 0f) {
                continue;
            }
            float qb = 2f * (fx * dx + fy * dy);
            float qc = fx * fx + fy * fy - radius * radius;
            float disc = qb * qb - 4f * qa * qc;
            if (disc <= 0f) {
                continue;
            }
            float root = FastMath.sqrt(disc);
            float t0 = Math.max(0f, (-qb - root) / (2f * qa));
            float t1 = Math.min(1f, (-qb + root) / (2f * qa));
            if (t0 >= t1) {
                continue;
            }
            out.add(ax + t0 * dx);
            out.add(ay + t0 * dy);
            out.add(ax + t1 * dx);
            out.add(ay + t1 * dy);
        }

        float[] flat = new float[out.size()];
        for (int i = 0; i < flat.length; i++) {
            flat[i] = out.get(i);
        }
        return flat;
    }

}
