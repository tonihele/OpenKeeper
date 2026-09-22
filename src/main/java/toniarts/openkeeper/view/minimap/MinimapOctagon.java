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
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import java.nio.FloatBuffer;

/**
 * Builds and updates the octagon {@code Mesh} that displays the minimap
 * raster (minimap_design.md §5.7/§5.10): 8 vertices, 6 triangles, indices
 * {@code (0,1,7) (1,6,7) (1,2,6) (2,5,6) (2,4,5) (2,3,4)}.
 *
 * <p>
 * Vertex <b>positions</b> are a regular octagon inscribed in a circle,
 * fixed on screen, {@code z = 0}, built once by {@link #build}. Vertex
 * <b>texture coordinates</b> rotate with the camera's yaw (design §5.7) -
 * {@link #updateUv} rewrites them in place every frame (design §5.10:
 * "write into the existing FloatBuffer and call
 * {@code mesh.getBuffer(Type.TexCoord).updateData}"), independent of the
 * raster rebuild cadence, since the camera can turn between raster
 * rebuilds.
 *
 * <p>
 * <b>The exact per-vertex corner angle design §5.7 specifies
 * (θ ± 0.4266 rad, and that pattern repeated at the ±π/2/π offsets) is not
 * used here.</b> That table is explicitly the design doc's own best guess,
 * pending verification against the original (§9 item 4), and - unlike the
 * identity-UV case Step 4 shipped - adopting it now would also change the
 * octagon's shape at yaw zero, which has already been confirmed correct
 * against the running game twice (an upside-down bug, then a left-right
 * one, both fixed). To avoid changing two unverified things (the corner
 * angles *and* the rotation) in the same untested step, this keeps the
 * already-confirmed evenly-spaced 8 angles and only adds the yaw rotation
 * on top of them. If the verify-in-game checkpoint for rotation
 * (minimap_jmonkey.md Step 6 item 4) shows the octagon reads as "too
 * round" compared to the original's more angular shape, switching to
 * design's asymmetric table is the follow-up.
 */
public final class MinimapOctagon {

    private static final int VERTEX_COUNT = 8;

    private static final short[] INDICES = {
        0, 1, 7,
        1, 6, 7,
        1, 2, 6,
        2, 5, 6,
        2, 4, 5,
        2, 3, 4
    };

    private MinimapOctagon() {
    }

    /**
     * @param centerX, centerY, radius the on-screen circle the 8 vertices
     * are inscribed in, in the same local coordinate space the mesh's
     * material will later sample the raster texture in
     */
    public static Mesh build(float centerX, float centerY, float radius) {
        float[] positions = new float[VERTEX_COUNT * 3];
        for (int k = 0; k < VERTEX_COUNT; k++) {
            float angle = cornerAngle(k);
            positions[k * 3] = centerX + radius * FastMath.cos(angle);
            positions[k * 3 + 1] = centerY + radius * FastMath.sin(angle);
            positions[k * 3 + 2] = 0f;
        }

        Mesh mesh = new Mesh();
        mesh.setBuffer(Type.Position, 3, positions);
        mesh.setBuffer(Type.TexCoord, 2, new float[VERTEX_COUNT * 2]);
        mesh.setBuffer(Type.Index, 3, INDICES);
        mesh.setMode(Mesh.Mode.Triangles);
        mesh.updateBound();
        mesh.updateCounts();

        updateUv(mesh, 0f);
        return mesh;
    }

    /**
     * Rewrites the mesh's texture coordinates for the given camera yaw.
     * Positions and indices are untouched.
     *
     * @param yawRadians see {@link MinimapCoordinates#cameraYawRadians} -
     * a continuous float standing in for the original's integer 0..2047
     * yaw; sign/zero convention unverified (design §9 item 4/5)
     */
    public static void updateUv(Mesh mesh, float yawRadians) {
        FloatBuffer uvBuffer = (FloatBuffer) mesh.getBuffer(Type.TexCoord).getData();
        uvBuffer.rewind();
        for (int k = 0; k < VERTEX_COUNT; k++) {
            // V only is negated relative to cornerAngle's own cos/sin -
            // confirmed against the running game for the yaw=0 case (see
            // this class's own history): only the vertical axis needed
            // flipping, not the horizontal one.
            float phi = cornerAngle(k) + yawRadians;
            uvBuffer.put(0.5f + 0.5f * FastMath.cos(phi));
            uvBuffer.put(0.5f - 0.5f * FastMath.sin(phi));
        }
        uvBuffer.rewind();
        mesh.getBuffer(Type.TexCoord).updateData(uvBuffer);
    }

    private static float cornerAngle(int k) {
        return k * FastMath.PI / 4f;
    }

}
