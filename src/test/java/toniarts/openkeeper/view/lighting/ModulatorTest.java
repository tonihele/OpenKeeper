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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Modulator} against {@code lighting_design.md} &sect;7.1.
 */
public class ModulatorTest {

    @Test
    public void pulsePeriodIs48TicksAndStaysInRange() {
        Modulator modulator = new Modulator();
        modulator.pulse();
        int[] base = {400, 100, 200}; // distinct r/g/b so the swap is visible
        int[] colour = new int[3];

        // One full period is 48 ticks (10 -> 250 -> 10 in steps of 10).
        for (int i = 0; i < 48; i++) {
            modulator.apply(base, colour);

            // colour must never exceed base (phase/256 <= 250/256 < 1)
            assertTrue(colour[0] <= base[0]);
            assertTrue(colour[1] <= base[2]); // green channel takes blue's base (D6)
            assertTrue(colour[2] <= base[1]); // blue channel takes green's base (D6)
        }

        // After exactly one period (48 ticks) the phase/direction must repeat.
        Modulator fresh = new Modulator();
        fresh.pulse();
        int[] colourAtStart = new int[3];
        fresh.apply(base, colourAtStart);
        for (int i = 0; i < 47; i++) {
            fresh.apply(base, colour);
        }
        int[] colourAfterOnePeriod = new int[3];
        fresh.apply(base, colourAfterOnePeriod);
        assertEquals(colourAtStart[0], colourAfterOnePeriod[0]);
        assertEquals(colourAtStart[1], colourAfterOnePeriod[1]);
        assertEquals(colourAtStart[2], colourAfterOnePeriod[2]);
    }

    @Test
    public void pulseSwapsGreenAndBlueChannels() {
        Modulator modulator = new Modulator();
        modulator.pulse();
        int[] base = {0, 300, 100};
        int[] colour = new int[3];

        modulator.apply(base, colour);

        // phase starts at 10: colour = base * 10 / 256
        assertEquals(base[0] * 10 / 256, colour[0]);
        assertEquals(base[2] * 10 / 256, colour[1]); // green output uses base blue
        assertEquals(base[1] * 10 / 256, colour[2]); // blue output uses base green
    }

    @Test
    public void pulseRangeIsRoughly4To98Percent() {
        Modulator modulator = new Modulator();
        modulator.pulse();
        int[] base = {256, 256, 256};
        int[] colour = new int[3];

        int minR = Integer.MAX_VALUE;
        int maxR = Integer.MIN_VALUE;
        for (int i = 0; i < 480; i++) { // several full periods
            modulator.apply(base, colour);
            minR = Math.min(minR, colour[0]);
            maxR = Math.max(maxR, colour[0]);
        }

        // 10/256 ~= 3.9%, 250/256 ~= 97.7%
        assertEquals(10, minR);
        assertEquals(base[0] * 250 / 256, maxR);
    }

    @Test
    public void fadeInReachesBaseExactlyAtDuration() {
        // colour = base * counter / duration, computed BEFORE counter is
        // incremented -- so it takes duration calls at counter=0..duration-1
        // (all < base) plus one more (at counter==duration) to reach base.
        Modulator modulator = new Modulator();
        int duration = 10;
        modulator.fadeIn(duration);
        int[] base = {510, 100, 0};
        int[] colour = new int[3];

        for (int i = 0; i < duration; i++) {
            modulator.apply(base, colour);
            assertTrue(colour[0] < base[0]);
        }
        modulator.apply(base, colour);
        assertEquals(base[0], colour[0]);
        assertEquals(base[1], colour[1]);
        assertEquals(base[2], colour[2]);
        assertEquals(Modulator.Type.NONE, modulator.getType());
    }

    @Test
    public void fadeOutReachesZeroAndStops() {
        // fadeOut starts with counter == duration, so the very first call
        // outputs full base; it takes duration+1 calls total to reach 0.
        Modulator modulator = new Modulator();
        int duration = 4;
        modulator.fadeOut(duration);
        int[] base = {400, 200, 100};
        int[] colour = new int[3];

        modulator.apply(base, colour); // counter=4 -> full base
        assertEquals(base[0], colour[0]);

        for (int i = 0; i < duration; i++) {
            modulator.apply(base, colour);
        }
        assertEquals(0, colour[0]);
        assertEquals(0, colour[1]);
        assertEquals(0, colour[2]);
        assertEquals(Modulator.Type.NONE, modulator.getType());
    }

    @Test
    public void flashRampsUpThenDownOverHalvesOfDuration() {
        // flash(8) fades in over 4 ticks, then out over 4 ticks (design
        // §6.6): duration+1 calls to ramp up to full and flip to FADE_OUT,
        // then duration+1 more to ramp back down to 0.
        Modulator modulator = new Modulator();
        int half = 4;
        modulator.flash(half * 2);
        int[] base = {400, 0, 0};
        int[] colour = new int[3];

        modulator.apply(base, colour); // counter 0 -> 0
        assertEquals(0, colour[0]);
        for (int i = 0; i < half - 1; i++) {
            modulator.apply(base, colour);
        }
        modulator.apply(base, colour); // counter reaches duration -> full, switches to FADE_OUT
        assertEquals(base[0], colour[0]);
        assertEquals(Modulator.Type.FADE_OUT, modulator.getType());

        // Ramp down back to 0. The switch to FADE_OUT above already emitted
        // one full-base output (from the FADE_IN side); FADE_OUT then
        // starts its own counter at `duration` too, so its first output is
        // ALSO full (matching fadeOutReachesZeroAndStops above) before it
        // starts counting down -- half+1 more calls to reach 0.
        for (int i = 0; i < half + 1; i++) {
            modulator.apply(base, colour);
        }
        assertEquals(0, colour[0]);
        assertEquals(Modulator.Type.NONE, modulator.getType());
    }

    @Test
    public void noneModulatorAlwaysReturnsBase() {
        Modulator modulator = new Modulator();
        int[] base = {123, 45, 67};
        int[] colour = new int[3];

        modulator.apply(base, colour);

        assertEquals(base[0], colour[0]);
        assertEquals(base[1], colour[1]);
        assertEquals(base[2], colour[2]);
    }
}
