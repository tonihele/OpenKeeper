/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static toniarts.openkeeper.animation.MotionTrace.*;

class MotionTraceTest {
    @Test void tracesAreStableAndIndependentOfRendering() throws Exception {
        for (Scenario scenario : Scenario.values()) {
            var a = new StringWriter(); var b = new StringWriter();
            write(a, scenario, 181, 1.0 / 60); write(b, scenario, 181, 1.0 / 60);
            assertEquals(a.toString(), b.toString());
            assertEquals(182, a.toString().lines().count());
            for (int tick = 0; tick <= 180; tick++) {
                var s = sample(scenario, tick, 1.0 / 60);
                assertEquals(tick, s.sequence()); assertTrue(Double.isFinite(s.x()));
            }
        }
    }
    @Test void teleportAndPauseHaveExplicitSemantics() {
        assertEquals(0, sample(Scenario.TELEPORT, 59, 1.0 / 60).epoch());
        assertTrue(sample(Scenario.TELEPORT, 60, 1.0 / 60).discontinuity());
        assertFalse(sample(Scenario.TELEPORT, 61, 1.0 / 60).discontinuity());
        assertEquals(11, sample(Scenario.TELEPORT, 60, 1.0 / 60).x());
        assertEquals(1, sample(Scenario.PAUSE, 90, 1.0 / 60).phaseTime());
        assertEquals(1, sample(Scenario.PAUSE, 120, 1.0 / 60).phaseTime());
        assertEquals(1.5, sample(Scenario.PAUSE, 150, 1.0 / 60).phaseTime());
        assertEquals(1, sample(Scenario.BLOCKED, 180, 1.0 / 60).x());
        assertEquals(1.5, sample(Scenario.ACCELERATE_STOP, 180, 1.0 / 60).x());
    }
    @Test void invalidInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> sample(null, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> sample(Scenario.CONSTANT, -1, 1));
        for (double step : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class, () -> sample(Scenario.CONSTANT, 0, step));
    }
}
