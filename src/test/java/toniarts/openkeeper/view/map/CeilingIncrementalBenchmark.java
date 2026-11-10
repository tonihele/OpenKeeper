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
 */
package toniarts.openkeeper.view.map;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import java.lang.management.ManagementFactory;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import toniarts.openkeeper.utils.Point;

/**
 * CPU ceiling-refresh benchmark for replacing full rebuilds with spatial
 * chunks. It includes tile grouping, clearance-field construction and mesh
 * generation, and reports draw counts for each active material/chunk pair.
 */
public final class CeilingIncrementalBenchmark {

    private static final int WIDTH = 85;
    private static final int HEIGHT = 85;
    private static final int UPDATE_NEIGHBOR_MARGIN = 1;
    private static final int[] CHUNK_SIZES = {8, 16, 32, 48, 64};
    private static final int WARMUP_PAIRS = 6;
    private static final int MEASURED_PAIRS = 9;
    private static final float[] TEST_LEAN_H = {0.06f, 0.04f, 0.02f};
    private static final float[] TEST_LEAN_V = {0.03f, 0.02f, 0.01f};
    private static volatile int meshSink;

    private CeilingIncrementalBenchmark() {
    }

    public static void main(String[] args) {
        System.out.printf(Locale.ROOT, "map=%dx%d java=%s processors=%d warmupPairs=%d measuredPairs=%d%n",
                WIDTH, HEIGHT, System.getProperty("java.version"), Runtime.getRuntime().availableProcessors(),
                WARMUP_PAIRS, MEASURED_PAIRS);
        for (Scenario scenario : benchmarkScenarios()) {
            benchmark(scenario);
        }
    }

    static List<Scenario> parityScenarios() {
        return List.of(digScenario("single-tile-dig", 25, 25, 11, 11, 1, 1),
                digScenario("room-dig-8x8", 25, 25, 8, 8, 8, 8),
                fogRevealScenario("fog-reveal-8x8", 25, 25, 8, 8, 8, 8),
                digScenario("chunk-boundary-dig", 85, 85, 47, 47, 2, 2),
                fogRevealScenario("map-edge-fog-reveal", 85, 85, 0, 0, 4, 4));
    }

    static int[] candidateChunkSizes() {
        return new int[]{8, 16, 32, 48, 64};
    }

    static MapState visibleBenchmarkMap(int width, int height) {
        return baseState(width, height);
    }

    static List<Mesh> buildBenchmarkMeshes(MapState state, int chunkSize) {
        ClearanceField field = new ClearanceField(state.width(), state.height(), state.solid());
        return buildMeshes(field, group(state, chunkSize).values());
    }

    static boolean verifyParity(Scenario scenario, int chunkSize) {
        Set<Point> straightTiles = Set.of(new Point(0, 0),
                new Point(scenario.after().width() - 1, scenario.after().height() - 1));
        Map<Point, PatchData> expected = buildPatchMap(scenario.after(), 0, null,
                createBenchmarkDeformer(scenario.after(), straightTiles), straightTiles);
        Map<Point, PatchData> candidate = buildPatchMap(scenario.before(), chunkSize, null,
                createBenchmarkDeformer(scenario.before(), straightTiles), straightTiles);
        Set<Chunk> dirty = dirtyChunks(scenario, chunkSize);
        candidate.keySet().removeIf(point -> dirty.contains(chunkOf(point, chunkSize)));
        candidate.putAll(buildPatchMap(scenario.after(), chunkSize, dirty,
                createBenchmarkDeformer(scenario.after(), straightTiles), straightTiles));
        return samePatches(expected, candidate);
    }

    private static List<Scenario> benchmarkScenarios() {
        return List.of(digScenario("single-tile-dig", WIDTH, HEIGHT, 31, 31, 1, 1),
                digScenario("room-dig-8x8", WIDTH, HEIGHT, 28, 28, 8, 8),
                digScenario("room-dig-16x16", WIDTH, HEIGHT, 24, 24, 16, 16),
                digScenario("room-dig-24x24", WIDTH, HEIGHT, 20, 20, 24, 24),
                fogRevealScenario("fog-reveal-16x16", WIDTH, HEIGHT, 34, 34, 16, 16));
    }

