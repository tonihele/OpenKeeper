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

import com.jme3.app.Application;
import com.jme3.renderer.Statistics;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.LinkedHashMap;
import java.util.Map;
import toniarts.openkeeper.game.state.GameClientState;
import toniarts.openkeeper.view.PlayerMapViewState;
import toniarts.openkeeper.view.lighting.LightRules;

/**
 * Collects per-frame samples and downsampled trends during measurement.
 */
public final class BenchmarkSampler {

    private static final int IDX_VERTICES = 0;
    private static final int IDX_TRIANGLES = 1;
    private static final int IDX_OBJECTS = 3;
    private static final int IDX_SHADER_SWITCHES = 4;
    private static final int IDX_TEXTURE_BINDS = 7;

    private final FrameTimeStats frameTimes = new FrameTimeStats();
    private final RunningStat drawCalls = new RunningStat();
    private final RunningStat triangles = new RunningStat();
    private final RunningStat vertices = new RunningStat();
    private final RunningStat shaderSwitches = new RunningStat();
    private final RunningStat textureBinds = new RunningStat();
    private final RunningStat lightsUploaded = new RunningStat();
    private final RunningStat lightSources = new RunningStat();
    private final RunningStat usedHeapMb = new RunningStat();
    private final TrendSeries trends = new TrendSeries(1.0);

    private final int[] statsScratch = new int[13];
    private long lastFrameNanos;
    private long startHeapBytes;
    private long peakHeapBytes;
    private long startGcCount;
    private long startGcTimeMs;
    private boolean heapBaselineSet;
    private boolean statsEnabled;
    private double measureElapsed;

    public void begin(Application app) {
        lastFrameNanos = 0L;
        measureElapsed = 0.0;
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        startHeapBytes = memory.getHeapMemoryUsage().getUsed();
        peakHeapBytes = startHeapBytes;
        startGcCount = totalGcCount();
        startGcTimeMs = totalGcTimeMs();
        heapBaselineSet = true;
        enableRendererStats(app);
    }

    public void sample(Application app, GameClientState client, double measureElapsedSec) {
        this.measureElapsed = measureElapsedSec;
        long now = System.nanoTime();
        double frameTimeMs = 0;
        if (lastFrameNanos > 0L) {
            frameTimeMs = (now - lastFrameNanos) / 1_000_000.0;
            frameTimes.addFrameTimeMs(frameTimeMs);
        }
        lastFrameNanos = now;

        int draws = 0;
        int tris = 0;
        int lights = 0;
        if (app.getRenderer() != null) {
            Statistics stats = app.getRenderer().getStatistics();
            if (stats != null) {
                if (!statsEnabled) {
                    stats.setEnabled(true);
                    statsEnabled = true;
                }
                stats.getData(statsScratch);
                vertices.add(statsScratch[IDX_VERTICES]);
                triangles.add(statsScratch[IDX_TRIANGLES]);
                drawCalls.add(statsScratch[IDX_OBJECTS]);
                shaderSwitches.add(statsScratch[IDX_SHADER_SWITCHES]);
                textureBinds.add(statsScratch[IDX_TEXTURE_BINDS]);
                draws = statsScratch[IDX_OBJECTS];
                tris = statsScratch[IDX_TRIANGLES];
            }
        }

        if (client != null) {
            PlayerMapViewState mapView = client.getPlayerMapViewState();
            if (mapView != null && mapView.isMapLoaded()) {
                LightRules lightRules = mapView.getMapLoader().getLightRules();
                lights = lightRules.getUploadedLightCount();
                lightsUploaded.add(lights);
                lightSources.add(lightRules.getSourceCount());
            }
        }

        long used = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        peakHeapBytes = Math.max(peakHeapBytes, used);
        double heapMb = used / (1024.0 * 1024.0);
        usedHeapMb.add(heapMb);

        if (frameTimeMs > 0) {
            trends.offer(measureElapsedSec, frameTimeMs, heapMb, draws, tris, lights);
        }
    }

    public Map<String, Object> summarize(double measuredSeconds) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        Map<String, Object> frameSummary = frameTimes.summarize();
        metrics.putAll(frameSummary);
        metrics.put("durationSec", Math.round(measuredSeconds * 1000.0) / 1000.0);
        metrics.put("cpuFrameTimeMs", frameSummary.get("frameTimeMs"));
        metrics.put("gpuFrameTimeMs", null);
        metrics.put("drawCalls", drawCalls.summarize());
        metrics.put("triangles", triangles.summarize());
        metrics.put("vertices", vertices.summarize());
        metrics.put("shaderSwitches", shaderSwitches.summarize());
        metrics.put("textureBinds", textureBinds.summarize());
        metrics.put("lightsUploaded", lightsUploaded.summarize());
        metrics.put("lightSources", lightSources.summarize());
        metrics.put("trends", trends.summarize());

        Map<String, Object> memory = new LinkedHashMap<>();
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        long endUsed = mem.getHeapMemoryUsage().getUsed();
        long maxHeap = mem.getHeapMemoryUsage().getMax();
        memory.put("heapUsedStartMb", mb(heapBaselineSet ? startHeapBytes : endUsed));
        memory.put("heapUsedEndMb", mb(endUsed));
        memory.put("heapUsedPeakMb", mb(peakHeapBytes));
        memory.put("heapMaxMb", maxHeap > 0 ? mb(maxHeap) : -1.0);
        memory.put("heapDeltaMb", mb(endUsed - (heapBaselineSet ? startHeapBytes : endUsed)));
        memory.put("heapUsedSeriesMb", usedHeapMb.summarize());
        memory.put("gcCountDelta", totalGcCount() - startGcCount);
        memory.put("gcTimeMsDelta", totalGcTimeMs() - startGcTimeMs);
        metrics.put("memory", memory);
        return metrics;
    }

    public FrameTimeStats getFrameTimes() {
        return frameTimes;
    }

    private void enableRendererStats(Application app) {
        if (app.getRenderer() != null && app.getRenderer().getStatistics() != null) {
            app.getRenderer().getStatistics().setEnabled(true);
            statsEnabled = true;
        }
    }

    private static double mb(long bytes) {
        return Math.round((bytes / (1024.0 * 1024.0)) * 1000.0) / 1000.0;
    }

    private static long totalGcCount() {
        long total = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            long c = gc.getCollectionCount();
            if (c > 0) {
                total += c;
            }
        }
        return total;
    }

    private static long totalGcTimeMs() {
        long total = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            long t = gc.getCollectionTime();
            if (t > 0) {
                total += t;
            }
        }
        return total;
    }
}
