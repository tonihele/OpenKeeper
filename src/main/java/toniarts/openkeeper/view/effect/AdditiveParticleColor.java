/*
 * Copyright (C) 2026 OpenKeeper
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
package toniarts.openkeeper.view.effect;

import com.jme3.effect.Particle;
import com.jme3.math.ColorRGBA;
import java.util.IdentityHashMap;
import java.util.Map;

/** Native additive colour stepping; alpha never attenuates these draws. */
final class AdditiveParticleColor {
    private final int red;
    private final int green;
    private final int blue;
    private final boolean fade;
    private final Map<Particle, State> states = new IdentityHashMap<>();

    AdditiveParticleColor(ColorRGBA color, boolean fade) {
        red = Math.round(color.r * 255f);
        green = Math.round(color.g * 255f);
        blue = Math.round(color.b * 255f);
        this.fade = fade;
    }

    void update(Particle particle, boolean justSpawned) {
        State state = states.get(particle);
        if (state == null || justSpawned) {
            state = new State(red, green, blue);
            states.put(particle, state);
        }
        // Native generators/particles update on 50 ms boundaries. A delayed
        // update decrements life by all elapsed steps but fades RGB only once.
        int step = (int) Math.floor((particle.startlife - particle.life) * 20f + 0.0001f);
        if (step > state.step) {
            int remaining = Math.round(particle.startlife * 20f) - step;
            if (fade && remaining > 0 && Math.max(state.red, Math.max(state.green, state.blue)) >= remaining) {
                // Exact integer-channel recurrence from 0x43652a..0x436563.
                state.red = state.red * (remaining - 1) / remaining;
                state.green = state.green * (remaining - 1) / remaining;
                state.blue = state.blue * (remaining - 1) / remaining;
            }
            state.step = step;
        }
        particle.color.set(state.red / 255f, state.green / 255f, state.blue / 255f, 1f);
    }

    private static final class State {
        int red;
        int green;
        int blue;
        int step;

        State(int red, int green, int blue) {
            this.red = red;
            this.green = green;
            this.blue = blue;
        }
    }
}
