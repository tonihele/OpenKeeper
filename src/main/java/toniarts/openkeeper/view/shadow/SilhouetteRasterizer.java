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
import java.util.Arrays;

import static toniarts.openkeeper.view.shadow.ShadowConstants.COVERAGE_PER_SUBSAMPLE;
import static toniarts.openkeeper.view.shadow.ShadowConstants.RASTER_CENTRE;
import static toniarts.openkeeper.view.shadow.ShadowConstants.RASTER_EXTENT;
import static toniarts.openkeeper.view.shadow.ShadowConstants.RASTER_UNITS;
import static toniarts.openkeeper.view.shadow.ShadowConstants.SILHOUETTE_SIZE;
import static toniarts.openkeeper.view.shadow.ShadowConstants.SUBSAMPLES;

/**
 * Rasterises a caster's silhouette into a {@link ShadowConstants#SILHOUETTE_SIZE}
 * square coverage buffer. The triangles are scan converted at 8 sub-rows and 8
 * sub-columns per pixel, each covered subsample adding 4 and saturating at
 * 255: an 8x8 supersampled, anti-aliased mask, 0 empty and 255 fully covered.
 */
final class SilhouetteRasterizer {

    private final int[] coverage = new int[SILHOUETTE_SIZE * SILHOUETTE_SIZE];
    private float[] projected = new float[0];

    void clear() {
        Arrays.fill(coverage, 0);
    }

    /**
     * Draws the triangles of a caster, projected orthographically along a
     * view tilted from straight down, onto this buffer. The image columns run
     * along {@code v}, the horizontal perpendicular of {@code u}, and the rows
     * along the view's up, which at {@code theta == 0} (straight down) is
     * {@code u}. The bounding sphere covers +-125 of the +-128 raster units.
     *
     * @param vertices xyz of the world space vertices
     * @param triangles the vertex indices, three per triangle
     * @param centreX the centre of the caster's bounding sphere (also Y, Z)
     * @param radius the radius of the bounding sphere
     * @param ux the horizontal unit vector from the light to the caster
     * @param theta the tilt from straight down in radians, along {@code u}
     */
    void draw(float[] vertices, int vertexCount, int[] triangles, int triangleCount,
            float centreX, float centreY, float centreZ, float radius,
            float ux, float uz, float theta) {
        if (radius <= 0f) {
            return;
        }
        if (projected.length < vertexCount * 2) {
            projected = new float[vertexCount * 2];
        }
        float scale = RASTER_EXTENT / radius;
        float vx = -uz;
        float vz = ux;
        float cos = (float) Math.cos(theta);
        float sin = (float) Math.sin(theta);
        for (int i = 0; i < vertexCount; i++) {
            float dx = vertices[i * 3] - centreX;
            float dy = vertices[i * 3 + 1] - centreY;
            float dz = vertices[i * 3 + 2] - centreZ;
            projected[i * 2] = RASTER_CENTRE + scale * (dx * vx + dz * vz);
            projected[i * 2 + 1] = RASTER_CENTRE + scale * ((dx * ux + dz * uz) * cos + dy * sin);
        }
        for (int i = 0; i < triangleCount; i++) {
            int a = triangles[i * 3] * 2;
            int b = triangles[i * 3 + 1] * 2;
            int c = triangles[i * 3 + 2] * 2;
            triangle(projected[a], projected[a + 1], projected[b], projected[b + 1], projected[c], projected[c + 1]);
        }
    }

    /**
     * Scan converts a triangle given in raster units (0..256 across), adding
     * its coverage
     */
    void triangle(float x0, float y0, float x1, float y1, float x2, float y2) {
        float minY = Math.min(y0, Math.min(y1, y2));
        float maxY = Math.max(y0, Math.max(y1, y2));
        if (Float.isNaN(minY) || Float.isNaN(maxY) || Float.isNaN(x0 + x1 + x2)) {
            return;
        }

        // A subsample row is sampled at its centre
        int firstRow = Math.max(0, (int) Math.ceil(minY - 0.5f));
        int lastRow = Math.min(RASTER_UNITS - 1, (int) Math.floor(maxY - 0.5f));
        for (int row = firstRow; row <= lastRow; row++) {
            float y = row + 0.5f;
            float left = Float.POSITIVE_INFINITY;
            float right = Float.NEGATIVE_INFINITY;
            if (crosses(y0, y1, y)) {
                float x = intersect(x0, y0, x1, y1, y);
                left = Math.min(left, x);
                right = Math.max(right, x);
            }
            if (crosses(y1, y2, y)) {
                float x = intersect(x1, y1, x2, y2, y);
                left = Math.min(left, x);
                right = Math.max(right, x);
            }
            if (crosses(y2, y0, y)) {
                float x = intersect(x2, y2, x0, y0, y);
                left = Math.min(left, x);
                right = Math.max(right, x);
            }
            if (left > right) {
                continue;
            }

            // The subsample columns whose centre is inside
            int firstColumn = Math.max(0, (int) Math.ceil(left - 0.5f));
            int lastColumn = Math.min(RASTER_UNITS - 1, (int) Math.floor(right - 0.5f));
            if (firstColumn > lastColumn) {
                continue;
            }
            for (int pixel = firstColumn / SUBSAMPLES; pixel <= lastColumn / SUBSAMPLES; pixel++) {
                int from = Math.max(firstColumn, pixel * SUBSAMPLES);
                int to = Math.min(lastColumn, pixel * SUBSAMPLES + SUBSAMPLES - 1);
                coverage[(row / SUBSAMPLES) * SILHOUETTE_SIZE + pixel] += (to - from + 1) * COVERAGE_PER_SUBSAMPLE;
            }
        }
    }

    private static boolean crosses(float ya, float yb, float y) {
        return (ya <= y && y < yb) || (yb <= y && y < ya);
    }

    private static float intersect(float xa, float ya, float xb, float yb, float y) {
        return xa + (y - ya) * (xb - xa) / (yb - ya);
    }

    /**
     * @return the coverage of a pixel, 0..255
     */
    int get(int column, int row) {
        return Math.min(255, coverage[row * SILHOUETTE_SIZE + column]);
    }

    /**
     * Writes the coverage as the alpha of black RGBA pixels, row 0 first
     */
    void writeTo(ByteBuffer rgba) {
        for (int i = 0; i < coverage.length; i++) {
            rgba.put(i * 4, (byte) 0);
            rgba.put(i * 4 + 1, (byte) 0);
            rgba.put(i * 4 + 2, (byte) 0);
            rgba.put(i * 4 + 3, (byte) Math.min(255, coverage[i]));
        }
    }

    /**
     * Copies the coverage of another rasteriser
     */
    void copyFrom(SilhouetteRasterizer other) {
        System.arraycopy(other.coverage, 0, coverage, 0, coverage.length);
    }
}
