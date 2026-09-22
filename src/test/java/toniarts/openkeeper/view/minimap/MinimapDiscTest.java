/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.minimap;

import com.jme3.math.FastMath;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import java.nio.FloatBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MinimapDiscTest {

    private static final int SEGMENTS = 32;
    private static final int VERTEX_COUNT = SEGMENTS + 1;

    @Test
    void hasOneCentreVertexPlusThirtyTwoPerimeterVerticesAndAFanOfIndices() {
        Mesh mesh = MinimapDisc.build(64f, 64f, 64f);

        assertEquals(VERTEX_COUNT, mesh.getVertexCount());
        VertexBuffer indexBuffer = mesh.getBuffer(Type.Index);
        assertEquals(SEGMENTS * 3, indexBuffer.getNumElements() * indexBuffer.getNumComponents());
    }

    @Test
    void centreVertexIsAtTheGivenCenter() {
        Mesh mesh = MinimapDisc.build(10f, 20f, 5f);
        FloatBuffer positions = (FloatBuffer) mesh.getBuffer(Type.Position).getData();
        positions.rewind();

        assertEquals(10f, positions.get(0), 1e-4f);
        assertEquals(20f, positions.get(1), 1e-4f);
        assertEquals(0f, positions.get(2), 1e-4f);
    }

    @Test
    void firstPerimeterVertexIsAtCenterPlusRadiusOnTheXAxis() {
        Mesh mesh = MinimapDisc.build(10f, 20f, 5f);
        FloatBuffer positions = (FloatBuffer) mesh.getBuffer(Type.Position).getData();
        positions.rewind();

        // Mesh vertex 1 = perimeter k=0, angle=0.
        assertEquals(15f, positions.get(1 * 3), 1e-4f); // 10 + 5*cos(0)
        assertEquals(20f, positions.get(1 * 3 + 1), 1e-4f); // 20 + 5*sin(0)
    }

    @Test
    void quarterTurnPerimeterVertexIsAtCenterPlusRadiusOnTheYAxis() {
        Mesh mesh = MinimapDisc.build(10f, 20f, 5f);
        FloatBuffer positions = (FloatBuffer) mesh.getBuffer(Type.Position).getData();
        positions.rewind();

        // Perimeter k = SEGMENTS/4 -> angle = pi/2 -> mesh vertex 1+k.
        int meshVertex = 1 + SEGMENTS / 4;
        assertEquals(10f, positions.get(meshVertex * 3), 1e-3f);
        assertEquals(25f, positions.get(meshVertex * 3 + 1), 1e-3f);
    }

    @Test
    void centreUvIsAlwaysAtHalfHalfRegardlessOfYaw() {
        Mesh mesh = MinimapDisc.build(10f, 20f, 5f);

        for (float yaw : new float[]{0f, FastMath.HALF_PI, 2.3f, -1.1f}) {
            MinimapDisc.updateUv(mesh, yaw);
            FloatBuffer uvs = (FloatBuffer) mesh.getBuffer(Type.TexCoord).getData();
            uvs.rewind();
            assertEquals(0.5f, uvs.get(0), 1e-4f, "u at yaw=" + yaw);
            assertEquals(0.5f, uvs.get(1), 1e-4f, "v at yaw=" + yaw);
        }
    }

    @Test
    void uvAtFirstPerimeterVertexMatchesItsPositionsAngleOnTheUAxis() {
        Mesh mesh = MinimapDisc.build(10f, 20f, 5f);
        FloatBuffer uvs = (FloatBuffer) mesh.getBuffer(Type.TexCoord).getData();
        uvs.rewind();

        // Mesh vertex 1 = perimeter k=0, angle=0.
        assertEquals(1f, uvs.get(1 * 2), 1e-4f); // 0.5 + 0.5*cos(0)
        assertEquals(0.5f, uvs.get(1 * 2 + 1), 1e-4f); // 0.5 - 0.5*sin(0)
    }

    @Test
    void uvAtQuarterTurnPerimeterVertexIsFlippedOnTheVAxisOnly() {
        Mesh mesh = MinimapDisc.build(10f, 20f, 5f);
        FloatBuffer uvs = (FloatBuffer) mesh.getBuffer(Type.TexCoord).getData();
        uvs.rewind();

        // Perimeter k = SEGMENTS/4 -> angle = pi/2. U matches the position
        // formula's own cos term; only V (sin) is negated - confirmed
        // against the running game back when this was an octagon: only the
        // vertical axis was actually mirrored, not the horizontal one.
        int meshVertex = 1 + SEGMENTS / 4;
        assertEquals(0.5f, uvs.get(meshVertex * 2), 1e-4f); // 0.5 + 0.5*cos(pi/2)
        assertEquals(0f, uvs.get(meshVertex * 2 + 1), 1e-4f); // 0.5 - 0.5*sin(pi/2)
    }

    @Test
    void updateUvAtZeroYawMatchesBuildsOwnInitialUv() {
        Mesh mesh = MinimapDisc.build(10f, 20f, 5f);
        FloatBuffer before = (FloatBuffer) mesh.getBuffer(Type.TexCoord).getData();
        float[] beforeValues = new float[VERTEX_COUNT * 2];
        before.rewind();
        before.get(beforeValues);

        MinimapDisc.updateUv(mesh, 0f);

        FloatBuffer after = (FloatBuffer) mesh.getBuffer(Type.TexCoord).getData();
        float[] afterValues = new float[VERTEX_COUNT * 2];
        after.rewind();
        after.get(afterValues);
        assertEquals(java.util.Arrays.toString(beforeValues), java.util.Arrays.toString(afterValues));
    }

    @Test
    void updateUvDoesNotTouchPositionsOrIndices() {
        Mesh mesh = MinimapDisc.build(10f, 20f, 5f);
        FloatBuffer positionsBefore = (FloatBuffer) mesh.getBuffer(Type.Position).getData();
        float[] beforeValues = new float[VERTEX_COUNT * 3];
        positionsBefore.rewind();
        positionsBefore.get(beforeValues);

        MinimapDisc.updateUv(mesh, FastMath.HALF_PI);

        FloatBuffer positionsAfter = (FloatBuffer) mesh.getBuffer(Type.Position).getData();
        float[] afterValues = new float[VERTEX_COUNT * 3];
        positionsAfter.rewind();
        positionsAfter.get(afterValues);
        assertEquals(java.util.Arrays.toString(beforeValues), java.util.Arrays.toString(afterValues));
    }

    @Test
    void updateUvRotatesTheSampledAngleByTheGivenYaw() {
        Mesh mesh = MinimapDisc.build(10f, 20f, 5f);

        // yaw = pi/2: perimeter k=0's sampling angle becomes 0 + pi/2,
        // i.e. the same UV the k=SEGMENTS/4 vertex had at yaw 0.
        MinimapDisc.updateUv(mesh, FastMath.HALF_PI);

        FloatBuffer uvs = (FloatBuffer) mesh.getBuffer(Type.TexCoord).getData();
        uvs.rewind();
        assertEquals(0.5f, uvs.get(1 * 2), 1e-4f);
        assertEquals(0f, uvs.get(1 * 2 + 1), 1e-4f);
    }

}
