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

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Drives every live {@link LightSource}'s modulators on the two clocks
 * the lighting needs: simulation ticks (pulse/fade/lifetime)
 * and a fixed 30&nbsp;Hz presentation clock (flicker). Owns its own
 * {@link Random} stream -- this must never share the simulation's RNG
 * state.
 */
public final class LightModulators {

    // Copy-on-write: the map loader thread adds torch/terrain lights while the
    // render thread may already be adding the cursor light or iterating.
    private final List<LightSource> sources = new CopyOnWriteArrayList<>();
    private final Random random = new Random();
    private float tickAccumulator;
    private float flickerAccumulator;

    public void add(LightSource source) {
        sources.add(source);
    }

    public void remove(LightSource source) {
        sources.remove(source);
    }

    /**
     * Every currently-live source, for {@link SceneLights} to read each
     * frame.
     */
    public List<LightSource> getSources() {
        return Collections.unmodifiableList(sources);
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
        for (int i = sources.size() - 1; i >= 0; i--) {
            LightSource source = sources.get(i);
            if (!source.tick()) {
                sources.remove(i);
            }
        }
    }

    private void flickerStep() {
        for (LightSource source : sources) {
            source.flickerStep(random);
        }
    }
}
