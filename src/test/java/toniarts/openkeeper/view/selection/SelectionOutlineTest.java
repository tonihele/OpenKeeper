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

import com.jme3.bounding.BoundingBox;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import java.nio.FloatBuffer;
import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectionOutlineTest {

    @Test
    void rendersSolidNondegenerateTriangles() {
        SelectionOutline mesh = new SelectionOutline(.51f, 1.01f, .51f);
        assertEquals(Mesh.Mode.Triangles, mesh.getMode());
        var indices = mesh.getIndexBuffer();
        FloatBuffer positions = mesh.getFloatBuffer(Type.Position);
        for (int i = 0; i < indices.size(); i += 3) {
            var a = vertex(positions, indices.get(i));
            var b = vertex(positions, indices.get(i + 1));
            var c = vertex(positions, indices.get(i + 2));
            assertTrue(b.subtract(a).cross(c.subtract(a)).lengthSquared() > 0);
        }
    }

    @Test
    void stretchingASelectionPreservesBeamThicknessAndUpdatesBounds() {
        SelectionOutline mesh = new SelectionOutline(.51f, 1.01f, .51f);
        float thickness = beamThickness(mesh);
        mesh.updatePositions(3.51f, 1.01f, 2.51f);
        assertEquals(thickness, beamThickness(mesh), 1e-6f);
        var bounds = (BoundingBox) mesh.getBound();
        assertEquals(3.51f + thickness / 2, bounds.getXExtent(), 1e-6f);
        assertEquals((1.01f + thickness / 2) / 2, bounds.getYExtent(), 1e-6f);
        assertEquals(2.51f + thickness / 2, bounds.getZExtent(), 1e-6f);
        assertEquals(new Vector3f(0, bounds.getYExtent(), 0), bounds.getCenter());
        mesh.updatePositions(.51f, 1.01f, .51f);
        assertEquals(.51f + thickness / 2, ((BoundingBox) mesh.getBound()).getXExtent(), 1e-6f);
    }

    @Test
    void hasNoGeometryBelowFloorThatCouldShowThroughLiquids() {
        SelectionOutline mesh = new SelectionOutline(.51f, 1.01f, .51f);
        mesh.updatePositions(2.51f, 1.01f, 3.51f);
        FloatBuffer positions = mesh.getFloatBuffer(Type.Position);
        for (int i = 1; i < positions.limit(); i += 3) {
            assertTrue(positions.get(i) >= 0, "Outline must end at floor level");
        }
        assertEquals(8 * 8, mesh.getVertexCount(), "Only the top rim and four uprights");
    }

    private static float beamThickness(SelectionOutline mesh) {
        FloatBuffer positions = mesh.getFloatBuffer(Type.Position);
        // First beam runs along X; its Y cross-section must stay narrow.
        float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
        for (int vertex = 0; vertex < 8; vertex++) {
            float y = positions.get(vertex * 3 + 1);
            min = Math.min(min, y);
            max = Math.max(max, y);
        }
        return max - min;
    }

    private static Vector3f vertex(FloatBuffer positions, int index) {
        return new Vector3f(positions.get(index * 3),
                positions.get(index * 3 + 1), positions.get(index * 3 + 2));
    }
}
