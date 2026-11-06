/*
 * Copyright (C) 2014-2025 OpenKeeper
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
package toniarts.openkeeper.view.lighting;

import java.util.ArrayList;
import java.util.Random;

/**
 * Drives every live {@link LightSource}'s modulators on the two clocks
 * the lighting needs: simulation ticks (pulse/fade/lifetime)
 * and a fixed 30&nbsp;Hz presentation clock (flicker). Owns its own
 * {@link Random} stream -- this must never share the simulation's RNG
 * state.
 * <p>
 * Sources are stored in an {@link ArrayList} guarded by {@link #lock}. The
 * map loader may add torch/terrain lights while the render thread iterates;
 * hot paths copy references into a reusable snapshot array under the lock,
 * then walk that array without allocating or holding the lock.
 */
public final class LightModulators {

    private static final LightSource[] EMPTY = new LightSource[0];

    private final Object lock = new Object();
    private final ArrayList<LightSource> sources = new ArrayList<>(256);
    private LightSource[] snapshot = EMPTY;
    private int snapshotSize;
    private boolean snapshotDirty = true;
    private final Random random = new Random();
    private float tickAccumulator;
    private float flickerAccumulator;
    /** Expired lights collected during tick without allocating a new list. */
    private final ArrayList<LightSource> expiredScratch = new ArrayList<>();

    public void add(LightSource source) {
        synchronized (lock) {
            sources.add(source);
            snapshotDirty = true;
        }
    }

    public void remove(LightSource source) {
        synchronized (lock) {
            if (sources.remove(source)) {
                snapshotDirty = true;
            }
        }
    }

    /**
     * Live source count (safe to call from any thread).
     *
     * @return number of registered lights
     */
    public int size() {
        synchronized (lock) {
            return sources.size();
        }
    }

    /**
     * Refreshes the iteration snapshot under the lock. Pair with
     * {@link #currentSnapshot()} on the same thread (render thread) before
     * walking lights; do not call {@link #add}/{@link #remove} between them.
     *
     * @return live entry count at the front of {@link #currentSnapshot()}
     */
    public int captureSnapshot() {
        synchronized (lock) {
            refreshSnapshotLocked();
            return snapshotSize;
        }
    }

    /**
     * @return array from the last {@link #captureSnapshot()} (do not mutate)
     */
    public LightSource[] currentSnapshot() {
        return snapshot;
    }

    /**
     * Reseeds the flicker RNG (presentation-only; never shared with sim).
     *
     * @param seed flicker seed
     */
    public void setSeed(long seed) {
        random.setSeed(seed);
    }

    /**
     * Advances both clocks by {@code tpf} seconds of presentation time.
     */
    public void update(float tpf) {
        tickAccumulator += tpf;
        float tickInterval = 1f / LightConstants.SIMULATION_TICKS_PER_SECOND;
        while (tickAccumulator >= tickInterval) {
            tickAccumulator -= tickInterval;
            tick();
        }

        flickerAccumulator += tpf;
        float flickerInterval = 1f / LightConstants.FLICKER_HZ;
        while (flickerAccumulator >= flickerInterval) {
            flickerAccumulator -= flickerInterval;
            flickerStep();
        }
    }

    private void tick() {
        int n = captureSnapshot();
        LightSource[] snap = currentSnapshot();
        expiredScratch.clear();
        for (int i = 0; i < n; i++) {
            LightSource source = snap[i];
            if (source != null && !source.tick()) {
                expiredScratch.add(source);
            }
        }
        if (!expiredScratch.isEmpty()) {
            synchronized (lock) {
                for (int i = 0, e = expiredScratch.size(); i < e; i++) {
                    if (sources.remove(expiredScratch.get(i))) {
                        snapshotDirty = true;
                    }
                }
            }
            expiredScratch.clear();
        }
    }

    private void flickerStep() {
        int n = captureSnapshot();
        LightSource[] snap = currentSnapshot();
        for (int i = 0; i < n; i++) {
            LightSource source = snap[i];
            if (source != null) {
                source.flickerStep(random);
            }
        }
    }

    private void refreshSnapshotLocked() {
        if (!snapshotDirty) {
            return;
        }
        int n = sources.size();
        if (snapshot.length < n) {
            snapshot = new LightSource[Math.max(n, snapshot.length == 0 ? 256 : snapshot.length * 2)];
        }
        for (int i = 0; i < n; i++) {
            snapshot[i] = sources.get(i);
        }
        for (int i = n; i < snapshotSize; i++) {
            snapshot[i] = null;
        }
        snapshotSize = n;
        snapshotDirty = false;
    }
}