    private static void benchmark(Scenario scenario) {
        Map<Batch, List<Point>> fullBatches = group(scenario.after(), 0);
        System.out.printf(Locale.ROOT,
                "scenario=%s changedTiles=%d fullTiles=%d fullDraws=%d fieldChanged=%s%n",
                scenario.name(), scenario.changedTiles().size(), tileCount(fullBatches), fullBatches.size(),
                scenario.fieldChanged());

        for (int chunkSize : CHUNK_SIZES) {
            Map<Batch, List<Point>> chunkBatches = group(scenario.after(), chunkSize);
            Set<Chunk> dirtyChunks = dirtyChunks(scenario, chunkSize);
            List<Map.Entry<Batch, List<Point>>> dirtyBatches = chunkBatches.entrySet().stream()
                    .filter(entry -> dirtyChunks.contains(new Chunk(entry.getKey().chunkX(), entry.getKey().chunkY())))
                    .toList();
            int dirtyTiles = dirtyBatches.stream().mapToInt(entry -> entry.getValue().size()).sum();

            long[] fullNanos = new long[MEASURED_PAIRS];
            long[] chunkNanos = new long[MEASURED_PAIRS];
            long[] fullAllocated = new long[MEASURED_PAIRS];
            long[] chunkAllocated = new long[MEASURED_PAIRS];
            long[] fullOutputBytes = new long[MEASURED_PAIRS];
            long[] chunkOutputBytes = new long[MEASURED_PAIRS];
            for (int i = 0; i < WARMUP_PAIRS; i++) {
                measure(scenario, 0);
                measure(scenario, chunkSize);
            }
            for (int i = 0; i < MEASURED_PAIRS; i++) {
                Result full;
                Result chunked;
                if ((i & 1) == 0) {
                    full = measure(scenario, 0);
                    chunked = measure(scenario, chunkSize);
                } else {
                    chunked = measure(scenario, chunkSize);
                    full = measure(scenario, 0);
                }
                fullNanos[i] = full.elapsedNanos();
                chunkNanos[i] = chunked.elapsedNanos();
                fullAllocated[i] = full.allocatedBytes();
                chunkAllocated[i] = chunked.allocatedBytes();
                fullOutputBytes[i] = full.outputBytes();
                chunkOutputBytes[i] = chunked.outputBytes();
            }

            double fullMedian = medianMillis(fullNanos);
            double chunkMedian = medianMillis(chunkNanos);
            System.out.printf(Locale.ROOT,
                    "  chunk=%d dirtyChunks=%d dirtyTiles=%d activeDraws=%d rebuiltDraws=%d fullMedianMs=%.3f incrementalMedianMs=%.3f meshBuildReductionPct=%.1f javaAllocatedMiB=%.2f->%.2f rebuiltMeshBuffersMiB=%.2f->%.2f%n",
                    chunkSize, dirtyChunks.size(), dirtyTiles, chunkBatches.size(), dirtyBatches.size(),
                    fullMedian, chunkMedian, 100d * (fullMedian - chunkMedian) / fullMedian,
                    medianMiB(fullAllocated), medianMiB(chunkAllocated),
                    medianMiB(fullOutputBytes), medianMiB(chunkOutputBytes));
        }
    }

