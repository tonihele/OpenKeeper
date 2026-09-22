/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.minimap;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MinimapOctagonTest {

    @Test
    void hasEightVerticesAndEighteenIndices() {
        Mesh mesh = MinimapOctagon.build(64f, 64f, 64f);

        assertEquals(8, mesh.getVertexCount());
        VertexBuffer indexBuffer = mesh.getBuffer(Type.Index);
        assertEquals(18, indexBuffer.getNumElements() * indexBuffer.getNumComponents());
    }

    @Test
    void indicesMatchTheDesignsTriangleTable() {
        Mesh mesh = MinimapOctagon.build(64f, 64f, 64f);
        ShortBuffer indices = (ShortBuffer) mesh.getBuffer(Type.Index).getData();

        short[] expected = {0, 1, 7, 1, 6, 7, 1, 2, 6, 2, 5, 6, 2, 4, 5, 2, 3, 4};
        short[] actual = new short[18];
        indices.rewind();
        indices.get(actual);
        assertEquals(java.util.Arrays.toString(expected), java.util.Arrays.toString(actual));
    }

    @Test
    void vertexZeroIsAtCenterPlusRadiusOnTheXAxis() {
        Mesh mesh = MinimapOctagon.build(10f, 20f, 5f);
        FloatBuffer positions = (FloatBuffer) mesh.getBuffer(Type.Position).getData();
        positions.rewind();

        assertEquals(15f, positions.get(0), 1e-4f); // 10 + 5*cos(0)
        assertEquals(20f, positions.get(1), 1e-4f); // 20 + 5*sin(0)
        assertEquals(0f, positions.get(2), 1e-4f);
    }

    @Test
    void vertexTwoIsAtCenterPlusRadiusOnTheYAxis() {
        Mesh mesh = MinimapOctagon.build(10f, 20f, 5f);
        FloatBuffer positions = (FloatBuffer) mesh.getBuffer(Type.Position).getData();
        positions.rewind();

        // k=2 -> angle = pi/2 -> cos=0, sin=1
        assertEquals(10f, positions.get(2 * 3), 1e-3f);
        assertEquals(25f, positions.get(2 * 3 + 1), 1e-3f);
    }

    @Test
    void uvAtVertexZeroIsZeroOnTheUAxisCenterOnTheVAxis() {
        Mesh mesh = MinimapOctagon.build(10f, 20f, 5f);
        FloatBuffer uvs = (FloatBuffer) mesh.getBuffer(Type.TexCoord).getData();
        uvs.rewind();

        // UV is position's angle rotated 180 degrees - confirmed against
        // the running game (see MinimapOctagon's javadoc).
        assertEquals(0f, uvs.get(0), 1e-4f); // 0.5 - 0.5*cos(0)
        assertEquals(0.5f, uvs.get(1), 1e-4f); // 0.5 - 0.5*sin(0)
    }

}
