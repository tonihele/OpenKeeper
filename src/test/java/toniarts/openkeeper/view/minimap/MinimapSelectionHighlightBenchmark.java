/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.minimap;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.game.map.FakeMapInformation;
import toniarts.openkeeper.game.map.FakeMapTile;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.map.MapColourClassifier;
import toniarts.openkeeper.game.map.MapColourGrid;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;

/**
 * Paired benchmark for the minimap selection and pending-tag overlay scan.
 */
public final class MinimapSelectionHighlightBenchmark {

    private static final int WIDTH = 85;
    private static final int HEIGHT = 85;
    private static final int WARMUP_BATCHES = 6;
    private static final int MEASURED_BATCHES = 9;
    private static volatile int checksumSink;

    private MinimapSelectionHighlightBenchmark() {
    }

    public static void main(String[] args) {
        runScenario("clear", 0, 0);
        runScenario("sparse", 137, 89);
        runScenario("dense", 8, 11);
    }

    private static void runScenario(String name, int selectionStride, int pendingStride) {
        Fixture fixture = new Fixture(selectionStride, pendingStride);
        com.sun.management.ThreadMXBean allocationBean = allocationBean();

        for (int batch = 0; batch < WARMUP_BATCHES; batch++) {
            if ((batch & 1) == 0) {
                runBatch(fixture, true, allocationBean);
                runBatch(fixture, false, allocationBean);
            } else {
                runBatch(fixture, false, allocationBean);
                runBatch(fixture, true, allocationBean);
            }
        }

        long[] baselineNanos = new long[MEASURED_BATCHES];
        long[] candidateNanos = new long[MEASURED_BATCHES];
        long[] baselineAllocated = new long[MEASURED_BATCHES];
        long[] candidateAllocated = new long[MEASURED_BATCHES];
        int outputChecksum = 0;
        for (int batch = 0; batch < MEASURED_BATCHES; batch++) {
            BatchResult baseline;
            BatchResult candidate;
            if ((batch & 1) == 0) {
                baseline = runBatch(fixture, true, allocationBean);
                candidate = runBatch(fixture, false, allocationBean);
            } else {
                candidate = runBatch(fixture, false, allocationBean);
                baseline = runBatch(fixture, true, allocationBean);
            }
            if (!Arrays.equals(baseline.output(), candidate.output())) {
                throw new IllegalStateException("Minimap highlight output mismatch: " + name);
            }
            baselineNanos[batch] = baseline.elapsedNanos();
            candidateNanos[batch] = candidate.elapsedNanos();
            baselineAllocated[batch] = baseline.allocatedBytes();
            candidateAllocated[batch] = candidate.allocatedBytes();
            outputChecksum = Arrays.hashCode(candidate.output());
            checksumSink ^= outputChecksum;
        }

        double baselineMs = median(baselineNanos) / 1_000_000d;
        double candidateMs = median(candidateNanos) / 1_000_000d;
        double baselineKiB = median(baselineAllocated) / 1024d;
        double candidateKiB = median(candidateAllocated) / 1024d;
        System.out.printf(Locale.ROOT,
                "scenario=%s map=%dx%d selectedStride=%d pendingStride=%d checksum=%d "
                        + "medianScanMs=%.4f->%.4f deltaPct=%.1f allocatedKiB=%.2f->%.2f deltaPct=%.1f%n",
                name, WIDTH, HEIGHT, selectionStride, pendingStride, outputChecksum,
                baselineMs, candidateMs, 100d * (candidateMs - baselineMs) / baselineMs,
                baselineKiB, candidateKiB,
                baselineKiB == 0 ? 0 : 100d * (candidateKiB - baselineKiB) / baselineKiB);
    }

    private static BatchResult runBatch(Fixture fixture, boolean baseline,
            com.sun.management.ThreadMXBean allocationBean) {
        MapColourGrid grid = fixture.newGrid();
        long threadId = Thread.currentThread().threadId();
        long allocatedBefore = allocationBean == null ? -1 : allocationBean.getThreadAllocatedBytes(threadId);
        long started = System.nanoTime();
        if (baseline) {
            applyBaseline(grid, fixture);
        } else {
            MinimapSelectionHighlighter.apply(grid, fixture.mapInformation, fixture.fog, (short) 1);
        }
        long elapsed = System.nanoTime() - started;
        long allocated = allocationBean == null
                ? -1 : allocationBean.getThreadAllocatedBytes(threadId) - allocatedBefore;
        return new BatchResult(elapsed, allocated, grid.colourClasses());
    }