    private static Result measure(Scenario scenario, int chunkSize) {
        System.gc();
        long threadId = Thread.currentThread().threadId();
        com.sun.management.ThreadMXBean allocationBean = allocationBean();
        long allocatedBefore = allocationBean == null ? -1 : allocationBean.getThreadAllocatedBytes(threadId);
        long start = System.nanoTime();
        MapState state = scenario.after();
        Set<Chunk> onlyChunks = chunkSize == 0 ? null : dirtyChunks(scenario, chunkSize);
        Map<Batch, List<Point>> batches = group(state, chunkSize, onlyChunks);
        ClearanceField field = new ClearanceField(state.width(), state.height(), state.solid());
        List<Mesh> meshes = buildMeshes(field, batches.values());
        long elapsed = System.nanoTime() - start;
        long allocated = allocationBean == null
                ? -1 : allocationBean.getThreadAllocatedBytes(threadId) - allocatedBefore;
        long outputBytes = 0;
        for (Mesh mesh : meshes) {
            meshSink += mesh.getTriangleCount();
            outputBytes += (long) mesh.getVertexCount() * 8 * Float.BYTES
                    + (long) mesh.getTriangleCount() * 3 * Integer.BYTES;
            clearMesh(mesh);
        }
        return new Result(elapsed, allocated, outputBytes);
    }

    private static List<Mesh> buildMeshes(ClearanceField field, Iterable<List<Point>> batches) {
        List<Mesh> meshes = new ArrayList<>();
        for (List<Point> tiles : batches) {
            if (!tiles.isEmpty()) {
                meshes.add(Ceiling.createMesh(field, tiles, null, Set.of()));
            }
        }
        return meshes;
    }

    private static Map<Point, PatchData> buildPatchMap(MapState state, int chunkSize) {
        return buildPatchMap(state, chunkSize, null);
    }

    private static Map<Point, PatchData> buildPatchMap(MapState state, int chunkSize, Set<Chunk> onlyChunks) {
        return buildPatchMap(state, chunkSize, onlyChunks, null, Set.of());
    }

    private static Map<Point, PatchData> buildPatchMap(MapState state, int chunkSize, Set<Chunk> onlyChunks,
            TerrainDeformer deformer, Set<Point> straightTiles) {
        Map<Batch, List<Point>> batches = group(state, chunkSize);
        ClearanceField field = new ClearanceField(state.width(), state.height(), state.solid());
        Map<Point, PatchData> patches = new HashMap<>();
        for (Map.Entry<Batch, List<Point>> entry : batches.entrySet()) {
            Batch key = entry.getKey();
            if (onlyChunks != null && !onlyChunks.contains(new Chunk(key.chunkX(), key.chunkY()))) {
                continue;
            }
            Mesh mesh = Ceiling.createMesh(field, entry.getValue(), deformer, straightTiles);
            patches.putAll(readPatches(mesh, entry.getValue()));
            clearMesh(mesh);
        }
        return patches;
    }

    private static TerrainDeformer createBenchmarkDeformer(MapState state, Set<Point> straightTiles) {
        TerrainDeformer deformer = new TerrainDeformer(state.width(), state.height(), (x, y) -> {
            boolean solid = state.solid()[y * state.width() + x];
            return new TerrainDeformer.TileShape(solid, 0.1875f, 0.094f, TEST_LEAN_H, TEST_LEAN_V);
        });
        for (Point point : straightTiles) {
            deformer.setStraight(point.x, point.y, true);
        }
        return deformer;
    }

