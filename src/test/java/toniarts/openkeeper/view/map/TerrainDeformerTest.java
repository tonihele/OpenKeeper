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

import com.jme3.asset.DesktopAssetManager;
import com.jme3.material.Material;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.shape.Quad;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import toniarts.openkeeper.view.map.TerrainDeformer.TileShape;

class TerrainDeformerTest {

    private static final int SIZE = 7;
    private static final float[] ROCK_LEAN = {0.5f, 0.5f, 0.5f};
    private static final float[] PATH_LEAN = {0.19f, 0.19f, 0f};
    private static final float[] NONE = {0, 0, 0};

    private static final TileShape ROCK = new TileShape(true, 0.25f, 0.125f, ROCK_LEAN, ROCK_LEAN);
    private static final TileShape PATH = new TileShape(false, 0.25f, 0.125f, PATH_LEAN, NONE);
    private static final TileShape RIGID = new TileShape(false, 0, 0, NONE, NONE);

    /**
     * A corridor along the middle row and a rigid tile in it
     */
    private static TerrainDeformer corridor() {
        return new TerrainDeformer(SIZE, SIZE, (x, y) -> y == 3 ? (x == 5 ? RIGID : PATH) : ROCK);
    }

    private static Vector3f deformed(TerrainDeformer d, float x, float y, float z) {
        Vector3f result = new Vector3f();
        d.deform(new Vector3f(x, y, z), result, null);
        return result;
    }

    @Test
    void sameInputGivesBitIdenticalOutput() {
        TerrainDeformer a = corridor();
        TerrainDeformer b = corridor();
        for (float x = -0.5f; x <= 6.5f; x += 0.25f) {
            for (float y = 0.5f; y < 2.5f; y += 0.25f) {
                assertEquals(deformed(a, x, y, 3.5f), deformed(b, x, y, 3.5f));
            }
        }
    }

    @Test
    void invalidatingGivesTheSameAsFreshField() {
        boolean[] dug = new boolean[1];
        TerrainDeformer d = new TerrainDeformer(SIZE, SIZE,
                (x, y) -> (y == 3 || dug[0] && x == 3 && y == 2) ? PATH : ROCK);
        Vector3f before = deformed(d, 3f, 2f, 2.5f);
        dug[0] = true;
        d.invalidateTile(3, 2);
        TerrainDeformer fresh = new TerrainDeformer(SIZE, SIZE,
                (x, y) -> (y == 3 || x == 3 && y == 2) ? PATH : ROCK);
        for (float x = 0.5f; x <= 5.5f; x += 0.25f) {
            for (float z = 0.5f; z <= 5.5f; z += 0.25f) {
                assertEquals(deformed(fresh, x, 2f, z), deformed(d, x, 2f, z));
            }
        }
        assertNotEquals(before, deformed(d, 3f, 2f, 2.5f));
    }

    @Test
    void rigidTileDoesNotMove() {
        TerrainDeformer d = corridor();
        // Pinned only where all the touching tiles are rigid, the centre of the tile is
        for (float y = 1f; y <= 2f; y += 0.25f) {
            Vector3f p = new Vector3f(5f, y, 3f);
            assertEquals(p, deformed(d, 5f, y, 3f));
        }
    }

    @Test
    void wallsLeanBackAndNeverIntoTheCorridor() {
        TerrainDeformer d = corridor();
        // Walk along the top of the south wall (rock row 4), the wall face is at z = 3.5
        for (float x = 0.5f; x <= 3.5f; x += 0.125f) {
            Vector3f top = deformed(d, x, 2f, 3.5f);
            assertTrue(top.z >= 3.5f, "wall top intrudes the corridor at x=" + x + ": " + top);
            Vector3f foot = deformed(d, x, 1f, 3.5f);
            assertTrue(foot.z >= 3.5f - 1e-5f, "wall foot intrudes the corridor at x=" + x + ": " + foot);
        }
        // And it does lean back at the top
        assertTrue(deformed(d, 2f, 2f, 3.5f).z > 3.5f + 0.05f);
    }

    @Test
    void belowAndAboveTheLatticeIsLeftAlone() {
        TerrainDeformer d = corridor();
        Vector3f p = new Vector3f(2f, 0.2f, 2.5f);
        assertEquals(p, deformed(d, p.x, p.y, p.z));
        p.set(2f, 3f, 2.5f);
        assertEquals(p, deformed(d, p.x, p.y, p.z));
        p.set(-3f, 1.5f, 2.5f);
        assertEquals(p, deformed(d, p.x, p.y, p.z));
    }

    @Test
    void jacobianMatchesFiniteDifferences() {
        TerrainDeformer d = corridor();
        Vector3f p = new Vector3f(2.3f, 1.6f, 3.4f);
        Matrix3f j = new Matrix3f();
        d.deform(p, new Vector3f(), j);
        float h = 1e-3f;
        Vector3f a = deformed(d, p.x + h, p.y, p.z);
        Vector3f b = deformed(d, p.x - h, p.y, p.z);
        assertEquals((a.x - b.x) / (2 * h), j.get(0, 0), 2e-2f);
        assertEquals((a.y - b.y) / (2 * h), j.get(1, 0), 2e-2f);
        a = deformed(d, p.x, p.y, p.z + h);
        b = deformed(d, p.x, p.y, p.z - h);
        assertEquals((a.z - b.z) / (2 * h), j.get(2, 2), 2e-2f);
    }

    @Test
    void batchedGeometryIsDeformedWithoutTouchingTheSharedMesh() {
        TerrainDeformer d = corridor();

        // A wall piece standing at the rock tile (2, 4), sharing its mesh like cached models do
        Mesh shared = new Quad(1, 1);
        shared.setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(
                new Vector3f(0, 0, 1), new Vector3f(0, 0, 1), new Vector3f(0, 0, 1), new Vector3f(0, 0, 1)));
        float[] before = new float[12];
        ((FloatBuffer) shared.getBuffer(Type.Position).getData()).duplicate().rewind().get(before);

        BatchNode inner = new BatchNode();
        Geometry piece = new Geometry("piece", shared);
        piece.setMaterial(new Material(new DesktopAssetManager(true), "Common/MatDefs/Misc/Unshaded.j3md"));
        piece.setLocalTranslation(2f, 1f, 3.5f);
        inner.attachChild(piece);
        inner.batch();

        Node tile = new Node();
        tile.attachChild(inner);
        Node page = new Node();
        BatchNode outer = new BatchNode();
        page.attachChild(outer);
        outer.attachChild(tile);

        d.apply(inner, page);
        outer.batch();

        float[] after = new float[12];
        ((FloatBuffer) shared.getBuffer(Type.Position).getData()).duplicate().rewind().get(after);
        assertArrayEquals(before, after, "the shared mesh was modified");

        Mesh mesh = piece.getMesh();
        assertNotSame(shared, mesh);
        FloatBuffer positions = (FloatBuffer) mesh.getBuffer(Type.Position).getData();
        // The top left vertex of the quad is at the top of the wall now, leaning
        Vector3f top = new Vector3f(positions.get(9), positions.get(10), positions.get(11));
        assertNotEquals(new Vector3f(1f, 1f, 0f), top);
        FloatBuffer normals = (FloatBuffer) mesh.getBuffer(Type.Normal).getData();
        assertEquals(1f, new Vector3f(normals.get(0), normals.get(1), normals.get(2)).length(), 1e-4f);
    }
}
