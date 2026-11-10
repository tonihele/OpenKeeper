/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.map;

import com.jme3.bounding.BoundingBox;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.lang.management.ManagementFactory;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Locale;
import toniarts.openkeeper.view.map.TerrainDeformer.TileShape;

/** A repeatable mesh-deformation benchmark with representative map dimensions. */
public final class TerrainDeformerBenchmark {

    private static final int WIDTH = 85;
    private static final int HEIGHT = 85;
    private static final int GRID_POINTS = 2 * WIDTH + 1;
    private static final int VERTEX_COUNT = GRID_POINTS * GRID_POINTS;
    private static final int WARMUPS = 3;
    private static final int SAMPLES = 9;
    private static final long THREAD_ID = Thread.currentThread().threadId();
    private static volatile long sink;

    private TerrainDeformerBenchmark() {
    }

    public static void main(String[] args) {
        Mesh sourceMesh = createMesh();
        long coldChecksum = runColdSamples(sourceMesh);
        long warmChecksum = runWarmSamples(sourceMesh);
        if (coldChecksum != warmChecksum) {
            throw new IllegalStateException("Cold and warm deformation output differs");
        }
    }

    private static long runColdSamples(Mesh sourceMesh) {
        long[] nanos = new long[SAMPLES];
        long[] allocated = new long[SAMPLES];
        long[] checksums = new long[SAMPLES];
        long[] tileLookups = new long[SAMPLES];
        for (int i = 0; i < WARMUPS + SAMPLES; i++) {
            long[] lookupCounter = {0};
            TerrainDeformer deformer = newDeformer(lookupCounter);
            Geometry geometry = geometry(sourceMesh);
            long beforeAllocation = allocatedBytes();
            long start = System.nanoTime();
            deformer.apply(geometry, null);
            long elapsed = System.nanoTime() - start;
            long allocatedBytes = allocatedBytes() - beforeAllocation;
            long checksum = checksum(geometry);
            sink += checksum;
            if (i >= WARMUPS) {
                int sample = i - WARMUPS;
                nanos[sample] = elapsed;
                allocated[sample] = allocatedBytes;
                checksums[sample] = checksum;
                tileLookups[sample] = lookupCounter[0];
            }
        }
        requireStableChecksums(checksums);
        print("cold", nanos, allocated, checksums, tileLookups);
        return checksums[0];
    }

    private static long runWarmSamples(Mesh sourceMesh) {
        long[] lookupCounter = {0};
        TerrainDeformer deformer = newDeformer(lookupCounter);
        deformer.apply(geometry(sourceMesh), null);
        long[] nanos = new long[SAMPLES];
        long[] allocated = new long[SAMPLES];
        long[] checksums = new long[SAMPLES];
        long[] tileLookups = new long[SAMPLES];
        for (int i = 0; i < WARMUPS + SAMPLES; i++) {
            Geometry geometry = geometry(sourceMesh);
            long beforeAllocation = allocatedBytes();
            long start = System.nanoTime();
            deformer.apply(geometry, null);
            long elapsed = System.nanoTime() - start;
            long allocatedBytes = allocatedBytes() - beforeAllocation;
            long checksum = checksum(geometry);
            sink += checksum;
            if (i >= WARMUPS) {
                int sample = i - WARMUPS;
                nanos[sample] = elapsed;
                allocated[sample] = allocatedBytes;
                checksums[sample] = checksum;
                tileLookups[sample] = lookupCounter[0];
            }
        }
        requireStableChecksums(checksums);
        print("warm", nanos, allocated, checksums, tileLookups);
        return checksums[0];
    }

