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

/**
 * Builds the octagon {@code Mesh} that displays the minimap raster
 * (minimap_design.md §5.7/§5.10): 8 vertices, 6 triangles, indices
 * {@code (0,1,7) (1,6,7) (1,2,6) (2,5,6) (2,4,5) (2,3,4)}.
 */
public final class MinimapOctagon {

    /**
     * The 8 corner angles, evenly spaced
     */
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
        float[] uvs = new float[VERTEX_COUNT * 2];
        for (int k = 0; k < VERTEX_COUNT; k++) {
            float angle = k * FastMath.PI / 4f;
            float cos = FastMath.cos(angle);
            float sin = FastMath.sin(angle);

            positions[k * 3] = centerX + radius * cos;
            positions[k * 3 + 1] = centerY + radius * sin;
            positions[k * 3 + 2] = 0f;

            // V only is flipped relative to the position's own angle -
            // confirmed against the running game across two rounds: a
            // first attempt that negated both cos and sin (a full 180
            // degree rotation) fixed the vertical mirror but introduced an
            // unwanted horizontal one, so only sin (V) is negated here.
            // Cause: the raster buffer is filled row 0 = top of the logical
            // image, but V=0 samples the bottom of the texture in this
            // engine's convention - a pure top/bottom mirror, not a
            // rotation.
            uvs[k * 2] = 0.5f + 0.5f * cos;
            uvs[k * 2 + 1] = 0.5f - 0.5f * sin;
        }

        Mesh mesh = new Mesh();
        mesh.setBuffer(Type.Position, 3, positions);
        mesh.setBuffer(Type.TexCoord, 2, uvs);
        mesh.setBuffer(Type.Index, 3, INDICES);
        mesh.setMode(Mesh.Mode.Triangles);
        mesh.updateBound();
        mesh.updateCounts();
        return mesh;
    }

}
