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

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import toniarts.openkeeper.utils.Point;

/**
 * A repeatable CPU microbenchmark for the clearance sampling phase of a
 * visible ceiling rebuild. Run with {@code ./gradlew benchmarkCeilingBuild}.
 */
public final class CeilingBuildBenchmark {

    private static final int WIDTH = 85;
    private static final int HEIGHT = 85;
    private static final int WARMUP_PAIRS = 4;
    private static final int MEASURED_PAIRS = 9;
    private static final boolean[] SOLID = createSolidGrid();
    private static final List<Workload> WORKLOADS = createWorkloads();
    private static volatile int meshSink;

    private CeilingBuildBenchmark() {
    }

    public static void main(String[] args) {
        System.out.printf(Locale.ROOT, "map=%dx%d warmups=%d measuredPairs=%d java=%s processors=%d%n",
                WIDTH, HEIGHT, WARMUP_PAIRS, MEASURED_PAIRS,
                System.getProperty("java.version"), Runtime.getRuntime().availableProcessors());

        for (Workload workload : WORKLOADS) {
            benchmark(workload);
        }
    }

    private static void benchmark(Workload workload) {
        System.out.printf(Locale.ROOT, "workload=%s visibleTiles=%d batches=%d%n",
                workload.name(), workload.tileCount(), workload.batches().size());
        for (int i = 0; i < WARMUP_PAIRS; i++) {
            measure(workload, false);
            measure(workload, true);
        }

        long[] directNanos = new long[MEASURED_PAIRS];
        long[] cachedNanos = new long[MEASURED_PAIRS];
        long[] directBytes = new long[MEASURED_PAIRS];
        long[] cachedBytes = new long[MEASURED_PAIRS];
        for (int i = 0; i < MEASURED_PAIRS; i++) {
            Result direct;
            Result cached;
            if ((i & 1) == 0) {
                direct = measure(workload, false);
                cached = measure(workload, true);
            } else {
                cached = measure(workload, true);
                direct = measure(workload, false);
            }
            directNanos[i] = direct.elapsedNanos();
            cachedNanos[i] = cached.elapsedNanos();
            directBytes[i] = direct.allocatedBytes();
            cachedBytes[i] = cached.allocatedBytes();
            System.out.printf(Locale.ROOT,
                    "pair=%d uncachedMs=%.3f cachedMs=%.3f uncachedAllocatedMb=%.3f cachedAllocatedMb=%.3f%n",
                    i + 1, toMilliseconds(direct.elapsedNanos()), toMilliseconds(cached.elapsedNanos()),
                    toMegabytes(direct.allocatedBytes()), toMegabytes(cached.allocatedBytes()));
        }

        double uncachedMedian = toMilliseconds(median(directNanos));
        double cachedMedian = toMilliseconds(median(cachedNanos));
        double uncachedBytesMedian = toMegabytes(median(directBytes));
        double cachedBytesMedian = toMegabytes(median(cachedBytes));
        System.out.printf(Locale.ROOT,
                "summary workload=%s uncachedMedianMs=%.3f cachedMedianMs=%.3f cpuReductionPct=%.1f uncachedAllocatedMedianMb=%.3f cachedAllocatedMedianMb=%.3f sink=%d%n",
                workload.name(),
                uncachedMedian, cachedMedian, 100d * (uncachedMedian - cachedMedian) / uncachedMedian,
                uncachedBytesMedian, cachedBytesMedian, meshSink);
    }

    private static Result measure(Workload workload, boolean useCache) {
        System.gc();
        long threadId = Thread.currentThread().threadId();
        com.sun.management.ThreadMXBean allocationBean = getAllocationBean();
        long allocatedBefore = allocationBean == null ? -1 : allocationBean.getThreadAllocatedBytes(threadId);

        long start = System.nanoTime();
        ClearanceField field = new ClearanceField(WIDTH, HEIGHT, SOLID);
        ClearanceFieldSamples samples = new ClearanceFieldSamples(field, useCache);
        Mesh[] meshes = new Mesh[workload.batches().size()];
        for (int i = 0; i < meshes.length; i++) {
            meshes[i] = Ceiling.createMesh(workload.batches().get(i), null, Set.of(), samples);
        }
        long elapsedNanos = System.nanoTime() - start;
        long allocatedBytes = allocationBean == null
                ? -1 : allocationBean.getThreadAllocatedBytes(threadId) - allocatedBefore;
        for (Mesh mesh : meshes) {
            meshSink += mesh.getTriangleCount();
            mesh.clearBuffer(Type.Position);
            mesh.clearBuffer(Type.TexCoord);
            mesh.clearBuffer(Type.Normal);
            mesh.clearBuffer(Type.Index);
        }
        return new Result(elapsedNanos, allocatedBytes);
    }

    private static com.sun.management.ThreadMXBean getAllocationBean() {
        if (ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean bean
                && bean.isThreadAllocatedMemorySupported()) {
            try {
                if (!bean.isThreadAllocatedMemoryEnabled()) {
                    bean.setThreadAllocatedMemoryEnabled(true);
                }
                return bean;
            } catch (SecurityException ignored) {
                return null;
            }
        }
        return null;
    }

    private static boolean[] createSolidGrid() {
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

    private static List<Workload> createWorkloads() {
        return List.of(createWorkload("visible-64", 64), createWorkload("visible-256", 256),
                createWorkload("visible-1024", 1024), createWorkload("visible-full", Integer.MAX_VALUE));
    }

    private static Workload createWorkload(String name, int maximumTiles) {
        List<List<Point>> batches = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            batches.add(new ArrayList<>());
        }
        int tileCount = 0;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (!SOLID[y * WIDTH + x]) {
                    if (tileCount == maximumTiles) {
                        return new Workload(name, nonEmptyBatches(batches), tileCount);
                    }
                    batches.get(((x / 8) + (y / 8)) & 3).add(new Point(x, y));
                    tileCount++;
                }
            }
        }
        return new Workload(name, nonEmptyBatches(batches), tileCount);
    }

    private static List<List<Point>> nonEmptyBatches(List<List<Point>> batches) {
        return batches.stream().filter(batch -> !batch.isEmpty()).map(List::copyOf).toList();
    }

    private static long median(long[] values) {
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    private static double toMilliseconds(long nanos) {
        return nanos / 1_000_000d;
    }

    private static double toMegabytes(long bytes) {
        return bytes < 0 ? -1 : bytes / (1024d * 1024d);
    }

    private record Result(long elapsedNanos, long allocatedBytes) {
    }

    private record Workload(String name, List<List<Point>> batches, int tileCount) {
    }
}
