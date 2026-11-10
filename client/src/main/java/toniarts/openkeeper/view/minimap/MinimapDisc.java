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
 * Builds and updates the disc {@code Mesh} that displays the minimap
 * raster: a centre vertex plus {@link #SEGMENTS} perimeter vertices,
 * triangulated as a fan
 *
 * <p>
 * Vertex <b>positions</b> are fixed on screen, {@code z = 0}, built once by
 * {@link #build}. Vertex <b>texture coordinates</b> rotate with the
 * camera's yaw every frame, independent of the
 * raster rebuild cadence, since the camera can turn between raster
 * rebuilds.
 */
public final class MinimapDisc {

    /**
     * Perimeter vertex count. Smooth at typical HUD panel sizes; cheap to
     * render for a mesh this small.
     */
    private static final int SEGMENTS = 32;

    private static final int VERTEX_COUNT = SEGMENTS + 1; // + the centre vertex
    private static final int CENTRE_INDEX = 0;

    private static final short[] INDICES = buildFanIndices();

    private MinimapDisc() {
    }

    private static short[] buildFanIndices() {
        short[] indices = new short[SEGMENTS * 3];
        for (int i = 0; i < SEGMENTS; i++) {
            int next = (i + 1) % SEGMENTS;
            indices[i * 3] = CENTRE_INDEX;
            indices[i * 3 + 1] = (short) (1 + i);
            indices[i * 3 + 2] = (short) (1 + next);
        }
        return indices;
    }

    /**
     * @param centerX, centerY, radius the on-screen circle the perimeter
     * vertices are inscribed in, in the same local coordinate space the
     * mesh's material will later sample the raster texture in
     */
    public static Mesh build(float centerX, float centerY, float radius) {
        float[] positions = new float[VERTEX_COUNT * 3];
        positions[CENTRE_INDEX * 3] = centerX;
        positions[CENTRE_INDEX * 3 + 1] = centerY;
        positions[CENTRE_INDEX * 3 + 2] = 0f;
        for (int k = 0; k < SEGMENTS; k++) {
            float angle = perimeterAngle(k);
            int i = (1 + k) * 3;
            positions[i] = centerX + radius * FastMath.cos(angle);
            positions[i + 1] = centerY + radius * FastMath.sin(angle);
            positions[i + 2] = 0f;
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
     * yaw
     */
    public static void updateUv(Mesh mesh, float yawRadians) {
        FloatBuffer uvBuffer = (FloatBuffer) mesh.getBuffer(Type.TexCoord).getData();
        uvBuffer.rewind();
        // The centre always samples the texture's own centre, regardless
        // of yaw - rotating around the exact centre point doesn't change
        // what's sampled there.
        uvBuffer.put(0.5f);
        uvBuffer.put(0.5f);
        for (int k = 0; k < SEGMENTS; k++) {
            // V only is negated relative to perimeterAngle's own cos/sin -
            // confirmed against the running game back when this was an
            // octagon: only the vertical axis needed flipping, not the
            // horizontal one.
            float phi = perimeterAngle(k) + yawRadians;
            uvBuffer.put(0.5f + 0.5f * FastMath.cos(phi));
            uvBuffer.put(0.5f - 0.5f * FastMath.sin(phi));
        }
        uvBuffer.rewind();
        mesh.getBuffer(Type.TexCoord).updateData(uvBuffer);
    }

    private static float perimeterAngle(int k) {
        return k * FastMath.TWO_PI / SEGMENTS;
    }

}
