/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.fogofwar;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import toniarts.openkeeper.game.fogofwar.FogOfWarRules;
import toniarts.openkeeper.game.fogofwar.FogState;
import toniarts.openkeeper.game.fogofwar.IBlocksSight;
import toniarts.openkeeper.game.fogofwar.LineOfSightTable;
import toniarts.openkeeper.utils.Point;

/** Compares the production LOS ring with Point-keyed and direct-grid door indexes. */
public final class FogDoorLookupBenchmark {

    private static final int WIDTH = 85;
    private static final int HEIGHT = 85;
    private static final int WARMUP_PAIRS = 20;
    private static final int MEASURED_PAIRS = 21;
    private static final int[] CREATURE_COUNTS = {16, 64, 128};
    private static volatile long sink;

    private FogDoorLookupBenchmark() {
    }

    public static void main(String[] args) {
        boolean[] solid = createSolidMap();
        List<DoorCell> doors = createDoors(solid);
        LineOfSightTable losTable = LineOfSightTable.generate(7);
        System.out.printf(Locale.ROOT,
                "map=%dx%d java=%s doors=%d offsets=%d warmupPairs=%d measuredPairs=%d%n",
                WIDTH, HEIGHT, System.getProperty("java.version"), doors.size(), losTable.getOffsets().size(),
                WARMUP_PAIRS, MEASURED_PAIRS);

        for (int creatureCount : CREATURE_COUNTS) {
            Scenario scenario = new Scenario(solid, doors, losTable, createCreatures(solid, creatureCount));
            if (!scenario.verifyParity()) {
                throw new IllegalStateException("Point and grid door lookups changed LOS for " + creatureCount + " creatures");
            }
            benchmark(scenario, creatureCount);
        }
    }

    private static void benchmark(Scenario scenario, int creatureCount) {
        FogState pointState = new FogState((short) 0, WIDTH, HEIGHT);
        FogState gridState = new FogState((short) 0, WIDTH, HEIGHT);
        scenario.runPointLookup(pointState);
        scenario.runGridLookup(gridState);
        pointState.drainDirtyTiles();
        gridState.drainDirtyTiles();

        long[] pointNanos = new long[MEASURED_PAIRS];
        long[] gridNanos = new long[MEASURED_PAIRS];
        long[] pointBytes = new long[MEASURED_PAIRS];
        long[] gridBytes = new long[MEASURED_PAIRS];
        for (int i = 0; i < WARMUP_PAIRS; i++) {
            measurePoint(scenario, pointState);
            measureGrid(scenario, gridState);
        }
        for (int i = 0; i < MEASURED_PAIRS; i++) {
            Measurement point;
            Measurement grid;
            if ((i & 1) == 0) {
                point = measurePoint(scenario, pointState);
                grid = measureGrid(scenario, gridState);
            } else {
                grid = measureGrid(scenario, gridState);
                point = measurePoint(scenario, pointState);
            }
            pointNanos[i] = point.elapsedNanos();
            gridNanos[i] = grid.elapsedNanos();
            pointBytes[i] = point.allocatedBytes();
            gridBytes[i] = grid.allocatedBytes();
        }
        double pointMedian = medianMillis(pointNanos);
        double gridMedian = medianMillis(gridNanos);
        System.out.printf(Locale.ROOT,
                "creatures=%d pointLookupMedianMs=%.3f gridLookupMedianMs=%.3f timeReductionPct=%.1f allocatedKiB=%.1f->%.1f%n",
                creatureCount, pointMedian, gridMedian, 100d * (pointMedian - gridMedian) / pointMedian,
                medianKiB(pointBytes), medianKiB(gridBytes));
    }

    private static Measurement measurePoint(Scenario scenario, FogState state) {
        return measure(() -> scenario.runPointLookup(state));
    }

    private static Measurement measureGrid(Scenario scenario, FogState state) {
        return measure(() -> scenario.runGridLookup(state));
    }

    private static Measurement measure(Runnable pass) {
        com.sun.management.ThreadMXBean bean = allocationBean();
        long threadId = Thread.currentThread().threadId();
        long allocatedBefore = bean == null ? -1 : bean.getThreadAllocatedBytes(threadId);
        long start = System.nanoTime();
        pass.run();
        long elapsed = System.nanoTime() - start;
        long allocated = bean == null ? -1 : bean.getThreadAllocatedBytes(threadId) - allocatedBefore;
        sink++;
        return new Measurement(elapsed, allocated);
    }

