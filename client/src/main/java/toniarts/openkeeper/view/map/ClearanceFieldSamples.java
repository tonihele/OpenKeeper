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

import java.util.BitSet;

/**
 * Caches clearance samples used while constructing one ceiling surface.
 * Ceiling vertices, quad centres, and normal samples lie on a quarter-tile
 * grid and neighbouring tile patches repeatedly query the same points.
 * One instance is shared by the material batches in that rebuild and is not
 * shared between threads.
 */
final class ClearanceFieldSamples {

    /** A dense cache is worthwhile when the rebuild contains at least this many patches. */
    static final int MINIMUM_TILES_FOR_CACHE = 1024;

    private static final int SAMPLES_PER_TILE = 4;
    private static final int SAMPLE_MARGIN = SAMPLES_PER_TILE;
    private static final float GRID_TOLERANCE = 0.0001f;

    private final ClearanceField field;
    private final boolean cacheEnabled;
    private final int gridWidth;
    private final float[] cachedValues;
    private final BitSet cachedSamples;

    ClearanceFieldSamples(ClearanceField field, boolean cacheEnabled) {
        this.field = field;
        this.cacheEnabled = cacheEnabled;
        if (cacheEnabled) {
            gridWidth = field.width() * SAMPLES_PER_TILE + SAMPLE_MARGIN + 1;
            int gridHeight = field.height() * SAMPLES_PER_TILE + SAMPLE_MARGIN + 1;
            cachedValues = new float[Math.multiplyExact(gridWidth, gridHeight)];
            cachedSamples = new BitSet(cachedValues.length);
        } else {
            gridWidth = 0;
            cachedValues = null;
            cachedSamples = null;
        }
    }

    float clearanceAt(float worldX, float worldY) {
        if (!cacheEnabled) {
            return field.clearanceAt(worldX, worldY);
        }

        float gridXValue = worldX * SAMPLES_PER_TILE;
        float gridYValue = worldY * SAMPLES_PER_TILE;
        int gridX = Math.round(gridXValue);
        int gridY = Math.round(gridYValue);
        if (Math.abs(gridXValue - gridX) > GRID_TOLERANCE
                || Math.abs(gridYValue - gridY) > GRID_TOLERANCE
                || gridX < -SAMPLE_MARGIN || gridX > field.width() * SAMPLES_PER_TILE
                || gridY < -SAMPLE_MARGIN || gridY > field.height() * SAMPLES_PER_TILE) {
            return field.clearanceAt(worldX, worldY);
        }

        int index = (gridY + SAMPLE_MARGIN) * gridWidth + gridX + SAMPLE_MARGIN;
        if (cachedSamples.get(index)) {
            return cachedValues[index];
        }

        float clearance = field.clearanceAt(worldX, worldY);
        cachedValues[index] = clearance;
        cachedSamples.set(index);
        return clearance;
    }
}
