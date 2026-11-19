/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.animation;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import toniarts.openkeeper.game.component.SimulationTime;

/** Monotonic, pause-aware view of replicated simulation time, never network wall time. */
public final class PresentationClock implements AutoCloseable {
    private final EntitySet clocks;
    private SimulationTime sample;
    private double elapsed;
    private double time = Double.NaN;
    private boolean paused;

    public PresentationClock(EntityData data) { clocks = data.getEntities(SimulationTime.class); }

    public void update(float tpf) {
        clocks.applyChanges();
        if (paused) return;
        for (var entity : clocks) {
            SimulationTime next = entity.get(SimulationTime.class);
            if (next != sample && Double.isFinite(next.seconds) && next.seconds >= 0
                    && Double.isFinite(next.tickSeconds) && next.tickSeconds >= 0) {
                sample = next; elapsed = 0;
            }
        }
        if (sample == null) return;
        // Bound prediction during stalled/disconnected delivery; never move backwards.
        double prediction = Math.max(0, Math.min(.25, sample.tickSeconds * 2));
        double candidate = sample.seconds + Math.min(elapsed, prediction);
        time = Double.isNaN(time) ? candidate : Math.max(time, candidate);
        if (Float.isFinite(tpf) && tpf > 0) elapsed += tpf;
    }

    public double getTime() { return time; }
    public void setPaused(boolean paused) { this.paused = paused; }
    @Override public void close() { clocks.release(); }
}
