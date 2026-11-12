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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper. If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.view.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.utils.Point;

class ClearanceFieldSamplesTest {

    @Test
    void cachedQuarterTileSamplesMatchTheClearanceField() {
        int width = 17;
        int height = 13;
        boolean[] solid = createSolidGrid(width, height);
        ClearanceField field = new ClearanceField(width, height, solid);
        ClearanceFieldSamples cached = new ClearanceFieldSamples(field, true);

        for (int gridY = -4; gridY <= height * 4; gridY++) {
            for (int gridX = -4; gridX <= width * 4; gridX++) {
                float worldX = gridX / 4f;
                float worldY = gridY / 4f;
                assertEquals(field.clearanceAt(worldX, worldY), cached.clearanceAt(worldX, worldY), 0f,
                        "clearance at quarter-grid coordinate " + worldX + ", " + worldY);
            }
        }
    }

    @Test
    void offGridQueriesUseTheExactClearancePath() {
        int width = 9;
        int height = 7;
        ClearanceField field = new ClearanceField(width, height, createSolidGrid(width, height));
        ClearanceFieldSamples cached = new ClearanceFieldSamples(field, true);

        float[][] points = {{0.123f, 1.789f}, {-0.37f, 2.13f}, {width + 0.37f, height + 0.13f}};
        for (float[] point : points) {
            assertEquals(field.clearanceAt(point[0], point[1]), cached.clearanceAt(point[0], point[1]), 0f);
        }
    }

    @Test
    void cachedAndUncachedMeshBuffersAreIdentical() {
        int width = 15;
        int height = 12;
        boolean[] solid = createSolidGrid(width, height);
        ClearanceField field = new ClearanceField(width, height, solid);
        List<Point> openTiles = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!solid[y * width + x]) {
                    openTiles.add(new Point(x, y));
                }
            }
        }

        Mesh uncached = Ceiling.createMesh(openTiles, null, Set.of(),
                new ClearanceFieldSamples(field, false));
        Mesh cached = Ceiling.createMesh(openTiles, null, Set.of(),
                new ClearanceFieldSamples(field, true));

        assertEquals(uncached.getTriangleCount(), cached.getTriangleCount());
        assertBufferEquals(uncached, cached, Type.Position);
        assertBufferEquals(uncached, cached, Type.TexCoord);
        assertBufferEquals(uncached, cached, Type.Normal);
        assertBufferEquals(uncached, cached, Type.Index);
    }

    private static void assertBufferEquals(Mesh expected, Mesh actual, Type type) {
        Buffer expectedData = expected.getBuffer(type).getData();
        Buffer actualData = actual.getBuffer(type).getData();
        assertNotNull(expectedData);
        assertNotNull(actualData);
        assertEquals(expectedData.limit(), actualData.limit(), "buffer size for " + type);

        if (expectedData instanceof FloatBuffer expectedFloats && actualData instanceof FloatBuffer actualFloats) {
            for (int i = 0; i < expectedFloats.limit(); i++) {
                assertEquals(expectedFloats.get(i), actualFloats.get(i), 0f, type + " at " + i);
            }
        } else if (expectedData instanceof IntBuffer expectedInts && actualData instanceof IntBuffer actualInts) {
            for (int i = 0; i < expectedInts.limit(); i++) {
                assertEquals(expectedInts.get(i), actualInts.get(i), type + " at " + i);
            }
        } else {
            throw new AssertionError("Unexpected buffer type for " + type + ": " + expectedData.getClass());
        }
    }

    private static boolean[] createSolidGrid(int width, int height) {
        boolean[] solid = new boolean[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                solid[y * width + x] = x == 0 || y == 0 || x == width - 1 || y == height - 1
                        || (x * 7 + y * 11) % 13 < 3;
            }
        }
        return solid;
    }
}
