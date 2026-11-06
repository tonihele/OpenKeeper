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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Aggregates wall-clock frame times into FPS / percentile metrics.
 * Pure Java — unit-tested without booting the engine.
 */
public final class FrameTimeStats {

    private final List<Double> frameTimesMs = new ArrayList<>();

    public void addFrameTimeMs(double frameTimeMs) {
        if (frameTimeMs > 0.0 && Double.isFinite(frameTimeMs)) {
            frameTimesMs.add(frameTimeMs);
        }
    }

    public int getFrameCount() {
        return frameTimesMs.size();
    }

    public List<Double> getFrameTimesMs() {
        return Collections.unmodifiableList(frameTimesMs);
    }

    public Map<String, Object> summarize() {
        Map<String, Object> out = new LinkedHashMap<>();
        int n = frameTimesMs.size();
        out.put("frames", n);
        if (n == 0) {
            out.put("avgFps", 0.0);
            out.put("onePercentLowFps", 0.0);
            out.put("pointOnePercentLowFps", 0.0);
            out.put("frameTimeMs", emptyPercentiles());
            return out;
        }

        List<Double> sortedAsc = new ArrayList<>(frameTimesMs);
        Collections.sort(sortedAsc);

        double sum = 0.0;
        for (double t : sortedAsc) {
            sum += t;
        }
        double avgMs = sum / n;
        out.put("avgFps", round(1000.0 / avgMs));
        out.put("onePercentLowFps", round(lowPercentFps(sortedAsc, 0.01)));
        out.put("pointOnePercentLowFps", round(lowPercentFps(sortedAsc, 0.001)));

        Map<String, Object> ft = new LinkedHashMap<>();
        ft.put("min", round(sortedAsc.get(0)));
        ft.put("max", round(sortedAsc.get(n - 1)));
        ft.put("avg", round(avgMs));
        ft.put("p50", round(percentile(sortedAsc, 0.50)));
        ft.put("p95", round(percentile(sortedAsc, 0.95)));
        ft.put("p99", round(percentile(sortedAsc, 0.99)));
        out.put("frameTimeMs", ft);
        return out;
    }

    /**
     * CapFrameX-style N% low: average of the slowest N% of frames, as FPS.
     */
    static double lowPercentFps(List<Double> sortedAscMs, double fraction) {
        int n = sortedAscMs.size();
        int worst = Math.max(1, (int) Math.ceil(n * fraction));
        double sum = 0.0;
        for (int i = n - worst; i < n; i++) {
            sum += sortedAscMs.get(i);
        }
        return 1000.0 / (sum / worst);
    }

    static double percentile(List<Double> sortedAsc, double p) {
        if (sortedAsc.isEmpty()) {
            return 0.0;
        }
        if (sortedAsc.size() == 1) {
            return sortedAsc.get(0);
        }
        double idx = p * (sortedAsc.size() - 1);
        int lo = (int) Math.floor(idx);
        int hi = (int) Math.ceil(idx);
        if (lo == hi) {
            return sortedAsc.get(lo);
        }
        double w = idx - lo;
        return sortedAsc.get(lo) * (1.0 - w) + sortedAsc.get(hi) * w;
    }

    private static Map<String, Object> emptyPercentiles() {
        Map<String, Object> ft = new LinkedHashMap<>();
        ft.put("min", 0.0);
        ft.put("max", 0.0);
        ft.put("avg", 0.0);
        ft.put("p50", 0.0);
        ft.put("p95", 0.0);
        ft.put("p99", 0.0);
        return ft;
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "FrameTimeStats{frames=%d}", frameTimesMs.size());
    }
}
