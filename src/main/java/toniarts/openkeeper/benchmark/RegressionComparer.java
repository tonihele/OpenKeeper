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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compares a candidate run (or aggregated baseline) against a saved baseline
 * and produces actionable regression / improvement diagnostics.
 * <p>
 * Uses a lightweight JSON number extractor to avoid a full parser dependency.
 */
public final class RegressionComparer {

    /** Relative FPS drop beyond this is flagged as a regression. */
    public static final double FPS_REGRESSION_THRESHOLD = 0.05;
    public static final double FRAME_TIME_REGRESSION_THRESHOLD = 0.08;

    private RegressionComparer() {
    }

    public static Map<String, Object> compareFile(Path baselinePath, Map<String, Object> candidate)
            throws IOException {
        String baselineJson = Files.readString(baselinePath, StandardCharsets.UTF_8);
        return compareJson(baselineJson, candidate);
    }

    public static Map<String, Object> compareJson(String baselineJson, Map<String, Object> candidate) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("baselinePresent", true);

        Double baseAvg = readNumber(baselineJson, "\"avgFps\"");
        Double candAvg = asDouble(deepGet(candidate, "metrics", "avgFps"));
        Double baseOne = readNumber(baselineJson, "\"onePercentLowFps\"");
        Double candOne = asDouble(deepGet(candidate, "metrics", "onePercentLowFps"));
        Double baseP99 = readNested(baselineJson, "frameTimeMs", "p99");
        Double candP99 = asDouble(deepGet(candidate, "metrics", "frameTimeMs", "p99"));
        Double baseHeap = readNested(baselineJson, "memory", "heapUsedPeakMb");
        Double candHeap = asDouble(deepGet(candidate, "metrics", "memory", "heapUsedPeakMb"));

        List<Map<String, Object>> findings = new ArrayList<>();
        addDelta(findings, "avgFps", baseAvg, candAvg, true, FPS_REGRESSION_THRESHOLD);
        addDelta(findings, "onePercentLowFps", baseOne, candOne, true, FPS_REGRESSION_THRESHOLD);
        addDelta(findings, "frameTimeMs.p99", baseP99, candP99, false, FRAME_TIME_REGRESSION_THRESHOLD);
        addDelta(findings, "memory.heapUsedPeakMb", baseHeap, candHeap, false, 0.15);

        boolean visualPass = true;
        Object visual = candidate.get("visual");
        if (visual instanceof Map<?, ?> vmap && vmap.get("passed") instanceof Boolean passed) {
            visualPass = passed;
            findings.add(Map.of(
                    "metric", "visualParity",
                    "status", passed ? "pass" : "fail",
                    "detail", passed ? "All checkpoints within thresholds" : "One or more visual checkpoints failed"));
        }

        long regressions = findings.stream().filter(f -> "regression".equals(f.get("status"))).count();
        long improvements = findings.stream().filter(f -> "improvement".equals(f.get("status"))).count();

        report.put("findings", findings);
        report.put("regressionCount", regressions);
        report.put("improvementCount", improvements);
        report.put("visualPass", visualPass);
        report.put("overall", regressions == 0 && visualPass ? "pass" : "fail");
        report.put("bottlenecks", inferBottlenecks(candidate));
        report.put("optimizationHints", hints(candidate, findings));
        return report;
    }

    private static void addDelta(List<Map<String, Object>> findings, String name,
            Double baseline, Double candidate, boolean higherIsBetter, double threshold) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("metric", name);
        if (baseline == null || candidate == null || baseline == 0.0) {
            row.put("status", "n/a");
            findings.add(row);
            return;
        }
        double delta = candidate - baseline;
        double rel = delta / Math.abs(baseline);
        row.put("baseline", round(baseline));
        row.put("candidate", round(candidate));
        row.put("delta", round(delta));
        row.put("relative", round(rel));
        boolean worse = higherIsBetter ? rel < -threshold : rel > threshold;
        boolean better = higherIsBetter ? rel > threshold : rel < -threshold;
        row.put("status", worse ? "regression" : better ? "improvement" : "stable");
        findings.add(row);
    }

    @SuppressWarnings("unchecked")
    private static List<String> inferBottlenecks(Map<String, Object> candidate) {
        List<String> out = new ArrayList<>();
        Map<String, Object> metrics = (Map<String, Object>) candidate.get("metrics");
        if (metrics == null) {
            return out;
        }
        Double avgFps = asDouble(metrics.get("avgFps"));
        Double onePct = asDouble(metrics.get("onePercentLowFps"));
        if (avgFps != null && onePct != null && avgFps > 0 && onePct < avgFps * 0.5) {
            out.add("High frame-time variance (1% lows << average) — investigate stalls/GC/spikes");
        }
        Map<String, Object> draws = (Map<String, Object>) metrics.get("drawCalls");
        if (draws != null && asDouble(draws.get("avg")) != null && asDouble(draws.get("avg")) > 4000) {
            out.add("High draw-call count — batching / material consolidation candidates");
        }
        Map<String, Object> lights = (Map<String, Object>) metrics.get("lightsUploaded");
        if (lights != null && asDouble(lights.get("avg")) != null && asDouble(lights.get("avg")) > 200) {
            out.add("Many uploaded lights — vertex light loop / culling pressure");
        }
        Map<String, Object> trends = (Map<String, Object>) metrics.get("trends");
        if (trends != null && Boolean.TRUE.equals(trends.get("possibleThrottling"))) {
            out.add("FPS drift over the run — possible thermal throttling or accumulating work");
        }
        return out;
    }

    private static List<String> hints(Map<String, Object> candidate, List<Map<String, Object>> findings) {
        List<String> hints = new ArrayList<>(inferBottlenecks(candidate));
        for (Map<String, Object> f : findings) {
            if ("regression".equals(f.get("status"))) {
                hints.add("Regressed metric: " + f.get("metric") + " (Δ rel=" + f.get("relative") + ")");
            }
        }
        return hints;
    }

    private static Double readNumber(String json, String keyLiteral) {
        Pattern p = Pattern.compile(Pattern.quote(keyLiteral) + "\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
        Matcher m = p.matcher(json);
        return m.find() ? Double.parseDouble(m.group(1)) : null;
    }

    private static Double readNested(String json, String object, String field) {
        Pattern p = Pattern.compile("\"" + object + "\"\\s*:\\s*\\{[^}]*\"" + field + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
        Matcher m = p.matcher(json);
        return m.find() ? Double.parseDouble(m.group(1)) : null;
    }

    @SuppressWarnings("unchecked")
    private static Object deepGet(Map<String, Object> root, String... keys) {
        Object cur = root;
        for (String key : keys) {
            if (!(cur instanceof Map<?, ?> map)) {
                return null;
            }
            cur = ((Map<String, Object>) map).get(key);
        }
        return cur;
    }

    private static Double asDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return null;
    }

    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
