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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.view.map;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Counters for the tile/room view rebuild pipeline. Reset at benchmark
 * measure start; sampled into suite metrics so rebuild storms are visible
 * as rate/duration rather than only as frame-time noise.
 */
public final class TileRebuildStats {

    private final AtomicLong geometryRebuildCalls = new AtomicLong();
    private final AtomicLong geometryTilesRebuilt = new AtomicLong();
    private final AtomicLong geometryInputPoints = new AtomicLong();
    private final AtomicLong geometryNanos = new AtomicLong();
    private final AtomicLong overlayRefreshCalls = new AtomicLong();
    private final AtomicLong overlayTilesTouched = new AtomicLong();
    private final AtomicLong overlayNanos = new AtomicLong();
    private final AtomicLong ceilingRebuilds = new AtomicLong();
    private final AtomicLong ceilingSkippedHidden = new AtomicLong();
    private final AtomicLong ceilingNanos = new AtomicLong();
    private final AtomicLong coalesceFlushes = new AtomicLong();
    private final AtomicLong coalescePointsIn = new AtomicLong();
    private final AtomicLong coalesceUniquePoints = new AtomicLong();
    private final AtomicLong geometryWithRoomRebuild = new AtomicLong();
    private final AtomicLong geometryWithoutRoomRebuild = new AtomicLong();

    public void reset() {
        geometryRebuildCalls.set(0);
        geometryTilesRebuilt.set(0);
        geometryInputPoints.set(0);
        geometryNanos.set(0);
        overlayRefreshCalls.set(0);
        overlayTilesTouched.set(0);
        overlayNanos.set(0);
        ceilingRebuilds.set(0);
        ceilingSkippedHidden.set(0);
        ceilingNanos.set(0);
        coalesceFlushes.set(0);
        coalescePointsIn.set(0);
        coalesceUniquePoints.set(0);
        geometryWithRoomRebuild.set(0);
        geometryWithoutRoomRebuild.set(0);
    }

    public void recordGeometryRebuild(int inputPoints, int tilesRebuilt, long nanos, boolean rebuiltRooms) {
        geometryRebuildCalls.incrementAndGet();
        geometryInputPoints.addAndGet(inputPoints);
        geometryTilesRebuilt.addAndGet(tilesRebuilt);
        geometryNanos.addAndGet(nanos);
        if (rebuiltRooms) {
            geometryWithRoomRebuild.incrementAndGet();
        } else {
            geometryWithoutRoomRebuild.incrementAndGet();
        }
    }

    public void recordOverlayRefresh(int tilesTouched, long nanos) {
        overlayRefreshCalls.incrementAndGet();
        overlayTilesTouched.addAndGet(tilesTouched);
        overlayNanos.addAndGet(nanos);
    }

    public void recordCeilingRebuild(long nanos) {
        ceilingRebuilds.incrementAndGet();
        ceilingNanos.addAndGet(nanos);
    }

    public void recordCeilingSkippedHidden() {
        ceilingSkippedHidden.incrementAndGet();
    }

    public void recordCoalesceFlush(int pointsIn, int uniquePoints) {
        coalesceFlushes.incrementAndGet();
        coalescePointsIn.addAndGet(pointsIn);
        coalesceUniquePoints.addAndGet(uniquePoints);
    }

    public Map<String, Object> summarize() {
        Map<String, Object> out = new LinkedHashMap<>();
        long geomCalls = geometryRebuildCalls.get();
        long overlayCalls = overlayRefreshCalls.get();
        long ceilingCalls = ceilingRebuilds.get();
        long coalesceCalls = coalesceFlushes.get();
        out.put("geometryRebuildCalls", geomCalls);
        out.put("geometryWithRoomRebuild", geometryWithRoomRebuild.get());
        out.put("geometryWithoutRoomRebuild", geometryWithoutRoomRebuild.get());
        out.put("geometryTilesRebuilt", geometryTilesRebuilt.get());
        out.put("geometryInputPoints", geometryInputPoints.get());
        out.put("geometryTotalMs", ms(geometryNanos.get()));
        out.put("geometryAvgMs", geomCalls > 0 ? ms(geometryNanos.get() / geomCalls) : 0.0);
        out.put("overlayRefreshCalls", overlayCalls);
        out.put("overlayTilesTouched", overlayTilesTouched.get());
        out.put("overlayTotalMs", ms(overlayNanos.get()));
        out.put("overlayAvgMs", overlayCalls > 0 ? ms(overlayNanos.get() / overlayCalls) : 0.0);
        out.put("ceilingRebuilds", ceilingCalls);
        out.put("ceilingSkippedHidden", ceilingSkippedHidden.get());
        out.put("ceilingTotalMs", ms(ceilingNanos.get()));
        out.put("ceilingAvgMs", ceilingCalls > 0 ? ms(ceilingNanos.get() / ceilingCalls) : 0.0);
        out.put("coalesceFlushes", coalesceCalls);
        out.put("coalescePointsIn", coalescePointsIn.get());
        out.put("coalesceUniquePoints", coalesceUniquePoints.get());
        long pointsIn = coalescePointsIn.get();
        long unique = coalesceUniquePoints.get();
        out.put("coalesceRedundantPointsEliminated", Math.max(0L, pointsIn - unique));
        return out;
    }

    private static double ms(long nanos) {
        return Math.round((nanos / 1_000_000.0) * 1000.0) / 1000.0;
    }
}
