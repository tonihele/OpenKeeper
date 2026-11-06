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
package toniarts.openkeeper.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrameTimeStatsTest {

    @Test
    void emptyStatsAreZero() {
        FrameTimeStats stats = new FrameTimeStats();
        Map<String, Object> summary = stats.summarize();
        assertEquals(0, summary.get("frames"));
        assertEquals(0.0, (Double) summary.get("avgFps"), 1e-9);
    }

    @Test
    void constantFrameTimeYieldsExpectedFps() {
        FrameTimeStats stats = new FrameTimeStats();
        for (int i = 0; i < 100; i++) {
            stats.addFrameTimeMs(10.0); // 100 FPS
        }
        Map<String, Object> summary = stats.summarize();
        assertEquals(100.0, (Double) summary.get("avgFps"), 0.01);
        assertEquals(100.0, (Double) summary.get("onePercentLowFps"), 0.01);
        assertEquals(100.0, (Double) summary.get("pointOnePercentLowFps"), 0.01);
    }

    @Test
    void onePercentLowUsesWorstFrames() {
        List<Double> times = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            times.add(10.0);
        }
        // One terrible frame (50 ms → 20 FPS) among 100 → 1% low ≈ 20 FPS
        times.set(50, 50.0);
        times.sort(Double::compareTo);
        assertEquals(20.0, FrameTimeStats.lowPercentFps(times, 0.01), 0.01);
    }

    @Test
    void percentileIsInterpolated() {
        List<Double> values = List.of(1.0, 2.0, 3.0, 4.0);
        assertEquals(1.0, FrameTimeStats.percentile(values, 0.0), 1e-9);
        assertEquals(4.0, FrameTimeStats.percentile(values, 1.0), 1e-9);
        assertEquals(2.5, FrameTimeStats.percentile(values, 0.5), 1e-9);
    }

    @Test
    void ignoresNonPositiveSamples() {
        FrameTimeStats stats = new FrameTimeStats();
        stats.addFrameTimeMs(0);
        stats.addFrameTimeMs(-1);
        stats.addFrameTimeMs(Double.NaN);
        stats.addFrameTimeMs(16.0);
        assertEquals(1, stats.getFrameCount());
        assertTrue((Double) stats.summarize().get("avgFps") > 60.0);
    }
}
