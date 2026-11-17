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
 * colour. Timed pulses use presentation time; other modes use simulation ticks.
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
    private int pulsePeriodTicks;
    private float pulseElapsedSeconds;
    private int pulseDipTicks;
    private int pulseMinimumScale;

    public Type getType() {
        return type;
    }

    public void pulse() {
        type = Type.PULSE;
        pulsePeriodTicks = 0;
        phase = PULSE_MIN_PHASE;
        rising = true;
    }

    /** Hold the base colour, then briefly dip and recover at the end of each period. */
    public void pulse(int periodTicks, int dipTicks, float minimumBrightness) {
        type = Type.PULSE;
        pulsePeriodTicks = Math.max(2, periodTicks);
        pulseElapsedSeconds = 0;
        pulseDipTicks = Math.clamp(dipTicks, 2, pulsePeriodTicks);
        pulseMinimumScale = Math.round(Math.clamp(minimumBrightness, 0f, 1f) * PULSE_PHASE_RANGE);
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

    /** Advance a timed pulse on the presentation clock, independently of simulation ticks. */
    void updatePresentation(float tpf, int[] base, int[] outColour) {
        if (type != Type.PULSE || pulsePeriodTicks <= 0) {
            return;
        }
        float period = (float) pulsePeriodTicks / LightConstants.SIMULATION_TICKS_PER_SECOND;
        float dip = (float) pulseDipTicks / LightConstants.SIMULATION_TICKS_PER_SECOND;
        pulseElapsedSeconds = (pulseElapsedSeconds + tpf) % period;
        float progress = Math.max(0, pulseElapsedSeconds - (period - dip)) / dip;
        float amount = (1f - (float) Math.cos(2 * Math.PI * progress)) * .5f;
        float scale = 1f - (1f - (float) pulseMinimumScale / PULSE_PHASE_RANGE) * amount;
        for (int i = 0; i < 3; i++) {
            outColour[i] = Math.round(base[i] * scale);
        }
    }

    private void applyPulse(int[] base, int[] outColour) {
        if (pulsePeriodTicks > 0) {
            return;
        }
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