    private static Map<Point, PatchData> readPatches(Mesh mesh, List<Point> tiles) {
        FloatBuffer positions = ((FloatBuffer) mesh.getBuffer(VertexBuffer.Type.Position).getData()).duplicate();
        FloatBuffer texCoords = ((FloatBuffer) mesh.getBuffer(VertexBuffer.Type.TexCoord).getData()).duplicate();
        FloatBuffer normals = ((FloatBuffer) mesh.getBuffer(VertexBuffer.Type.Normal).getData()).duplicate();
        IntBuffer indices = ((IntBuffer) mesh.getBuffer(VertexBuffer.Type.Index).getData()).duplicate();
        positions.rewind();
        texCoords.rewind();
        normals.rewind();
        indices.rewind();

        Map<Point, PatchData> result = new HashMap<>();
        int vertexOffset = 0;
        int indexOffset = 0;
        for (Point tile : tiles) {
            int base = vertexOffset;
            int splitQuads = 0;
            int patchIndexStart = indexOffset;
            for (int quad = 0; quad < 4; quad++) {
                boolean split = indices.get(indexOffset + 2) >= base + 9;
                int quadIndices = split ? 12 : 6;
                splitQuads += split ? 1 : 0;
                indexOffset += quadIndices;
            }
            int vertexCount = 9 + splitQuads;
            int patchIndexCount = indexOffset - patchIndexStart;
            result.put(tile, new PatchData(copyFloats(positions, base * 3, vertexCount * 3),
                    copyFloats(texCoords, base * 2, vertexCount * 2),
                    copyFloats(normals, base * 3, vertexCount * 3),
                    copyIndices(indices, patchIndexStart, patchIndexCount, base)));
            vertexOffset += vertexCount;
        }
        if (vertexOffset != mesh.getVertexCount() || indexOffset != indices.limit()) {
            throw new IllegalStateException("Could not split the ceiling mesh into tile patches");
        }
        return result;
    }

    private static float[] copyFloats(FloatBuffer source, int start, int length) {
        float[] copy = new float[length];
        FloatBuffer view = source.duplicate();
        view.position(start);
        view.get(copy);
        return copy;
    }

    private static int[] copyIndices(IntBuffer source, int start, int length, int vertexBase) {
        int[] copy = new int[length];
        IntBuffer view = source.duplicate();
        view.position(start);
        view.get(copy);
        for (int i = 0; i < copy.length; i++) {
            copy[i] -= vertexBase;
        }
        return copy;
    }

