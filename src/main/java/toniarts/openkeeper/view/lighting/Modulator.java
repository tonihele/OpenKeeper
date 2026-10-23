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

import java.util.Arrays;

/**
 * A time-varying multiplier/override on a {@link LightSource}'s current
 * colour, advanced once per simulation tick.
 */
public final class Modulator {

    private static final int PULSE_STEP = 10;
    private static final int PULSE_MIN_PHASE = 10;
    private static final int PULSE_MAX_PHASE = 250;
    private static final int PULSE_PHASE_RANGE = 256;

    public enum Type {
        NONE,
        PULSE,
        FADE_IN,
        FADE_OUT,
        FLASH
    }

    private Type type = Type.NONE;
    private int duration;
    private int counter;
    private int phase = PULSE_MIN_PHASE;
    private boolean rising = true;

    public Type getType() {
        return type;
    }

    public void pulse() {
        type = Type.PULSE;
        phase = PULSE_MIN_PHASE;
        rising = true;
    }

    public void fadeIn(int ticks) {
        type = Type.FADE_IN;
        duration = Math.max(ticks, 0);
        counter = 0;
    }

    public void fadeOut(int ticks) {
        type = Type.FADE_OUT;
        duration = Math.max(ticks, 0);
        counter = duration;
    }

    /**
     * Fades in over {@code ticks / 2}, then out over {@code ticks / 2}.
     */
    public void flash(int ticks) {
        type = Type.FLASH;
        duration = Math.max(ticks / 2, 0);
        counter = 0;
    }

    /**
     * Advances the modulator by one simulation tick and writes the current
     * colour into {@code outColour}, reading the base colour from
     * {@code base}. Both arrays are {@code [r, g, b]}, 0..{@link LightConstants#MAX_COLOUR}.
     */
    public void apply(int[] base, int[] outColour) {
        switch (type) {
            case PULSE -> applyPulse(base, outColour);
            case FADE_IN, FLASH -> applyFadeIn(base, outColour);
            case FADE_OUT -> applyFadeOut(base, outColour);
            default -> {
                outColour[0] = base[0];
                outColour[1] = base[1];
                outColour[2] = base[2];
            }
        }
    }

    private void applyPulse(int[] base, int[] outColour) {
        // The original fades the colour towards black and swaps green and blue;
        // that reads as the colour disappearing, so here the pulse only
        // brightens the base colour (up to about twice as vibrant)
        int scale = PULSE_PHASE_RANGE + phase;
        for (int i = 0; i < 3; i++) {
            outColour[i] = Math.min(base[i] * scale / PULSE_PHASE_RANGE, LightConstants.MAX_COLOUR);
        }

        // Reflect off the turnaround points instead of clamping-and-repeating
        // for one extra tick -- clamping would output the boundary value
        // twice in a row and stretch the period to 49 ticks; reflecting is
        // what actually yields the documented 4%..98% range over an exact
        // 48-tick period.
        int raw = phase + (rising ? PULSE_STEP : -PULSE_STEP);
        if (raw > PULSE_MAX_PHASE) {
            raw = PULSE_MAX_PHASE - (raw - PULSE_MAX_PHASE);
            rising = false;
        } else if (raw < PULSE_MIN_PHASE) {
            raw = PULSE_MIN_PHASE + (PULSE_MIN_PHASE - raw);
            rising = true;
        }
        phase = raw;
    }

    private void applyFadeIn(int[] base, int[] outColour) {
        if (duration <= 0) {
            System.arraycopy(base, 0, outColour, 0, 3);
            type = Type.NONE;
            return;
        }
        scaleColour(base, outColour);
        if (counter < duration) {
            counter++;
        } else if (type == Type.FLASH) {
            type = Type.FADE_OUT;
            counter = duration;
        } else {
            type = Type.NONE;
        }
    }

    private void applyFadeOut(int[] base, int[] outColour) {
        if (duration <= 0) {
            Arrays.fill(outColour, 0);
            type = Type.NONE;
            return;
        }
        scaleColour(base, outColour);
        if (counter > 0) {
            counter--;
        } else {
            type = Type.NONE;
        }
    }

    private void scaleColour(int[] base, int[] outColour) {
        for (int i = 0; i < 3; i++) {
            outColour[i] = base[i] * counter / duration;
        }
    }
}