    private static TerrainDeformer newDeformer(long[] tileLookups) {
        return new TerrainDeformer(WIDTH, HEIGHT, (x, y) -> {
            tileLookups[0]++;
            boolean solid = ((x * 13 + y * 7) % 10) < 6;
            float wibbleH = ((x + y) % 7 + 1) / 1024f;
            float wibbleV = ((x * 3 + y) % 5 + 1) / 1024f;
            float[] leanH = {
                (x % 3 - 1) / 512f,
                (y % 3 - 1) / 512f,
                ((x + y) % 3 - 1) / 512f
            };
            float[] leanV = {
                (y % 3 - 1) / 1024f,
                (x % 3 - 1) / 1024f,
                ((x * 2 + y) % 3 - 1) / 1024f
            };
            return new TileShape(solid, wibbleH, wibbleV, leanH, leanV);
        });
    }

    private static Mesh createMesh() {
        float[] positions = new float[VERTEX_COUNT * 3];
        float[] tangents = new float[VERTEX_COUNT * 4];
        int vertex = 0;
        for (int y = 0; y < GRID_POINTS; y++) {
            for (int x = 0; x < GRID_POINTS; x++) {
                positions[vertex * 3] = x * 0.5f - 0.5f;
                positions[vertex * 3 + 1] = (x + y) % 3 == 0 ? 1.5f : 1f;
                positions[vertex * 3 + 2] = y * 0.5f - 0.5f;
                tangents[vertex * 4] = 1f;
                tangents[vertex * 4 + 1] = 0f;
                tangents[vertex * 4 + 2] = 0f;
                tangents[vertex * 4 + 3] = 1f;
                vertex++;
            }
        }
        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(positions));
        mesh.setBuffer(VertexBuffer.Type.Tangent, 4, BufferUtils.createFloatBuffer(tangents));
        mesh.setBound(new BoundingBox());
        mesh.updateBound();
        mesh.updateCounts();
        return mesh;
    }

    private static Geometry geometry(Mesh sourceMesh) {
        Geometry geometry = new Geometry("terrain-deformer-benchmark", sourceMesh);
        geometry.setModelBound(new BoundingBox());
        return geometry;
    }

    private static long checksum(Geometry geometry) {
        long result = 0xcbf29ce484222325L;
        for (VertexBuffer.Type type : new VertexBuffer.Type[]{VertexBuffer.Type.Position, VertexBuffer.Type.Tangent}) {
            FloatBuffer buffer = (FloatBuffer) geometry.getMesh().getBuffer(type).getData();
            for (int i = 0; i < buffer.limit(); i++) {
                result ^= Float.floatToIntBits(buffer.get(i));
                result *= 0x100000001b3L;
            }
        }
        return result;
    }

    private static long allocatedBytes() {
        java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        if (bean instanceof com.sun.management.ThreadMXBean extended
                && extended.isThreadAllocatedMemorySupported()) {
            if (!extended.isThreadAllocatedMemoryEnabled()) {
                extended.setThreadAllocatedMemoryEnabled(true);
            }
            return extended.getThreadAllocatedBytes(THREAD_ID);
        }
        return -1;
    }

    private static void requireStableChecksums(long[] checksums) {
        for (long checksum : checksums) {
            if (checksum != checksums[0]) {
                throw new IllegalStateException("Mesh deformation output was not deterministic");
            }
        }
    }

    private static void print(String phase, long[] nanos, long[] allocated, long[] checksums,
            long[] tileLookups) {
        System.out.printf(Locale.ROOT,
                "%s vertices=%d medianMs=%.3f rangeMs=%.3f-%.3f medianAllocMiB=%.3f "
                        + "rangeAllocMiB=%.3f-%.3f tileLookups=%d checksum=%016x%n",
                phase, VERTEX_COUNT, median(nanos) / 1_000_000d,
                min(nanos) / 1_000_000d, max(nanos) / 1_000_000d,
                median(allocated) / (1024d * 1024d),
                min(allocated) / (1024d * 1024d), max(allocated) / (1024d * 1024d),
                median(tileLookups), median(checksums));
    }

    private static long median(long[] values) {
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    private static long min(long[] values) {
        return Arrays.stream(values).min().orElseThrow();
    }

    private static long max(long[] values) {
        return Arrays.stream(values).max().orElseThrow();
    }
}
