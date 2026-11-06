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

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Parsed {@code -benchmark} CLI options. Suite mode lengthens measurement,
 * enables visual checkpoints, and locks resolution for reproducible frames.
 */
public final class BenchmarkConfig {

    public static final String FRAMEWORK_VERSION = "2";
    public static final String DEFAULT_LEVEL = "Level20";
    public static final long DEFAULT_SEED = 42L;
    public static final double DEFAULT_WARMUP_SECONDS = 3.0;
    public static final double DEFAULT_DURATION_SECONDS = 20.0;
    public static final double DEFAULT_SETTLE_SECONDS = 2.0;
    /** Suite defaults keep a full baseline under a few minutes. */
    public static final double SUITE_WARMUP_SECONDS = 3.0;
    public static final double SUITE_DURATION_SECONDS = 20.0;
    public static final double SUITE_SETTLE_SECONDS = 2.0;
    public static final int DEFAULT_CREATURE_COUNT = 120;
    public static final int DEFAULT_IMP_COUNT = 40;
    public static final int DEFAULT_WIDTH = 1920;
    public static final int DEFAULT_HEIGHT = 1080;
    /** Informational for animated scenes; pass is gated on SSIM/MAE. */
    public static final double DEFAULT_MAX_MISMATCH_RATIO = 0.50;
    public static final double DEFAULT_MIN_SSIM = 0.92;
    public static final double DEFAULT_MAX_MAE = 4.0;

    private final boolean enabled;
    private final boolean suite;
    private final Path outputPath;
    private final Path baselineDir;
    private final Path compareBaselinePath;
    private final String level;
    private final String workloadId;
    private final long seed;
    private final double warmupSeconds;
    private final double durationSeconds;
    private final double settleSeconds;
    private final int creatureCount;
    private final int impCount;
    private final boolean headless;
    private final boolean vsync;
    private final boolean visual;
    private final boolean updateVisualRefs;
    private final int width;
    private final int height;
    private final int runIndex;
    private final List<Double> visualCheckpointFractions;
    private final double maxMismatchRatio;
    private final double minSsim;
    private final double maxMae;

    public BenchmarkConfig(boolean enabled, boolean suite, Path outputPath, Path baselineDir,
            Path compareBaselinePath, String level, String workloadId, long seed,
            double warmupSeconds, double durationSeconds, double settleSeconds,
            int creatureCount, int impCount, boolean headless, boolean vsync,
            boolean visual, boolean updateVisualRefs, int width, int height, int runIndex,
            List<Double> visualCheckpointFractions, double maxMismatchRatio, double minSsim, double maxMae) {
        this.enabled = enabled;
        this.suite = suite;
        this.outputPath = outputPath;
        this.baselineDir = baselineDir;
        this.compareBaselinePath = compareBaselinePath;
        this.level = level;
        this.workloadId = workloadId;
        this.seed = seed;
        this.warmupSeconds = warmupSeconds;
        this.durationSeconds = durationSeconds;
        this.settleSeconds = settleSeconds;
        this.creatureCount = creatureCount;
        this.impCount = impCount;
        this.headless = headless;
        this.vsync = vsync;
        this.visual = visual;
        this.updateVisualRefs = updateVisualRefs;
        this.width = width;
        this.height = height;
        this.runIndex = runIndex;
        this.visualCheckpointFractions = List.copyOf(visualCheckpointFractions);
        this.maxMismatchRatio = maxMismatchRatio;
        this.minSsim = minSsim;
        this.maxMae = maxMae;
    }

