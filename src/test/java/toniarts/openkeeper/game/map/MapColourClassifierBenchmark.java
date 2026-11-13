/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.game.map;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;

/**
 * Paired benchmark for full map-colour-grid classification, comparing the
 * pre-change owned-tile path with the production classifier in the same JVM.
 */
public final class MapColourClassifierBenchmark {

    private static final int WIDTH = 85;
    private static final int HEIGHT = 85;
    private static final int WARMUP_BATCHES = 6;
    private static final int MEASURED_BATCHES = 9;
    private static final int RECOMPUTES_PER_BATCH = 4;
    private static volatile long checksumSink;

    private MapColourClassifierBenchmark() {
    }

    public static void main(String[] args) {
        Fixture fixture = new Fixture();
        MapColourClassifier candidate = new MapColourClassifier(fixture.mapInformation,
                IFogOfWarInformation.ALWAYS_VISIBLE, null);
        short[] baselineOutput = new short[WIDTH * HEIGHT];
        short[] candidateOutput = new short[WIDTH * HEIGHT];
        com.sun.management.ThreadMXBean allocationBean = allocationBean();

        for (int batch = 0; batch < WARMUP_BATCHES; batch++) {
            if ((batch & 1) == 0) {
                runBatch(fixture, null, baselineOutput, allocationBean);
                runBatch(fixture, candidate, candidateOutput, allocationBean);
            } else {
                runBatch(fixture, candidate, candidateOutput, allocationBean);
                runBatch(fixture, null, baselineOutput, allocationBean);
            }
        }

        long[] baselineNanos = new long[MEASURED_BATCHES];
        long[] candidateNanos = new long[MEASURED_BATCHES];
        long[] baselineAllocated = new long[MEASURED_BATCHES];
        long[] candidateAllocated = new long[MEASURED_BATCHES];
        long baselineChecksum = 0;
        long candidateChecksum = 0;
        for (int batch = 0; batch < MEASURED_BATCHES; batch++) {
            BatchResult baseline;
            BatchResult updated;
            if ((batch & 1) == 0) {
                baseline = runBatch(fixture, null, baselineOutput, allocationBean);
                updated = runBatch(fixture, candidate, candidateOutput, allocationBean);
            } else {
                updated = runBatch(fixture, candidate, candidateOutput, allocationBean);
                baseline = runBatch(fixture, null, baselineOutput, allocationBean);
            }
            if (baseline.checksum() != updated.checksum()
                    || !Arrays.equals(baselineOutput, candidateOutput)) {
                throw new IllegalStateException("Map-colour output mismatch");
            }
            baselineNanos[batch] = baseline.elapsedNanos();
            candidateNanos[batch] = updated.elapsedNanos();
            baselineAllocated[batch] = baseline.allocatedBytes();
            candidateAllocated[batch] = updated.allocatedBytes();
            baselineChecksum = baseline.checksum();
            candidateChecksum = updated.checksum();
            checksumSink ^= baselineChecksum ^ candidateChecksum;
        }

        double baselineMs = median(baselineNanos) / 1_000_000d / RECOMPUTES_PER_BATCH;
        double candidateMs = median(candidateNanos) / 1_000_000d / RECOMPUTES_PER_BATCH;
        double baselineKiB = median(baselineAllocated) / 1024d / RECOMPUTES_PER_BATCH;
        double candidateKiB = median(candidateAllocated) / 1024d / RECOMPUTES_PER_BATCH;
        System.out.printf(Locale.ROOT,
                "map=%dx%d owner=keeper1 tiles=%d recomputesPerBatch=%d checksumEqual=%s "
                        + "medianClassifyMs=%.4f->%.4f deltaPct=%.1f allocatedKiB=%.2f->%.2f deltaPct=%.1f%n",
                WIDTH, HEIGHT, WIDTH * HEIGHT, RECOMPUTES_PER_BATCH,
                baselineChecksum == candidateChecksum, baselineMs, candidateMs,
                100d * (candidateMs - baselineMs) / baselineMs, baselineKiB, candidateKiB,
                baselineKiB == 0 ? 0 : 100d * (candidateKiB - baselineKiB) / baselineKiB);
    }

