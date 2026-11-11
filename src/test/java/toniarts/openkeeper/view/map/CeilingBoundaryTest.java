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
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CeilingBoundaryTest {

    @Test
    void perimeterMeshClosesAllFourMapEdgesAtCeilingHeight() {
        int width = 3;
        int height = 2;
        Mesh mesh = Ceiling.createBoundaryMesh(width, height, null);
        FloatBuffer positions = (FloatBuffer) mesh.getBuffer(Type.Position).getData();

        assertEquals(8 * (width + height), positions.limit() / 3);
        assertEquals(1f, minimum(positions, 1), 1e-6f);
        assertEquals(4f, maximum(positions, 1), 1e-6f);
        assertEquals(-0.5f, minimum(positions, 0), 1e-6f);
        assertEquals(width - 0.5f, maximum(positions, 0), 1e-6f);
        assertEquals(-0.5f, minimum(positions, 2), 1e-6f);
        assertEquals(height - 0.5f, maximum(positions, 2), 1e-6f);
        assertEquals(12 * (width + height), mesh.getBuffer(Type.Index).getData().limit());
    }

    @Test
    void perimeterNormalsFaceIntoThePlayableMap() {
        int width = 3;
        int height = 2;
        Mesh mesh = Ceiling.createBoundaryMesh(width, height, null);
        FloatBuffer normals = (FloatBuffer) mesh.getBuffer(Type.Normal).getData();

        assertEquals(1f, normals.get(0), 1e-6f, "west edge faces east");
        assertEquals(-1f, normals.get(4 * 3), 1e-6f, "east edge faces west");
        assertEquals(1f, normals.get(8 * height * 3 + 2), 1e-6f, "north edge faces south");
        assertEquals(-1f, normals.get((8 * height + 4) * 3 + 2), 1e-6f,
                "south edge faces north");
    }

    private static float minimum(FloatBuffer positions, int component) {
        float result = Float.POSITIVE_INFINITY;
        for (int i = component; i < positions.limit(); i += 3) {
            result = Math.min(result, positions.get(i));
        }
        return result;
    }

    private static float maximum(FloatBuffer positions, int component) {
        float result = Float.NEGATIVE_INFINITY;
        for (int i = component; i < positions.limit(); i += 3) {
            result = Math.max(result, positions.get(i));
        }
        return result;
    }
}
