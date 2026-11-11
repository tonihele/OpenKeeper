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
package toniarts.openkeeper.view.map;

import com.jme3.math.FastMath;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import java.nio.FloatBuffer;

/** Geometry utilities for decorative room pillars. */
public final class PillarGeometry {

    private PillarGeometry() {
    }

    /**
     * Moves a pillar so the lowest point of its loaded model meets the given
     * world-space floor height. DK II models use different local origins, so
     * object position alone does not describe where their bases sit.
     *
     * @param pillar the root spatial of the loaded pillar model
     * @param floorY the world-space floor height
     */
    public static void alignBaseToFloor(Spatial pillar, float floorY) {
        pillar.updateGeometricState();
        float minimumY = minimumY(pillar, Float.POSITIVE_INFINITY);
        if (!Float.isFinite(minimumY)) {
            return;
        }

        float offset = floorY - minimumY;
        if (Math.abs(offset) > FastMath.ZERO_TOLERANCE) {
            Vector3f translation = pillar.getLocalTranslation().clone();
            translation.y += offset;
            pillar.setLocalTranslation(translation);
            pillar.updateGeometricState();
        }
    }

    private static float minimumY(Spatial spatial, float minimum) {
        if (spatial instanceof Geometry geometry) {
            Mesh mesh = geometry.getMesh();
            VertexBuffer positionBuffer = mesh.getBuffer(Type.Position);
            if (positionBuffer == null || positionBuffer.getFormat() != VertexBuffer.Format.Float
                    || positionBuffer.getNumComponents() != 3) {
                return minimum;
            }

            Transform toWorld = geometry.getLocalTransform().clone();
            for (Spatial parent = geometry.getParent(); parent != null; parent = parent.getParent()) {
                toWorld.combineWithParent(parent.getLocalTransform());
            }
            FloatBuffer positions = (FloatBuffer) positionBuffer.getData();
            Vector3f local = new Vector3f();
            Vector3f world = new Vector3f();
            for (int i = 0; i + 2 < positions.limit(); i += 3) {
                local.set(positions.get(i), positions.get(i + 1), positions.get(i + 2));
                toWorld.transformVector(local, world);
                minimum = Math.min(minimum, world.y);
            }
        } else if (spatial instanceof Node node) {
            for (Spatial child : node.getChildren()) {
                minimum = minimumY(child, minimum);
            }
        }
        return minimum;
    }
}