    private static BatchResult runBatch(Fixture fixture, MapColourClassifier classifier,
            short[] output, com.sun.management.ThreadMXBean allocationBean) {
        long threadId = Thread.currentThread().threadId();
        long allocatedBefore = allocationBean == null ? -1 : allocationBean.getThreadAllocatedBytes(threadId);
        long started = System.nanoTime();
        long checksum = 0xcbf29ce484222325L;
        for (int repeat = 0; repeat < RECOMPUTES_PER_BATCH; repeat++) {
            for (int y = 0; y < HEIGHT; y++) {
                int row = y * WIDTH;
                for (int x = 0; x < WIDTH; x++) {
                    short value = classifier == null
                            ? classifyBaseline(fixture, x, y)
                            : classifier.classify(x, y);
                    output[row + x] = value;
                    checksum = (checksum ^ value) * 0x100000001b3L;
                }
            }
        }
        long elapsed = System.nanoTime() - started;
        long allocated = allocationBean == null
                ? -1 : allocationBean.getThreadAllocatedBytes(threadId) - allocatedBefore;
        return new BatchResult(elapsed, allocated, checksum);
    }

    /** Baseline logic for this benchmark's owned floor/rock fixture. */
    private static short classifyBaseline(Fixture fixture, int x, int y) {
        FakeMapTile tile = fixture.mapData.getTile(x, y);
        if (tile == null) {
            return MapColourClass.UNEXPLORED_OR_IMPENETRABLE;
        }
        Point point = new Point(x, y);
        Terrain terrain = fixture.mapInformation.getTerrain(tile);
        if (!(IFogOfWarInformation.ALWAYS_VISIBLE.isPerceived(point)
                && terrain.getFlags().contains(Terrain.TerrainFlag.REVEAL_THROUGH_FOG_OF_WAR))
                && !IFogOfWarInformation.ALWAYS_VISIBLE.isExplored(point)) {
            return MapColourClass.UNEXPLORED_OR_IMPENETRABLE;
        }
        if (tile.getRoomId() != null) {
            throw new IllegalStateException("The owned floor/rock fixture must not contain rooms");
        }
        short owner = tile.getOwnerId();
        if (owner != Player.NEUTRAL_PLAYER_ID) {
            boolean solid = fixture.mapInformation.isSolid(point);
            short base = solid ? MapColourClass.OWNED_SOLID_BASE : MapColourClass.OWNED_FLOOR_BASE;
            return (short) (base + PlayerNumbers.playerNumber(owner));
        }
        throw new IllegalStateException("The owned floor/rock fixture must be fully owned");
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

    private record BatchResult(long elapsedNanos, long allocatedBytes, long checksum) {
    }

    private static final class Fixture {

        private static final short FLOOR_ID = 1;
        private static final short ROCK_ID = 2;
        private static final Terrain FLOOR = TerrainFixtures.terrain(FLOOR_ID,
                EnumSet.noneOf(Terrain.TerrainFlag.class), 0, 0, 0, FLOOR_ID);
        private static final Terrain ROCK = TerrainFixtures.terrain(ROCK_ID,
                TerrainFixtures.flags(Terrain.TerrainFlag.SOLID), 0, 0, 0, ROCK_ID);

        private final FakeMapData mapData = new FakeMapData(WIDTH, HEIGHT);
        private final FakeMapInformation mapInformation;

        private Fixture() {
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    FakeMapTile tile = mapData.getTile(x, y);
                    boolean solid = (x * 31 + y * 17) % 5 < 2;
                    tile.terrainId = solid ? ROCK_ID : FLOOR_ID;
                    tile.ownerId = Player.KEEPER1_ID;
                }
            }
            mapInformation = new FakeMapInformation(mapData, Map.of(FLOOR_ID, FLOOR, ROCK_ID, ROCK));
        }
    }
}
