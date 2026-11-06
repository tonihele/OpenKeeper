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

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenchmarkConfigTest {

    @Test
    void absentFlagDisablesBenchmark() {
        assertFalse(BenchmarkConfig.fromParams(Map.of()).isEnabled());
    }

    @Test
    void flagWithoutValueUsesDefaults() {
        Map<String, String> params = new HashMap<>();
        params.put("benchmark", null);
        BenchmarkConfig config = BenchmarkConfig.fromParams(params);
        assertTrue(config.isEnabled());
        assertEquals(BenchmarkConfig.DEFAULT_LEVEL, config.getLevel());
        assertEquals(BenchmarkConfig.DEFAULT_SEED, config.getSeed());
        assertFalse(config.isSuite());
    }

    @Test
    void suiteModeEnablesVisualAndLongerDefaults() {
        Map<String, String> params = new HashMap<>();
        params.put("benchmark-suite", "out.json");
        BenchmarkConfig config = BenchmarkConfig.fromParams(params);
        assertTrue(config.isEnabled());
        assertTrue(config.isSuite());
        assertTrue(config.isVisual());
        assertEquals(BenchmarkConfig.SUITE_DURATION_SECONDS, config.getDurationSeconds());
        assertEquals(BenchmarkConfig.SUITE_WARMUP_SECONDS, config.getWarmupSeconds());
        assertEquals(1920, config.getWidth());
        assertEquals(1080, config.getHeight());
    }

    @Test
    void workloadProfileOverridesLevelAndDensity() {
        Map<String, String> params = new HashMap<>();
        params.put("benchmark-suite", "out.json");
        params.put("benchmark-workload", "caverns-complex");
        BenchmarkConfig config = BenchmarkConfig.fromParams(params);
        assertEquals("Caverns", config.getLevel());
        assertEquals(WorkloadProfile.CAVERNS_COMPLEX.getCreatureCount(), config.getCreatureCount());
        assertEquals("caverns-complex", config.getWorkloadId());
    }

    @Test
    void dedicatedOptionsAreParsed() {
        Map<String, String> params = new HashMap<>();
        params.put("benchmark", "build/bench.json");
        params.put("benchmark-level", "Olympia");
        params.put("benchmark-seed", "99");
        params.put("benchmark-warmup", "2.5");
        params.put("benchmark-duration", "12");
        params.put("benchmark-settle", "1");
        params.put("benchmark-creatures", "80");
        params.put("benchmark-imps", "20");
        params.put("headless", null);
        BenchmarkConfig config = BenchmarkConfig.fromParams(params);
        assertEquals("Olympia", config.getLevel());
        assertEquals(99L, config.getSeed());
        assertEquals(2.5, config.getWarmupSeconds());
        assertEquals(12.0, config.getDurationSeconds());
        assertEquals(1.0, config.getSettleSeconds());
        assertEquals(80, config.getCreatureCount());
        assertEquals(20, config.getImpCount());
        assertTrue(config.isHeadless());
        assertFalse(config.isVisual());
    }
}