    public static BenchmarkConfig fromParams(Map<String, String> params) {
        Objects.requireNonNull(params, "params");
        if (!params.containsKey("benchmark") && !params.containsKey("benchmark-suite")) {
            return disabled();
        }

        boolean suite = params.containsKey("benchmark-suite")
                || "suite".equalsIgnoreCase(nullToEmpty(params.get("benchmark-mode")));
        String outRaw = firstNonBlank(params.get("benchmark"), params.get("benchmark-suite"));
        Path outputPath = (outRaw == null || outRaw.isBlank())
                ? (suite ? defaultSuiteOutputPath() : defaultOutputPath())
                : Path.of(outRaw);

        Path baselineDir = params.containsKey("benchmark-baseline-dir")
                ? Path.of(params.get("benchmark-baseline-dir"))
                : Path.of("baselines", "performance");
        Path compareBaseline = params.containsKey("benchmark-compare")
                ? Path.of(params.get("benchmark-compare"))
                : null;

        WorkloadProfile workload = null;
        if (params.containsKey("benchmark-workload")) {
            workload = WorkloadProfile.byId(params.get("benchmark-workload"));
        }

        double defaultWarmup = suite ? SUITE_WARMUP_SECONDS : DEFAULT_WARMUP_SECONDS;
        double defaultDuration = suite
                ? (workload != null ? workload.getDurationSeconds() : SUITE_DURATION_SECONDS)
                : DEFAULT_DURATION_SECONDS;
        double defaultSettle = suite ? SUITE_SETTLE_SECONDS : DEFAULT_SETTLE_SECONDS;
        int defaultCreatures = workload != null ? workload.getCreatureCount() : DEFAULT_CREATURE_COUNT;
        int defaultImps = workload != null ? workload.getImpCount() : DEFAULT_IMP_COUNT;
        String defaultLevel = workload != null ? workload.getLevel()
                : firstNonBlank(params.get("benchmark-level"), params.get("level"), DEFAULT_LEVEL);

        String level = firstNonBlank(params.get("benchmark-level"), params.get("level"),
                workload != null ? workload.getLevel() : defaultLevel);
        String workloadId = workload != null ? workload.getId()
                : (suite ? WorkloadProfile.LATE_CAMPAIGN_DENSE.getId() : "custom");

        boolean visual = suite || params.containsKey("benchmark-visual");
        if (params.containsKey("benchmark-no-visual")) {
            visual = false;
        }
        if (params.containsKey("headless") || params.containsKey("benchmark-headless")) {
            visual = false;
        }

        return new BenchmarkConfig(
                true,
                suite,
                outputPath,
                baselineDir,
                compareBaseline,
                level,
                workloadId,
                parseLong(params.get("benchmark-seed"), DEFAULT_SEED),
                parseDouble(params.get("benchmark-warmup"), defaultWarmup),
                parseDouble(params.get("benchmark-duration"), defaultDuration),
                parseDouble(params.get("benchmark-settle"), defaultSettle),
                Math.max(0, parseInt(params.get("benchmark-creatures"), defaultCreatures)),
                Math.max(0, parseInt(params.get("benchmark-imps"), defaultImps)),
                params.containsKey("headless") || params.containsKey("benchmark-headless"),
                params.containsKey("benchmark-vsync"),
                visual,
                params.containsKey("benchmark-update-visual-refs"),
                parseInt(params.get("benchmark-width"), DEFAULT_WIDTH),
                parseInt(params.get("benchmark-height"), DEFAULT_HEIGHT),
                parseInt(params.get("benchmark-run-index"), 1),
                parseFractions(params.get("benchmark-checkpoints"), List.of(0.0, 0.5, 1.0)),
                parseDouble(params.get("benchmark-max-mismatch"), DEFAULT_MAX_MISMATCH_RATIO),
                parseDouble(params.get("benchmark-min-ssim"), DEFAULT_MIN_SSIM),
                parseDouble(params.get("benchmark-max-mae"), DEFAULT_MAX_MAE));
    }

    public static BenchmarkConfig disabled() {
        return new BenchmarkConfig(false, false, defaultOutputPath(),
                Path.of("baselines", "performance"), null, DEFAULT_LEVEL, "custom",
                DEFAULT_SEED, DEFAULT_WARMUP_SECONDS, DEFAULT_DURATION_SECONDS, DEFAULT_SETTLE_SECONDS,
                DEFAULT_CREATURE_COUNT, DEFAULT_IMP_COUNT, false, false, false, false,
                DEFAULT_WIDTH, DEFAULT_HEIGHT, 1, List.of(0.0, 0.5, 1.0),
                DEFAULT_MAX_MISMATCH_RATIO, DEFAULT_MIN_SSIM, DEFAULT_MAX_MAE);
    }

