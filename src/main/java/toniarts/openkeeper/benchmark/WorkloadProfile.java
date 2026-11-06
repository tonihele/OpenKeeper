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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Named late-game workloads. {@link #DEFAULT_SUITE} is the fast path used by
 * {@code benchmarkBaseline}; {@link #FULL_SUITE} is optional coverage.
 */
public final class WorkloadProfile {

    private static final double DEFAULT_MEASURE_SEC = BenchmarkConfig.SUITE_DURATION_SECONDS;

    public static final WorkloadProfile LATE_CAMPAIGN_DENSE = new WorkloadProfile(
            "late-campaign-dense", "Level20", 100, 40, DEFAULT_MEASURE_SEC,
            "Late campaign map with dense combat + worker population");

    public static final WorkloadProfile CAVERNS_COMPLEX = new WorkloadProfile(
            "caverns-complex", "Caverns", 90, 35, DEFAULT_MEASURE_SEC,
            "Large excavated cavern network with heavy room/tunnel topology");

    public static final WorkloadProfile PRESSURE_SKIRMISH = new WorkloadProfile(
            "pressure-skirmish", "Pressure", 100, 40, DEFAULT_MEASURE_SEC,
            "Skirmish pressure map stressing AI pathing and combat clusters");

    /** Fast default: one representative late-game map (≈1–3 min for 3 runs). */
    public static final List<WorkloadProfile> DEFAULT_SUITE = List.of(LATE_CAMPAIGN_DENSE);

    /** Optional extended coverage: all three maps. */
    public static final List<WorkloadProfile> FULL_SUITE = List.of(
            LATE_CAMPAIGN_DENSE, CAVERNS_COMPLEX, PRESSURE_SKIRMISH);

    private final String id;
    private final String level;
    private final int creatureCount;
    private final int impCount;
    private final double durationSeconds;
    private final String description;

    public WorkloadProfile(String id, String level, int creatureCount, int impCount,
            double durationSeconds, String description) {
        this.id = id;
        this.level = level;
        this.creatureCount = creatureCount;
        this.impCount = impCount;
        this.durationSeconds = durationSeconds;
        this.description = description;
    }

    public static WorkloadProfile byId(String id) {
        String key = id.trim().toLowerCase(Locale.ROOT);
        if ("full".equals(key) || "all".equals(key)) {
            throw new IllegalArgumentException("Use suite --workloads with comma-separated ids, not " + id);
        }
        for (WorkloadProfile profile : FULL_SUITE) {
            if (profile.id.equalsIgnoreCase(key) || profile.level.equalsIgnoreCase(key)) {
                return profile;
            }
        }
        throw new IllegalArgumentException("Unknown workload: " + id);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("level", level);
        map.put("creatureCount", creatureCount);
        map.put("impCount", impCount);
        map.put("durationSeconds", durationSeconds);
        map.put("description", description);
        return map;
    }

    public String getId() {
        return id;
    }

    public String getLevel() {
        return level;
    }

    public int getCreatureCount() {
        return creatureCount;
    }

    public int getImpCount() {
        return impCount;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public String getDescription() {
        return description;
    }
}