    private static long median(long[] values) {
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    private static double medianMillis(long[] values) {
        return median(values) / 1_000_000d;
    }

    private static double medianKiB(long[] values) {
        long bytes = median(values);
        return bytes < 0 ? -1 : bytes / 1024d;
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

    private static List<DoorCell> createDoors(boolean[] solid) {
        List<DoorCell> result = new ArrayList<>();
        for (int y = 8; y < HEIGHT - 8; y += 17) {
            for (int x = 8; x < WIDTH - 8; x += 19) {
                if (!solid[y * WIDTH + x]) {
                    result.add(new DoorCell(x, y, (short) ((x + y) % 3 == 0 ? 2 : 1)));
                }
            }
        }
        return result;
    }

    private static List<Creature> createCreatures(boolean[] solid, int count) {
        List<Creature> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int x = 5 + (i * 17) % (WIDTH - 10);
            int y = 5 + (i * 31) % (HEIGHT - 10);
            if (solid[y * WIDTH + x]) {
                outer:
                for (int radius = 1; radius < 8; radius++) {
                    for (int dy = -radius; dy <= radius; dy++) {
                        for (int dx = -radius; dx <= radius; dx++) {
                            int xx = x + dx;
                            int yy = y + dy;
                            if (!solid[yy * WIDTH + xx]) {
                                x = xx;
                                y = yy;
                                break outer;
                            }
                        }
                    }
                }
            }
            result.add(new Creature(x, y));
        }
        return result;
    }

    private record Creature(int x, int y) {
    }

    private record DoorCell(int x, int y, short doorId) {
    }

    private record Measurement(long elapsedNanos, long allocatedBytes) {
    }

    private static final class Scenario {

        private final boolean[] solid;
        private final List<DoorCell> doors;
        private final LineOfSightTable losTable;
        private final List<Creature> creatures;
        private final Map<Short, Boolean> doorDefinitions = Map.of((short) 1, true, (short) 2, false);
        private final Map<Point, DoorCell> pointDoors = new HashMap<>();
        private final boolean[] gridDoors = new boolean[WIDTH * HEIGHT];
        private int[] gridDoorIndices = new int[16];
        private int gridDoorCount;

        private Scenario(boolean[] solid, List<DoorCell> doors, LineOfSightTable losTable, List<Creature> creatures) {
            this.solid = solid;
            this.doors = doors;
            this.losTable = losTable;
            this.creatures = creatures;
        }

        private void runPointLookup(FogState state) {
            pointDoors.clear();
            for (DoorCell door : doors) {
                pointDoors.put(new Point(door.x(), door.y()), door);
            }
            for (Creature creature : creatures) {
                FogOfWarRules.exploreVisionRing(state, this::blocksSightPoint,
                        losTable, creature.x(), creature.y(), 7);
            }
            state.drainDirtyTiles();
        }

        private void runGridLookup(FogState state) {
            for (int i = 0; i < gridDoorCount; i++) {
                gridDoors[gridDoorIndices[i]] = false;
            }
            gridDoorCount = 0;
            for (DoorCell door : doors) {
                if (gridDoorCount == gridDoorIndices.length) {
                    gridDoorIndices = Arrays.copyOf(gridDoorIndices, gridDoorIndices.length * 2);
                }
                int index = door.y() * WIDTH + door.x();
                gridDoorIndices[gridDoorCount++] = index;
                gridDoors[index] = doorDefinitions.get(door.doorId());
            }
            for (Creature creature : creatures) {
                FogOfWarRules.exploreVisionRing(state, this::blocksSightGrid,
                        losTable, creature.x(), creature.y(), 7);
            }
            state.drainDirtyTiles();
        }

        private boolean blocksSightPoint(int x, int y) {
            if (x < 0 || y < 0 || x >= WIDTH || y >= HEIGHT) {
                return true;
            }
            int index = y * WIDTH + x;
            if (solid[index]) {
                return true;
            }
            DoorCell door = pointDoors.get(new Point(x, y));
            return door != null && doorDefinitions.get(door.doorId());
        }

        private boolean blocksSightGrid(int x, int y) {
            if (x < 0 || y < 0 || x >= WIDTH || y >= HEIGHT) {
                return true;
            }
            int index = y * WIDTH + x;
            return solid[index] || gridDoors[index];
        }

        private boolean verifyParity() {
            FogState pointState = new FogState((short) 0, WIDTH, HEIGHT);
            FogState gridState = new FogState((short) 0, WIDTH, HEIGHT);
            runPointLookup(pointState);
            runGridLookup(gridState);
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    if (pointState.isExplored(x, y) != gridState.isExplored(x, y)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