    public BenchmarkConfig withOutputPath(Path path) {
        return new BenchmarkConfig(enabled, suite, path, baselineDir, compareBaselinePath, level, workloadId,
                seed, warmupSeconds, durationSeconds, settleSeconds, creatureCount, impCount,
                headless, vsync, visual, updateVisualRefs, width, height, runIndex,
                visualCheckpointFractions, maxMismatchRatio, minSsim, maxMae);
    }

    public BenchmarkConfig withRunIndex(int index) {
        return new BenchmarkConfig(enabled, suite, outputPath, baselineDir, compareBaselinePath, level, workloadId,
                seed, warmupSeconds, durationSeconds, settleSeconds, creatureCount, impCount,
                headless, vsync, visual, updateVisualRefs, width, height, index,
                visualCheckpointFractions, maxMismatchRatio, minSsim, maxMae);
    }

    public static Path defaultOutputPath() {
        return Path.of("build", "benchmark-results", "benchmark-" + utcStamp() + ".json");
    }

    public static Path defaultSuiteOutputPath() {
        return Path.of("build", "benchmark-results", "suite-" + utcStamp() + ".json");
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("frameworkVersion", FRAMEWORK_VERSION);
        map.put("suite", suite);
        map.put("workloadId", workloadId);
        map.put("level", level);
        map.put("seed", seed);
        map.put("warmupSeconds", warmupSeconds);
        map.put("durationSeconds", durationSeconds);
        map.put("settleSeconds", settleSeconds);
        map.put("creatureCount", creatureCount);
        map.put("impCount", impCount);
        map.put("headless", headless);
        map.put("vsync", vsync);
        map.put("visual", visual);
        map.put("updateVisualRefs", updateVisualRefs);
        map.put("width", width);
        map.put("height", height);
        map.put("runIndex", runIndex);
        map.put("visualCheckpointFractions", visualCheckpointFractions);
        map.put("maxMismatchRatio", maxMismatchRatio);
        map.put("minSsim", minSsim);
        map.put("maxMae", maxMae);
        map.put("outputPath", outputPath.toString().replace('\\', '/'));
        map.put("baselineDir", baselineDir.toString().replace('\\', '/'));
        if (compareBaselinePath != null) {
            map.put("compareBaselinePath", compareBaselinePath.toString().replace('\\', '/'));
        }
        return map;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isSuite() {
        return suite;
    }

    public Path getOutputPath() {
        return outputPath;
    }

    public Path getBaselineDir() {
        return baselineDir;
    }

    public Path getCompareBaselinePath() {
        return compareBaselinePath;
    }

    public String getLevel() {
        return level;
    }

    public String getWorkloadId() {
        return workloadId;
    }

    public long getSeed() {
        return seed;
    }

    public double getWarmupSeconds() {
        return warmupSeconds;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public double getSettleSeconds() {
        return settleSeconds;
    }

    public int getCreatureCount() {
        return creatureCount;
    }

    public int getImpCount() {
        return impCount;
    }

    public boolean isHeadless() {
        return headless;
    }

    public boolean isVsync() {
        return vsync;
    }

    public boolean isVisual() {
        return visual;
    }

    public boolean isUpdateVisualRefs() {
        return updateVisualRefs;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getRunIndex() {
        return runIndex;
    }

    public List<Double> getVisualCheckpointFractions() {
        return visualCheckpointFractions;
    }

    public double getMaxMismatchRatio() {
        return maxMismatchRatio;
    }

    public double getMinSsim() {
        return minSsim;
    }

    public double getMaxMae() {
        return maxMae;
    }

    private static String utcStamp() {
        return DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return DEFAULT_LEVEL;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static List<Double> parseFractions(String raw, List<Double> fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        List<Double> out = new ArrayList<>();
        for (String part : raw.split(",")) {
            out.add(Double.parseDouble(part.trim()));
        }
        return out;
    }

    private static long parseLong(String raw, long fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return Long.parseLong(raw.trim());
    }

    private static double parseDouble(String raw, double fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return Double.parseDouble(raw.trim());
    }

    private static int parseInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return Integer.parseInt(raw.trim());
    }
}
