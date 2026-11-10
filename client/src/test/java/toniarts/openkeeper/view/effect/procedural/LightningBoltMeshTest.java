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
package toniarts.openkeeper.view.effect.procedural;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.Vector3f;
import com.jme3.scene.VertexBuffer;
import java.nio.FloatBuffer;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LightningBoltMeshTest {

    private static final int SEGMENTS = 12;
    private static final float HEIGHT = 3f;
    private static final float JITTER = 0.2f;
    private static final float EPSILON = 1e-5f;
    private static final int LAYERS = LightningBoltMesh.LAYER_HALF_WIDTHS.length;

    @Test
    void boltStrikesTheOriginAndStaysWithinItsJitter() {
        LightningBoltMesh mesh = new LightningBoltMesh(SEGMENTS, HEIGHT, JITTER);
        Random random = new Random(42);

        for (int roll = 0; roll < 100; roll++) {
            mesh.randomizePath(random);

            assertEquals(Vector3f.ZERO, mesh.getPoint(0));
            assertEquals(HEIGHT, mesh.getPoint(SEGMENTS).y, EPSILON);
            for (int i = 0; i < mesh.getPointCount(); i++) {
                Vector3f point = mesh.getPoint(i);
                assertTrue(Math.abs(point.x) <= JITTER, "Point " + i + " wandered off: " + point);
                assertEquals(0f, point.z, "The bolt stays in its plane");
                assertEquals(HEIGHT * i / SEGMENTS, point.y, EPSILON);
            }
        }
    }

    @Test
    void ribbonsSpreadAlongX() {
        LightningBoltMesh mesh = new LightningBoltMesh(SEGMENTS, HEIGHT, JITTER);

        // The billboard turns the bolt's plane to the camera, so the ribbons only need to spread within it
        mesh.randomizePath(new Random(3));

        FloatBuffer positions = (FloatBuffer) mesh.getBuffer(VertexBuffer.Type.Position).getData();
        for (int layer = 0; layer < LAYERS; layer++) {
            float halfWidth = LightningBoltMesh.LAYER_HALF_WIDTHS[layer];
            for (int i = 0; i < mesh.getPointCount(); i++) {
                int first = (layer * mesh.getPointCount() + i) * LightningBoltMesh.VERTICES_ACROSS;
                Vector3f edge = getVertex(positions, first);
                Vector3f centre = getVertex(positions, first + 1);
                Vector3f otherEdge = getVertex(positions, first + 2);

                assertEquals(mesh.getPoint(i), centre);
                assertEquals(new Vector3f(centre.x - halfWidth, centre.y, centre.z), edge);
                assertEquals(new Vector3f(centre.x + halfWidth, centre.y, centre.z), otherEdge);
            }
        }
    }

    @Test
    void onlyTheCentreOfARibbonShines() {
        LightningBoltMesh mesh = new LightningBoltMesh(SEGMENTS, HEIGHT, JITTER);

        mesh.setIntensity(0.5f);

        FloatBuffer colors = (FloatBuffer) mesh.getBuffer(VertexBuffer.Type.Color).getData();
        for (int vertex = 0; vertex < mesh.getVertexCount(); vertex++) {
            float alpha = getAlpha(colors, vertex);
            if (vertex % LightningBoltMesh.VERTICES_ACROSS == 1) {
                assertTrue(alpha >= 0f && alpha <= 0.5f, "Centre vertex " + vertex + " alpha " + alpha);
            } else {
                assertEquals(0f, alpha, "Edge vertex " + vertex);
            }
        }
    }

    @Test
    void boltStrikesBrightAndFadesOutAtTheTop() {
        LightningBoltMesh mesh = new LightningBoltMesh(SEGMENTS, HEIGHT, JITTER);

        mesh.setIntensity(1f);

        // The core layer comes first, its centre vertex of a point is the middle one of three
        FloatBuffer colors = (FloatBuffer) mesh.getBuffer(VertexBuffer.Type.Color).getData();
        float strike = getAlpha(colors, 1);
        float top = getAlpha(colors, SEGMENTS * LightningBoltMesh.VERTICES_ACROSS + 1);
        float belowTop = getAlpha(colors, (SEGMENTS - 1) * LightningBoltMesh.VERTICES_ACROSS + 1);
        assertEquals(1f, strike, EPSILON);
        assertEquals(0f, top, EPSILON);
        assertTrue(belowTop > top && belowTop < strike, "The fade is gradual, below the top " + belowTop);
    }

    @Test
    void meshIsSizedForItsSegments() {
        LightningBoltMesh mesh = new LightningBoltMesh(SEGMENTS, HEIGHT, JITTER);

        assertEquals(LAYERS * (SEGMENTS + 1) * LightningBoltMesh.VERTICES_ACROSS, mesh.getVertexCount());
        assertEquals(LAYERS * SEGMENTS * 4, mesh.getTriangleCount());
    }

    @Test
    void boundCoversEveryShape() {
        LightningBoltMesh mesh = new LightningBoltMesh(SEGMENTS, HEIGHT, JITTER);
        BoundingBox bound = (BoundingBox) mesh.getBound();
        Random random = new Random(7);

        for (int roll = 0; roll < 100; roll++) {
            mesh.randomizePath(random);

            FloatBuffer positions = (FloatBuffer) mesh.getBuffer(VertexBuffer.Type.Position).getData();
            for (int vertex = 0; vertex < mesh.getVertexCount(); vertex++) {
                Vector3f point = getVertex(positions, vertex);
                assertTrue(bound.intersects(point), "Vertex " + vertex + " outside the bound: " + point);
            }
        }
    }

    private static float getAlpha(FloatBuffer colors, int vertex) {
        return colors.get(vertex * 4 + 3);
    }

    private static Vector3f getVertex(FloatBuffer positions, int vertex) {
        return new Vector3f(positions.get(vertex * 3), positions.get(vertex * 3 + 1), positions.get(vertex * 3 + 2));
    }

}
