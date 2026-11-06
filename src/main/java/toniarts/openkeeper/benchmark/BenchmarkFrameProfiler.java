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
import com.jme3.profile.AppProfiler;
import com.jme3.profile.AppStep;
import com.jme3.profile.SpStep;
import com.jme3.profile.VpStep;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;

/**
 * Suite AppProfiler: CPU update vs render splits plus a whole-frame GPU
 * elapsed query around the render phase. Ignores steps until the first
 * {@link AppStep#BeginFrame} after {@link #attach} so mid-update attach
 * does not produce bogus timings. Technique-level VS splits still need
 * RenderDoc/Nsight.
 */
public final class BenchmarkFrameProfiler implements AppProfiler {

    /** Reject absurd samples (e.g. attach mid-frame before BeginFrame). */
    private static final double MAX_SANE_CPU_MS = 250.0;

    private final GpuFrameTimer gpuTimer = new GpuFrameTimer();
    private Renderer renderer;
    private long frameStartNanos;
    private long renderStartNanos;
    private double lastCpuUpdateMs;
    private double lastCpuRenderMs;
    private double lastGpuMs = Double.NaN;
    private boolean attached;
    private boolean primed;
    private boolean sampleReady;

    /**
     * Installs as the application profiler (replaces any prior hook).
     *
     * @param app host application
     */
    public void attach(Application app) {
        renderer = app.getRenderer();
        primed = false;
        sampleReady = false;
        lastGpuMs = Double.NaN;
        lastCpuUpdateMs = 0.0;
        lastCpuRenderMs = 0.0;
        app.setAppProfiler(this);
        attached = true;
    }

    /**
     * Clears the application profiler if this instance is still installed.
     *
     * @param app host application
     */
    public void detach(Application app) {
        if (attached && app.getAppProfiler() == this) {
            app.setAppProfiler(null);
        }
        attached = false;
        primed = false;
        sampleReady = false;
    }

    /**
     * {@code true} after at least one full BeginFrame→EndFrame cycle.
     */
    public boolean hasSample() {
        return sampleReady;
    }

    public double getLastCpuUpdateMs() {
        return lastCpuUpdateMs;
    }

    public double getLastCpuRenderMs() {
        return lastCpuRenderMs;
    }

    /**
     * @return last completed GPU render elapsed ms, or {@link Double#NaN}
     */
    public double getLastGpuMs() {
        return lastGpuMs;
    }

    public boolean isGpuSupported() {
        return gpuTimer.isSupported();
    }

    @Override
    public void appStep(AppStep step) {
        switch (step) {
            case BeginFrame -> {
                primed = true;
                frameStartNanos = System.nanoTime();
            }
            case RenderFrame -> {
                if (!primed) {
                    return;
                }
                double updateMs = (System.nanoTime() - frameStartNanos) / 1_000_000.0;
                if (updateMs >= 0.0 && updateMs <= MAX_SANE_CPU_MS) {
                    lastCpuUpdateMs = updateMs;
                }
                renderStartNanos = System.nanoTime();
                gpuTimer.begin(renderer);
            }
            case EndFrame -> {
                if (!primed) {
                    return;
                }
                double renderMs = (System.nanoTime() - renderStartNanos) / 1_000_000.0;
                if (renderMs >= 0.0 && renderMs <= MAX_SANE_CPU_MS) {
                    lastCpuRenderMs = renderMs;
                }
                lastGpuMs = gpuTimer.end(renderer);
                sampleReady = true;
            }
            default -> {
            }
        }
    }

    @Override
    public void appSubStep(String... additionalInfo) {
    }

    @Override
    public void vpStep(VpStep step, ViewPort vp, RenderQueue.Bucket bucket) {
    }

    @Override
    public void spStep(SpStep step, String... additionalInfo) {
    }
}
