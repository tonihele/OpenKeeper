/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.map;

import com.jme3.math.FastMath;
import java.util.Arrays;
import java.util.Locale;

/** Reproducible hot-method benchmark for the ceiling clearance calculation. */
public final class ClearanceFieldDistanceBenchmark {

    private static final int WIDTH = 85;
    private static final int HEIGHT = 85;
    private static final int WARMUP_PAIRS = 20;
    private static final int MEASURED_PAIRS = 21;
    private static volatile long resultSink;

    private ClearanceFieldDistanceBenchmark() {
    }

    public static void main(String[] args) {
        boolean[] solid = createSolidMap();
        ClearanceField field = new ClearanceField(WIDTH, HEIGHT, solid);
        long previousChecksum = samplePrevious(solid);
        long optimizedChecksum = sampleOptimized(field);
        if (previousChecksum != optimizedChecksum) {
            throw new IllegalStateException("Clearance outputs differ on the benchmark sample grid");
        }
        long[] previousNanos = new long[MEASURED_PAIRS];
        long[] optimizedNanos = new long[MEASURED_PAIRS];
        for (int i = 0; i < WARMUP_PAIRS; i++) {
            resultSink ^= measurePrevious(solid);
            resultSink ^= measureOptimized(field);
        }
        for (int i = 0; i < MEASURED_PAIRS; i++) {
            long previous;
            long optimized;
            if ((i & 1) == 0) {
                previous = measurePrevious(solid);
                optimized = measureOptimized(field);
            } else {
                optimized = measureOptimized(field);
                previous = measurePrevious(solid);
            }
            previousNanos[i] = previous;
            optimizedNanos[i] = optimized;
        }

        double previousMedian = medianMillis(previousNanos);
        double optimizedMedian = medianMillis(optimizedNanos);
        System.out.printf(Locale.ROOT,
                "map=%dx%d java=%s samples=%d warmupPairs=%d measuredPairs=%d previousMedianMs=%.3f optimizedMedianMs=%.3f reductionPct=%.1f checksum=%d%n",
                WIDTH, HEIGHT, System.getProperty("java.version"), (WIDTH * 4 + 4) * (HEIGHT * 4 + 4),
                WARMUP_PAIRS, MEASURED_PAIRS, previousMedian, optimizedMedian,
                100d * (previousMedian - optimizedMedian) / previousMedian, resultSink);
    }

    private static long measurePrevious(boolean[] solid) {
        long start = System.nanoTime();
        resultSink ^= samplePrevious(solid);
        return System.nanoTime() - start;
    }

    private static long measureOptimized(ClearanceField field) {
        long start = System.nanoTime();
        resultSink ^= sampleOptimized(field);
        return System.nanoTime() - start;
    }

    private static long samplePrevious(boolean[] solid) {
        long checksum = 0;
        for (int yi = -2; yi < HEIGHT * 4 + 2; yi++) {
            float y = yi * 0.25f;
            for (int xi = -2; xi < WIDTH * 4 + 2; xi++) {
                float x = xi * 0.25f;
                checksum += Float.floatToIntBits(previousClearanceAt(x, y, solid));
            }
        }
        return checksum;
    }

    private static long sampleOptimized(ClearanceField field) {
        long checksum = 0;
        for (int yi = -2; yi < HEIGHT * 4 + 2; yi++) {
            float y = yi * 0.25f;
            for (int xi = -2; xi < WIDTH * 4 + 2; xi++) {
                float x = xi * 0.25f;
                checksum += Float.floatToIntBits(field.clearanceAt(x, y));
            }
        }
        return checksum;
    }

    private static float previousClearanceAt(float worldX, float worldY, boolean[] solid) {
        int baseX = Math.round(worldX);
        int baseY = Math.round(worldY);
        float bestDistance = 4f;
        for (int oy = -5; oy <= 5; oy++) {
            int ty = baseY + oy;
            if (ty < 0 || ty >= HEIGHT) {
                continue;
            }
            for (int ox = -5; ox <= 5; ox++) {
                int tx = baseX + ox;
                if (tx < 0 || tx >= WIDTH || !solid[ty * WIDTH + tx]) {
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

    private static boolean[] createSolidMap() {
        boolean[] solid = new boolean[WIDTH * HEIGHT];
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                boolean boundary = x == 0 || y == 0 || x == WIDTH - 1 || y == HEIGHT - 1;
                boolean verticalWall = x % 19 == 0 && y % 23 != 11;
                boolean horizontalWall = y % 17 == 0 && x % 29 != 14;
                solid[y * WIDTH + x] = boundary || verticalWall || horizontalWall;
            }
        }
        return solid;
    }

    private static double medianMillis(long[] values) {
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2] / 1_000_000d;
    }
}
