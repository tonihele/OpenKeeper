/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.map;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jme3.math.FastMath;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ClearanceFieldDistanceParityTest {

    private static final int SEARCH_RADIUS = 5;
    private static final float SEARCH_LIMIT = 4f;

    @Test
    void squaredDistanceSelectionMatchesThePreviousClearanceCalculation() {
        assertMatchesPreviousCalculation(1, 1, 1L, 0f);
        assertMatchesPreviousCalculation(7, 9, 2L, 0.31f);
        assertMatchesPreviousCalculation(85, 85, 3L, 0.27f);
        assertMatchesPreviousCalculation(85, 85, 4L, 0f);
    }

    private static void assertMatchesPreviousCalculation(int width, int height, long seed,
            float solidProbability) {
        Random random = new Random(seed);
        boolean[] solid = new boolean[width * height];
        for (int i = 0; i < solid.length; i++) {
            solid[i] = random.nextFloat() < solidProbability;
        }
        ClearanceField field = new ClearanceField(width, height, solid);

        // The ceiling mesh samples on quarter-tile intervals. Include the
        // edge margin used by its normals and arbitrary off-grid positions.
        for (int yi = -2; yi < height * 4 + 2; yi++) {
            float y = yi * 0.25f;
            for (int xi = -2; xi < width * 4 + 2; xi++) {
                float x = xi * 0.25f;
                assertSameBits(previousClearanceAt(x, y, width, height, solid), field.clearanceAt(x, y), x, y);
            }
        }
        for (int i = 0; i < 2_000; i++) {
            float x = random.nextFloat() * (width + 2f) - 1f;
            float y = random.nextFloat() * (height + 2f) - 1f;
            assertSameBits(previousClearanceAt(x, y, width, height, solid), field.clearanceAt(x, y), x, y);
        }
    }

    private static float previousClearanceAt(float worldX, float worldY, int width, int height,
            boolean[] solid) {
        int baseX = Math.round(worldX);
        int baseY = Math.round(worldY);
        float bestDistance = SEARCH_LIMIT;

        for (int oy = -SEARCH_RADIUS; oy <= SEARCH_RADIUS; oy++) {
            int ty = baseY + oy;
            if (ty < 0 || ty >= height) {
                continue;
            }
            for (int ox = -SEARCH_RADIUS; ox <= SEARCH_RADIUS; ox++) {
                int tx = baseX + ox;
                if (tx < 0 || tx >= width || !solid[ty * width + tx]) {
                    continue;
                }
                float dx = Math.max(0f, Math.abs(worldX - tx) - 0.5f);
                float dy = Math.max(0f, Math.abs(worldY - ty) - 0.5f);
                float distance = FastMath.sqrt(dx * dx + dy * dy);
                if (distance < bestDistance) {
                    bestDistance = distance;
                }
            }
        }
        return Math.min(ClearanceField.MAX_CLEARANCE, 4f * bestDistance);
    }

    private static void assertSameBits(float expected, float actual, float x, float y) {
        assertEquals(Float.floatToIntBits(expected), Float.floatToIntBits(actual),
                () -> "clearance differs at (" + x + ", " + y + "): " + expected + " != " + actual);
    }
}
