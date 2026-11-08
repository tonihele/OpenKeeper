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

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * The top rim and four uprights, drawn as narrow solid beams. Unlike
 * wide GL lines, triangles retain their width in OpenGL core profiles.
 */
final class SelectionOutline extends Mesh {

    // Visually fitted against DK II at matching tile size. Keep the thickness in
    // world units so it follows camera zoom and viewport resolution with the box.
    private static final float HALF_THICKNESS = WorldUtils.TILE_WIDTH / 64f;
    private static final int[] BOX_INDICES = {
        0, 2, 3, 0, 3, 1, 4, 5, 7, 4, 7, 6,
        0, 1, 5, 0, 5, 4, 2, 6, 7, 2, 7, 3,
        0, 4, 6, 0, 6, 2, 1, 3, 7, 1, 7, 5
    };
    private static final int EDGE_COUNT = 8;
    private final FloatBuffer positions = BufferUtils.createFloatBuffer(EDGE_COUNT * 8 * 3);

    SelectionOutline(float xExtent, float yExtent, float zExtent) {
        short[] indices = new short[EDGE_COUNT * BOX_INDICES.length];
        for (int edge = 0; edge < EDGE_COUNT; edge++) {
            for (int i = 0; i < BOX_INDICES.length; i++) {
                indices[edge * BOX_INDICES.length + i] = (short) (edge * 8 + BOX_INDICES[i]);
            }
        }
        setBuffer(Type.Index, 3, indices);
        setBuffer(Type.Position, 3, positions);
        setDynamic();
        updatePositions(xExtent, yExtent, zExtent);
    }

    void updatePositions(float xExtent, float yExtent, float zExtent) {
        float[] extents = {xExtent, yExtent, zExtent};
        positions.clear();
        for (int axis = 0; axis < 3; axis++) {
            int across = (axis + 1) % 3;
            for (int edge = 0; edge < 4; edge++) {
                // No underground rim: liquid surfaces may blend over geometry
                // below them instead of hiding it. Uprights end at floor level.
                int yCorner = across == 1 ? edge & 1 : (edge >> 1) & 1;
                if (axis != 1 && yCorner == 0) {
                    continue;
                }
                for (int vertex = 0; vertex < 8; vertex++) {
                    for (int dimension = 0; dimension < 3; dimension++) {
                        float side = (vertex & (1 << dimension)) == 0 ? -1 : 1;
                        if (dimension == axis) {
                            positions.put(dimension == 1 && side < 0 ? 0
                                    : side * (extents[dimension] + HALF_THICKNESS));
                        } else {
                            int corner = dimension == across ? edge & 1 : (edge >> 1) & 1;
                            float center = (corner == 0 ? -1 : 1) * extents[dimension];
                            positions.put(center + side * HALF_THICKNESS);
                        }
                    }
                }
            }
        }
        positions.flip();
        getBuffer(Type.Position).updateData(positions);
        updateBound();
    }
}
