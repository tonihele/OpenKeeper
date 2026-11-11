/*
 * Copyright (C) 2014-2015 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.map;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import java.nio.FloatBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import toniarts.openkeeper.utils.Point;

class CeilingMeshTest {

    @Test
    void qualitySettingScalesRegularVertexAndIndexCounts() {
        ClearanceField field = new ClearanceField(3, 3, new boolean[9]);

        for (int subdivisions : new int[]{2, 4, 8, 16}) {
            Mesh mesh = Ceiling.createMesh(field, List.of(new Point(1, 1)), null, subdivisions);
            int side = subdivisions + 1;
            assertEquals(side * side, vertexCount(mesh));
            assertEquals(subdivisions * subdivisions * 6,
                    mesh.getBuffer(Type.Index).getData().limit());
        }
    }

    @Test
    void neighboringPatchesShareEveryEdgeSampleAfterTerrainDeformation() {
        ClearanceField field = new ClearanceField(5, 3,
                new boolean[]{true, false, false, false, false,
                    true, false, false, false, false,
                    true, false, false, false, false});
        TerrainDeformer deformer = new TerrainDeformer(5, 3, (x, y) -> x == 0
                ? new TerrainDeformer.TileShape(true, 0.08f, 0.04f,
                        new float[]{0.03f, 0.01f, 0.02f}, new float[]{0.02f, 0.01f, 0.03f})
                : new TerrainDeformer.TileShape(false, 0.04f, 0.02f,
                        new float[]{0.01f, 0.02f, 0.01f}, new float[]{0.03f, 0.02f, 0.01f}));
        int subdivisions = 8;
        Mesh mesh = Ceiling.createMesh(field,
                List.of(new Point(2, 1), new Point(3, 1)), deformer, subdivisions);
        FloatBuffer positions = (FloatBuffer) mesh.getBuffer(Type.Position).getData();
        int side = subdivisions + 1;
        int verticesPerTile = side * side;

        for (int row = 0; row < side; row++) {
            int leftEdge = row * side + subdivisions;
            int rightEdge = verticesPerTile + row * side;
            for (int component = 0; component < 3; component++) {
                assertEquals(positions.get(leftEdge * 3 + component),
                        positions.get(rightEdge * 3 + component), 1e-6f,
                        "shared edge row " + row + ", component " + component);
            }
        }
    }

    @Test
    void pillarCapPatchStitchesToTheAdjacentOpenCeiling() {
        ClearanceField field = new ClearanceField(3, 3,
                new boolean[]{false, false, false,
                    false, true, false,
                    false, false, false});
        TerrainDeformer deformer = new TerrainDeformer(3, 3, (x, y) -> x == 1 && y == 1
                ? new TerrainDeformer.TileShape(true, 0.1f, 0.05f,
                        new float[]{0.02f, 0.01f, 0.03f}, new float[]{0.01f, 0.03f, 0.02f})
                : new TerrainDeformer.TileShape(false, 0.03f, 0.02f,
                        new float[]{0.01f, 0.02f, 0.01f}, new float[]{0.02f, 0.01f, 0.03f}));
        int subdivisions = 8;
        Mesh mesh = Ceiling.createMesh(field,
                List.of(new Point(0, 1), new Point(1, 1)), deformer, subdivisions);
        FloatBuffer positions = (FloatBuffer) mesh.getBuffer(Type.Position).getData();
        FloatBuffer normals = (FloatBuffer) mesh.getBuffer(Type.Normal).getData();
        int side = subdivisions + 1;
        int verticesPerTile = side * side;

        for (int row = 0; row < side; row++) {
            int openEdge = row * side + subdivisions;
            int pillarEdge = verticesPerTile + row * side;
            for (int component = 0; component < 3; component++) {
                assertEquals(positions.get(openEdge * 3 + component),
                        positions.get(pillarEdge * 3 + component), 1e-6f,
                        "pillar cap position, row " + row + ", component " + component);
                assertEquals(normals.get(openEdge * 3 + component),
                        normals.get(pillarEdge * 3 + component), 1e-6f,
                        "pillar cap normal, row " + row + ", component " + component);
            }
        }
    }

    @Test
    void qualityIsClampedToSupportedBounds() {
        assertEquals(Ceiling.MIN_SUBDIVISIONS, Ceiling.normalizeSubdivisions(0));
        assertEquals(Ceiling.MAX_SUBDIVISIONS, Ceiling.normalizeSubdivisions(100));
    }

    private static int vertexCount(Mesh mesh) {
        return mesh.getBuffer(Type.Position).getData().limit() / 3;
    }
}
