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
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Aggregates repeated suite run JSON files into an official machine baseline.
 */
public final class BaselineAggregator {

    private BaselineAggregator() {
    }

    public static Map<String, Object> aggregate(List<Path> runFiles, String machineId) throws IOException {
        List<Map<String, Double>> metrics = new ArrayList<>();
        List<String> rawPaths = new ArrayList<>();
        List<Boolean> visualPasses = new ArrayList<>();
        String sampleJson = null;

        for (Path path : runFiles) {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            if (sampleJson == null) {
                sampleJson = json;
            }
            rawPaths.add(path.toAbsolutePath().toString().replace('\\', '/'));
            Map<String, Double> row = new LinkedHashMap<>();
            row.put("avgFps", requireNumber(json, "avgFps"));
            row.put("onePercentLowFps", requireNumber(json, "onePercentLowFps"));
            row.put("pointOnePercentLowFps", requireNumber(json, "pointOnePercentLowFps"));
            row.put("frameTimeP50", nested(json, "frameTimeMs", "p50"));
            row.put("frameTimeP95", nested(json, "frameTimeMs", "p95"));
            row.put("frameTimeP99", nested(json, "frameTimeMs", "p99"));
            row.put("drawCallsAvg", nested(json, "drawCalls", "avg"));
            row.put("trianglesAvg", nested(json, "triangles", "avg"));
            row.put("lightsAvg", nested(json, "lightsUploaded", "avg"));
            row.put("heapPeakMb", nested(json, "memory", "heapUsedPeakMb"));
            metrics.add(row);
            visualPasses.add(!json.contains("\"passed\": false"));
        }

        Map<String, Object> aggregates = new LinkedHashMap<>();
        for (String key : metrics.get(0).keySet()) {
            List<Double> values = new ArrayList<>();
            for (Map<String, Double> row : metrics) {
                values.add(row.get(key));
            }
            aggregates.put(key, stats(values));
        }

        Map<String, Object> stability = new LinkedHashMap<>();
        Map<?, ?> avgFpsStats = (Map<?, ?>) aggregates.get("avgFps");
        double cv = ((Number) avgFpsStats.get("cv")).doubleValue();
        stability.put("avgFpsCoefficientOfVariation", cv);
        stability.put("unstable", cv > 0.08);
        stability.put("note", cv > 0.08
                ? "Run-to-run avgFps CV > 8% — environmental interference likely"
                : "Run-to-run variance within expected noise");

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", 2);
        root.put("benchmark", "openkeeper-official-baseline");
        root.put("frameworkVersion", BenchmarkConfig.FRAMEWORK_VERSION);
        root.put("timestamp", Instant.now().toString());
        root.put("machineId", machineId);
        root.put("runCount", runFiles.size());
        root.put("rawRuns", rawPaths);
        root.put("visualAllPassed", visualPasses.stream().allMatch(Boolean::booleanValue));
        root.put("aggregates", aggregates);
        root.put("stability", stability);
        if (sampleJson != null) {
            root.put("engine", extractObject(sampleJson, "engine"));
            root.put("hardware", extractObject(sampleJson, "hardware"));
            root.put("parameters", extractObject(sampleJson, "parameters"));
        }
        root.put("notes", "Official machine baseline from consecutive identical suite runs. "
                + "Use -benchmark-compare <this-file> on future runs.");
        return root;
    }

    public static Path write(Path path, Map<String, Object> document) throws IOException {
        return BenchmarkResultWriter.write(path, document);
    }

    private static Map<String, Object> stats(List<Double> values) {
        double sum = 0;
        for (double v : values) {
            sum += v;
        }
        double mean = sum / values.size();
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Double::compareTo);
        double median = sorted.size() % 2 == 0
                ? 0.5 * (sorted.get(sorted.size() / 2 - 1) + sorted.get(sorted.size() / 2))
                : sorted.get(sorted.size() / 2);
        double var = 0;
        for (double v : values) {
            double d = v - mean;
            var += d * d;
        }
        double std = Math.sqrt(var / values.size());
        double cv = mean == 0 ? 0 : std / Math.abs(mean);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("values", values);
        out.put("mean", round(mean));
        out.put("median", round(median));
        out.put("stdDev", round(std));
        out.put("cv", round(cv));
        out.put("min", round(sorted.get(0)));
        out.put("max", round(sorted.get(sorted.size() - 1)));
        return out;
    }

    private static double requireNumber(String json, String field) {
        Double v = readNumber(json, field);
        if (v == null) {
            throw new IllegalStateException("Missing metric " + field);
        }
        return v;
    }

    private static double nested(String json, String object, String field) {
        Pattern p = Pattern.compile("\"" + object + "\"\\s*:\\s*\\{[^}]*\"" + field + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
        Matcher m = p.matcher(json);
        if (!m.find()) {
            return 0.0;
        }
        return Double.parseDouble(m.group(1));
    }

    private static Double readNumber(String json, String field) {
        Pattern p = Pattern.compile("\"" + field + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
        Matcher m = p.matcher(json);
        return m.find() ? Double.parseDouble(m.group(1)) : null;
    }

    private static String extractObject(String json, String name) {
        Pattern p = Pattern.compile("\"" + name + "\"\\s*:\\s*(\\{.*?\\})", Pattern.DOTALL);
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : "{}";
    }

    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    public static String defaultMachineId() {
        String os = System.getProperty("os.name", "os").replaceAll("\\s+", "-");
        String arch = System.getProperty("os.arch", "arch");
        return (os + "-" + arch).toLowerCase(Locale.ROOT);
    }
}