    /** The original point-based scan from MinimapPanelState. */
    private static void applyBaseline(MapColourGrid grid, Fixture fixture) {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                Point point = new Point(x, y);
                if (fixture.mapInformation.isSelected(point, (short) 1)
                        || fixture.fog.isPendingTagged(point)) {
                    grid.setHighlight(x, y, true);
                }
            }
        }
    }

    private static com.sun.management.ThreadMXBean allocationBean() {
        java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        if (bean instanceof com.sun.management.ThreadMXBean allocationBean
                && allocationBean.isThreadAllocatedMemorySupported()) {
            if (!allocationBean.isThreadAllocatedMemoryEnabled()) {
                allocationBean.setThreadAllocatedMemoryEnabled(true);
            }
            return allocationBean;
        }
        return null;
    }

    private static long median(long[] values) {
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    private record BatchResult(long elapsedNanos, long allocatedBytes, short[] output) {
    }

    private static final class Fixture {

        private static final short FLOOR_ID = 1;
        private static final short REVEAL_ID = 2;
        private static final Terrain FLOOR = TerrainFixtures.terrain(FLOOR_ID,
                TerrainFixtures.flags(Terrain.TerrainFlag.TAGGABLE), 0, 0, 0, FLOOR_ID);
        private static final Terrain REVEAL = TerrainFixtures.terrain(REVEAL_ID,
                TerrainFixtures.flags(Terrain.TerrainFlag.TAGGABLE,
                        Terrain.TerrainFlag.REVEAL_THROUGH_FOG_OF_WAR), 0, 0, 0, REVEAL_ID);
        private static final Map<Short, Terrain> TERRAINS = Map.of(FLOOR_ID, FLOOR, REVEAL_ID, REVEAL);

        private final FakeMapData mapData = new FakeMapData(WIDTH, HEIGHT);
        private final FakeMapInformation mapInformation = new FakeMapInformation(mapData, TERRAINS);
        private final BenchmarkFog fog = new BenchmarkFog(mapData, TERRAINS);

        private Fixture(int selectionStride, int pendingStride) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    int index = y * WIDTH + x;
                    FakeMapTile tile = mapData.getTile(x, y);
                    tile.terrainId = index % 3 == 0 ? REVEAL_ID : FLOOR_ID;
                    tile.selected = selectionStride > 0 && index % selectionStride == 0;
                    fog.explored[index] = (index * 13) % 17 < 9;
                    fog.perceived[index] = (index * 7) % 19 < 5;
                    if (pendingStride > 0 && index % pendingStride == 0 && !fog.isVisible(tile)) {
                        fog.pendingTagged.add(tile.getLocation());
                    }
                }
            }
        }

        private MapColourGrid newGrid() {
            MapColourClassifier classifier = new MapColourClassifier(mapInformation, fog, null);
            return new MapColourGrid(WIDTH, HEIGHT, classifier, fog);
        }
    }

    private static final class BenchmarkFog implements IFogOfWarInformation {

        private final FakeMapData mapData;
        private final Map<Short, Terrain> terrains;
        private final boolean[] explored = new boolean[WIDTH * HEIGHT];
        private final boolean[] perceived = new boolean[WIDTH * HEIGHT];
        private final Set<Point> pendingTagged = new HashSet<>();

        private BenchmarkFog(FakeMapData mapData, Map<Short, Terrain> terrains) {
            this.mapData = mapData;
            this.terrains = terrains;
        }

        @Override
        public boolean isVisible(Point point) {
            FakeMapTile tile = mapData.getTile(point.x, point.y);
            return tile != null && isVisible(tile);
        }

        @Override
        public boolean isExplored(Point point) {
            return explored[index(point.x, point.y)];
        }

        @Override
        public boolean isPerceived(Point point) {
            return perceived[index(point.x, point.y)];
        }

        @Override
        public boolean isHighlightable(Point point) {
            FakeMapTile tile = mapData.getTile(point.x, point.y);
            return tile != null && isHighlightable(tile);
        }

        @Override
        public boolean isHighlightable(IMapTileInformation tile) {
            if (tile == null) {
                return false;
            }
            if (!explored[index(tile.getX(), tile.getY())]) {
                return true;
            }
            return terrains.get(tile.getTerrainId()).getFlags().contains(Terrain.TerrainFlag.TAGGABLE);
        }

        @Override
        public boolean isPendingTagged(Point point) {
            return !isVisible(point) && pendingTagged.contains(point);
        }

        @Override
        public boolean isPendingTagged(IMapTileInformation tile) {
            return tile != null && !isVisible(tile) && pendingTagged.contains(tile.getLocation());
        }

        private boolean isVisible(IMapTileInformation tile) {
            Terrain terrain = terrains.get(tile.getTerrainId());
            boolean revealThrough = terrain.getFlags().contains(Terrain.TerrainFlag.REVEAL_THROUGH_FOG_OF_WAR);
            int index = index(tile.getX(), tile.getY());
            return explored[index] || perceived[index] && revealThrough;
        }

        private static int index(int x, int y) {
            return y * WIDTH + x;
        }
    }
}