    private static boolean samePatches(Map<Point, PatchData> expected, Map<Point, PatchData> actual) {
        if (!expected.keySet().equals(actual.keySet())) {
            return false;
        }
        for (Map.Entry<Point, PatchData> entry : expected.entrySet()) {
            if (!entry.getValue().sameAs(actual.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    private static Map<Batch, List<Point>> group(MapState state, int chunkSize) {
        return group(state, chunkSize, null);
    }

    private static Map<Batch, List<Point>> group(MapState state, int chunkSize, Set<Chunk> onlyChunks) {
        Map<Batch, List<Point>> result = new HashMap<>();
        for (int y = 0; y < state.height(); y++) {
            for (int x = 0; x < state.width(); x++) {
                int index = y * state.width() + x;
                if (state.solid()[index] || !state.visible()[index]) {
                    continue;
                }
                int chunkX = chunkSize == 0 ? 0 : x / chunkSize;
                int chunkY = chunkSize == 0 ? 0 : y / chunkSize;
                if (onlyChunks != null && !onlyChunks.contains(new Chunk(chunkX, chunkY))) {
                    continue;
                }
                int material = ((x / 8) + (y / 8)) & 3;
                result.computeIfAbsent(new Batch(chunkX, chunkY, material), ignored -> new ArrayList<>())
                        .add(new Point(x, y));
            }
        }
        return result;
    }

    private static Set<Chunk> dirtyChunks(Scenario scenario, int chunkSize) {
        Comparator<Chunk> order = Comparator.comparingInt(Chunk::y).thenComparingInt(Chunk::x);
        Set<Chunk> result = new TreeSet<>(order);
        int margin = scenario.fieldChanged()
                ? ClearanceField.PATCH_DEPENDENCY_RADIUS + UPDATE_NEIGHBOR_MARGIN : 0;
        for (Point point : scenario.changedTiles()) {
            int minX = Math.max(0, point.x - margin);
            int maxX = Math.min(scenario.after().width() - 1, point.x + margin);
            int minY = Math.max(0, point.y - margin);
            int maxY = Math.min(scenario.after().height() - 1, point.y + margin);
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    result.add(new Chunk(x / chunkSize, y / chunkSize));
                }
            }
        }
        return result;
    }

    private static Chunk chunkOf(Point point, int chunkSize) {
        return new Chunk(point.x / chunkSize, point.y / chunkSize);
    }

    private static Scenario digScenario(String name, int mapWidth, int mapHeight,
            int x, int y, int width, int height) {
        MapState before = baseState(mapWidth, mapHeight);
        boolean[] beforeSolid = before.solid().clone();
        boolean[] afterSolid = before.solid().clone();
        List<Point> changed = new ArrayList<>(width * height);
        for (int yy = y; yy < y + height; yy++) {
            for (int xx = x; xx < x + width; xx++) {
                beforeSolid[yy * mapWidth + xx] = true;
                afterSolid[yy * mapWidth + xx] = false;
                changed.add(new Point(xx, yy));
            }
        }
        return new Scenario(name,
                new MapState(mapWidth, mapHeight, beforeSolid, before.visible().clone()),
                new MapState(mapWidth, mapHeight, afterSolid, before.visible().clone()), changed, true);
    }

    private static Scenario fogRevealScenario(String name, int mapWidth, int mapHeight,
            int x, int y, int width, int height) {
        MapState visible = baseState(mapWidth, mapHeight);
        boolean[] beforeVisible = visible.visible().clone();
        boolean[] afterVisible = visible.visible().clone();
        boolean[] beforeSolid = visible.solid().clone();
        List<Point> changed = new ArrayList<>(width * height);
        for (int yy = y; yy < y + height; yy++) {
            for (int xx = x; xx < x + width; xx++) {
                int index = yy * mapWidth + xx;
                if (!visible.solid()[index]) {
                    beforeVisible[index] = false;
                    beforeSolid[index] = true;
                    changed.add(new Point(xx, yy));
                }
            }
        }
        for (Point point : changed) {
            afterVisible[point.y * mapWidth + point.x] = true;
        }
        return new Scenario(name,
                new MapState(mapWidth, mapHeight, beforeSolid, beforeVisible),
                new MapState(mapWidth, mapHeight, visible.solid().clone(), afterVisible), changed, true);
    }

    private static MapState baseState(int width, int height) {
        boolean[] solid = new boolean[width * height];
        boolean[] visible = new boolean[width * height];
        Arrays.fill(visible, true);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean boundary = x == 0 || y == 0 || x == width - 1 || y == height - 1;
                boolean verticalWall = x % 19 == 0 && y % 23 != 11;
                boolean horizontalWall = y % 17 == 0 && x % 29 != 14;
                solid[y * width + x] = boundary || verticalWall || horizontalWall;
            }
        }
        return new MapState(width, height, solid, visible);
    }

    private static int tileCount(Map<Batch, List<Point>> batches) {
        return batches.values().stream().mapToInt(List::size).sum();
    }

    private static com.sun.management.ThreadMXBean allocationBean() {
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

    private static double medianMillis(long[] values) {
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2] / 1_000_000d;
    }

    private static double medianMiB(long[] values) {
        if (values[0] < 0) {
            return -1;
        }
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2] / (1024d * 1024d);
    }

    private static void clearMesh(Mesh mesh) {
        for (VertexBuffer.Type type : List.of(VertexBuffer.Type.Position, VertexBuffer.Type.TexCoord,
                VertexBuffer.Type.Normal, VertexBuffer.Type.Index)) {
            mesh.clearBuffer(type);
        }
    }

    record Scenario(String name, MapState before, MapState after, List<Point> changedTiles, boolean fieldChanged) {
    }

    record MapState(int width, int height, boolean[] solid, boolean[] visible) {
    }

    private record Batch(int chunkX, int chunkY, int material) {
    }

    private record Chunk(int x, int y) {
    }

    private record Result(long elapsedNanos, long allocatedBytes, long outputBytes) {
    }

    private record PatchData(float[] positions, float[] texCoords, float[] normals, int[] indices) {

        boolean sameAs(PatchData other) {
            return other != null && Arrays.equals(positions, other.positions)
                    && Arrays.equals(texCoords, other.texCoords)
                    && Arrays.equals(normals, other.normals)
                    && Arrays.equals(indices, other.indices);
        }
    }
}
