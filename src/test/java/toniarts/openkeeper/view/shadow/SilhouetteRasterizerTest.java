/*
 * Copyright (C) 2014-2025 OpenKeeper
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
package toniarts.openkeeper.view.shadow;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link SilhouetteRasterizer}.
 */
class SilhouetteRasterizerTest {

    @Test
    void emptyBufferHasNoCoverage() {
        SilhouetteRasterizer rasterizer = new SilhouetteRasterizer();

        for (int row = 0; row < 32; row++) {
            for (int column = 0; column < 32; column++) {
                assertEquals(0, rasterizer.get(column, row));
            }
        }
    }

    @Test
    void squareOfTwoTrianglesCoversItsPixelsFullyAndNothingElse() {
        SilhouetteRasterizer rasterizer = new SilhouetteRasterizer();

        // Pixels 8..23 are raster units 64..192
        rasterizer.triangle(64, 64, 192, 64, 192, 192);
        rasterizer.triangle(64, 64, 192, 192, 64, 192);

        assertEquals(255, rasterizer.get(16, 16));
        assertEquals(255, rasterizer.get(8, 8));
        assertEquals(255, rasterizer.get(23, 23));
        assertEquals(0, rasterizer.get(7, 16));
        assertEquals(0, rasterizer.get(24, 16));
        assertEquals(0, rasterizer.get(16, 7));
        assertEquals(0, rasterizer.get(16, 24));
    }

    @Test
    void partlyCoveredPixelGetsFourPerCoveredSubsample() {
        SilhouetteRasterizer rasterizer = new SilhouetteRasterizer();

        // A pixel at (8, 8), covered in its left 4 of 8 subsample columns
        rasterizer.triangle(64, 64, 68, 64, 68, 72);
        rasterizer.triangle(64, 64, 68, 72, 64, 72);

        assertEquals(8 * 4 * 4, rasterizer.get(8, 8)); // 8 rows x 4 columns x 4
    }

    @Test
    void overlappingTrianglesSaturateAtFullCoverage() {
        SilhouetteRasterizer rasterizer = new SilhouetteRasterizer();

        for (int i = 0; i < 3; i++) {
            rasterizer.triangle(0, 0, 256, 0, 0, 256);
        }

        assertEquals(255, rasterizer.get(1, 1));
    }

    @Test
    void trianglesOutsideTheRasterAreClipped() {
        SilhouetteRasterizer rasterizer = new SilhouetteRasterizer();

        rasterizer.triangle(-500, -500, 800, -500, 100, 900);
        rasterizer.triangle(Float.NaN, 0, 1, 1, 2, 2);

        assertEquals(255, rasterizer.get(16, 16));
    }

    @Test
    void clearEmptiesTheBuffer() {
        SilhouetteRasterizer rasterizer = new SilhouetteRasterizer();
        rasterizer.triangle(0, 0, 256, 0, 0, 256);

        rasterizer.clear();

        assertEquals(0, rasterizer.get(1, 1));
    }

    @Test
    void projectionFillsTheBoundingSphereSquareAndFlipsWithTheLightDirection() {
        // A flat 1x1 square on the floor, centred at the origin, radius 0.5 around it
        float[] vertices = {-0.5f, 0, -0.5f, 0.5f, 0, -0.5f, 0.5f, 0, 0.5f, -0.5f, 0, 0.5f};
        int[] triangles = {0, 1, 2, 0, 2, 3};
        SilhouetteRasterizer rasterizer = new SilhouetteRasterizer();

        rasterizer.draw(vertices, 4, triangles, 2, 0, 0, 0, 0.5f, 1, 0, 0f);

        // Radius 0.5 maps to +-125 raster units (3..253): pixels 1..30 are
        // covered fully, the border pixels only partly
        assertEquals(255, rasterizer.get(16, 16));
        assertEquals(255, rasterizer.get(1, 1));
        assertEquals(255, rasterizer.get(30, 30));
        assertTrue(rasterizer.get(0, 16) > 0 && rasterizer.get(0, 16) < 255);
        assertTrue(rasterizer.get(31, 16) > 0 && rasterizer.get(31, 16) < 255);
    }

    @Test
    void tiltedViewShiftsHighPointsAlongTheLightDirection() {
        // A single point-like sliver high above the centre, light along +x
        float[] vertices = {-0.05f, 0.4f, -0.05f, 0.05f, 0.4f, -0.05f, 0f, 0.4f, 0.05f};
        int[] triangles = {0, 1, 2};

        SilhouetteRasterizer straight = new SilhouetteRasterizer();
        straight.draw(vertices, 3, triangles, 1, 0, 0, 0, 0.5f, 1, 0, 0f);
        SilhouetteRasterizer tilted = new SilhouetteRasterizer();
        tilted.draw(vertices, 3, triangles, 1, 0, 0, 0, 0.5f, 1, 0, 0.8f);

        // Straight down it is in the middle row; tilted, a point above the
        // centre moves along the light direction by sin(theta) * 0.4 / 0.5 of
        // the half width, i.e. to rows 23..26
        assertTrue(straight.get(16, 16) > 0);
        assertEquals(0, tilted.get(16, 16));
        int shifted = 0;
        for (int row = 22; row <= 27; row++) {
            shifted += tilted.get(16, row);
        }
        assertTrue(shifted > 0);
    }

    @Test
    void writesTheCoverageAsTheAlphaOfBlackPixels() {
        SilhouetteRasterizer rasterizer = new SilhouetteRasterizer();
        rasterizer.triangle(0, 0, 256, 0, 0, 256);
        rasterizer.triangle(256, 0, 256, 256, 0, 256);
        ByteBuffer pixels = ByteBuffer.allocate(32 * 32 * 4);

        rasterizer.writeTo(pixels);

        assertEquals(0, pixels.get(0));
        assertEquals(0, pixels.get(1));
        assertEquals(0, pixels.get(2));
        assertEquals((byte) 255, pixels.get(3));
    }
}
