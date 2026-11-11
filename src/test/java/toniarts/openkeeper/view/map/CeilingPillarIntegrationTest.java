/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.map;

import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.map.TerrainDeformer.TileShape;

class CeilingPillarIntegrationTest {

    private static final int SIZE = 7;
    private static final float[] ROCK_LEAN = {0.5f, 0.5f, 0.5f};
    private static final float[] PATH_LEAN = {0.19f, 0.19f, 0f};
    private static final float[] NONE = {0, 0, 0};
    private static final TileShape ROCK = new TileShape(true, 0.25f, 0.125f, ROCK_LEAN, ROCK_LEAN);
    private static final TileShape PATH = new TileShape(false, 0.25f, 0.125f, PATH_LEAN, NONE);

    @Test
    void ceilingBoundaryVerticesUseTheSharedTerrainDeformationField() {
        Point pillar = new Point(3, 3);
        ClearanceField field = clearanceAround(pillar);
        TerrainDeformer deformer = new TerrainDeformer(SIZE, SIZE,
                (x, y) -> y == 3 ? PATH : ROCK);
        List<Point> adjacentCeilingTile = List.of(new Point(4, 3));

        Vector3f undeformed = firstVertex(Ceiling.createMesh(field, adjacentCeilingTile, null));
        Vector3f expected = undeformed.clone();
        deformer.deform(expected, null);
        Vector3f actual = firstVertex(Ceiling.createMesh(field, adjacentCeilingTile, deformer));

        assertNotEquals(undeformed, actual, "the fixture must deform at the pillar boundary");
        assertEquals(expected, actual,
                "a ceiling edge touching a pillar must sample the same field as the pillar mesh");
    }

    @Test
    void pillarTopAndAdjacentCeilingBoundaryStayCoincidentAfterDeformation() {
        Point pillarTile = new Point(3, 3);
        ClearanceField field = clearanceAround(pillarTile);
        TerrainDeformer deformer = new TerrainDeformer(SIZE, SIZE,
                (x, y) -> y == 3 ? PATH : ROCK);
        List<Point> adjacentCeilingTile = List.of(new Point(4, 3));
        Vector3f rawCeilingVertex = firstVertex(Ceiling.createMesh(field, adjacentCeilingTile, null));
        Vector3f rootPosition = new Vector3f(3, 1, 3);
        Vector3f localTop = rawCeilingVertex.subtract(rootPosition);

        Mesh mesh = new Mesh();
        mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(
                Vector3f.ZERO, new Vector3f(localTop.x, 0, localTop.z), localTop));
        mesh.setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(new int[]{0, 1, 2}));
        mesh.updateBound();
        Geometry pillar = new Geometry("pillar", mesh);
        pillar.setLocalTranslation(rootPosition);

        PillarGeometry.alignBaseToFloor(pillar, 1f);
        deformer.applyPillar(pillar, null);

        FloatBuffer positions = (FloatBuffer) pillar.getMesh().getBuffer(Type.Position).getData();
        Vector3f deformedLocalTop = new Vector3f(positions.get(6), positions.get(7), positions.get(8));
        Vector3f deformedTop = new Vector3f();
        pillar.getLocalTransform().transformVector(deformedLocalTop, deformedTop);
        Vector3f deformedCeilingVertex = firstVertex(
                Ceiling.createMesh(field, adjacentCeilingTile, deformer));

        assertTrue(deformedCeilingVertex.distance(deformedTop) < 1e-5f,
                "the pillar top and touching ceiling vertex use the same undeformed point and field: "
                + deformedTop + " vs " + deformedCeilingVertex);
    }

    private static ClearanceField clearanceAround(Point pillar) {
        boolean[] solid = new boolean[SIZE * SIZE];
        solid[pillar.y * SIZE + pillar.x] = true;
        return new ClearanceField(SIZE, SIZE, solid);
    }

    private static Vector3f firstVertex(Mesh mesh) {
        FloatBuffer positions = (FloatBuffer) mesh.getBuffer(Type.Position).getData();
        return new Vector3f(positions.get(0), positions.get(1), positions.get(2));
    }
}
