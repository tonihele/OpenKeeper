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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Downsampled time series for sustained-run diagnostics (spikes, stalls, memory growth).
 */
public final class TrendSeries {

    public static final class Sample {
        public final double tSec;
        public final double frameTimeMs;
        public final double fps;
        public final double heapMb;
        public final int drawCalls;
        public final int triangles;
        public final int lights;

        Sample(double tSec, double frameTimeMs, double fps, double heapMb,
                int drawCalls, int triangles, int lights) {
            this.tSec = tSec;
            this.frameTimeMs = frameTimeMs;
            this.fps = fps;
            this.heapMb = heapMb;
            this.drawCalls = drawCalls;
            this.triangles = triangles;
            this.lights = lights;
        }

        Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("tSec", round(tSec));
            m.put("frameTimeMs", round(frameTimeMs));
            m.put("fps", round(fps));
            m.put("heapMb", round(heapMb));
            m.put("drawCalls", drawCalls);
            m.put("triangles", triangles);
            m.put("lights", lights);
            return m;
        }
    }

    private final List<Sample> samples = new ArrayList<>();
    private final double intervalSec;
    private double nextSampleAt;
    private int stallCount;
    private double longestStallMs;
    private double maxFrameTimeMs;

    public TrendSeries(double intervalSec) {
        this.intervalSec = Math.max(0.25, intervalSec);
        this.nextSampleAt = 0.0;
    }

    public void offer(double tSec, double frameTimeMs, double heapMb,
            int drawCalls, int triangles, int lights) {
        if (frameTimeMs > maxFrameTimeMs) {
            maxFrameTimeMs = frameTimeMs;
        }
        // Stall heuristic: >100ms frame
        if (frameTimeMs >= 100.0) {
            stallCount++;
            longestStallMs = Math.max(longestStallMs, frameTimeMs);
        }
        if (tSec + 1e-9 < nextSampleAt && !samples.isEmpty()) {
            return;
        }
        double fps = frameTimeMs > 0 ? 1000.0 / frameTimeMs : 0.0;
        samples.add(new Sample(tSec, frameTimeMs, fps, heapMb, drawCalls, triangles, lights));
        nextSampleAt = tSec + intervalSec;
    }

    public Map<String, Object> summarize() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("intervalSec", intervalSec);
        out.put("sampleCount", samples.size());
        out.put("stallFramesOver100ms", stallCount);
        out.put("longestStallMs", round(longestStallMs));
        out.put("maxFrameTimeMs", round(maxFrameTimeMs));

        if (samples.size() >= 2) {
            Sample first = samples.get(0);
            Sample last = samples.get(samples.size() - 1);
            double dt = Math.max(1e-6, last.tSec - first.tSec);
            out.put("heapGrowthMbPerSec", round((last.heapMb - first.heapMb) / dt));
            out.put("fpsStart", round(first.fps));
            out.put("fpsEnd", round(last.fps));
            out.put("fpsDrift", round(last.fps - first.fps));
            // Possible thermal/OS throttling signal: sustained drop >15% from early window
            double early = averageFps(0, Math.min(5, samples.size()));
            double late = averageFps(Math.max(0, samples.size() - 5), samples.size());
            out.put("fpsEarlyWindow", round(early));
            out.put("fpsLateWindow", round(late));
            out.put("possibleThrottling", early > 1 && late < early * 0.85);
        } else {
            out.put("heapGrowthMbPerSec", 0.0);
            out.put("possibleThrottling", false);
        }

        List<Map<String, Object>> series = new ArrayList<>(samples.size());
        for (Sample s : samples) {
            series.add(s.toMap());
        }
        out.put("samples", series);
        return out;
    }

    private double averageFps(int from, int to) {
        double sum = 0;
        int n = 0;
        for (int i = from; i < to; i++) {
            sum += samples.get(i).fps;
            n++;
        }
        return n == 0 ? 0 : sum / n;
    }

    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
