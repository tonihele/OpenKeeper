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
package toniarts.openkeeper.view;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The possessed creature's footsteps. DK II (0x40E910) times them on the walk
 * cycle one second of walk time ahead of the sway: the right foot at a quarter
 * of each cycle, the left at three quarters, and none for a flyer
 */
class PossessionWalkCycleTest {

    private static final float TPF = 0.01f;

    @Test
    void stepsRightThenLeftHalfACycleApart() {

        // A walk cycle scale of 3 is 0.9375 cycles a second, the first right step comes at 1.25 cycles
        PossessionWalkCycle walkCycle = new PossessionWalkCycle(3, 1, 1, false, false);
        List<Float> right = new ArrayList<>();
        List<Float> left = new ArrayList<>();
        walk(walkCycle, 2, right, left);

        assertSteps(List.of(1.25f / 0.9375f - 1, 2.25f / 0.9375f - 1), right);
        assertSteps(List.of(1.75f / 0.9375f - 1, 2.75f / 0.9375f - 1), left);
    }

    @Test
    void aFastWalkerStepsWithBothFeetAsItStarts() {
        PossessionWalkCycle imp = new PossessionWalkCycle(12, 1, 1, false, false);
        imp.update(TPF, true);
        assertEquals(PossessionWalkCycle.LEFT_STEP | PossessionWalkCycle.RIGHT_STEP, imp.takeSteps());
        assertEquals(0, imp.takeSteps());
    }

    @Test
    void stoppingStartsTheStepsOver() {
        PossessionWalkCycle walkCycle = new PossessionWalkCycle(3, 1, 1, false, false);
        walk(walkCycle, 3, new ArrayList<>(), new ArrayList<>());
        walkCycle.update(TPF, false);
        assertEquals(0, walkCycle.takeSteps());

        List<Float> right = new ArrayList<>();
        walk(walkCycle, 0.5f, right, new ArrayList<>());
        assertSteps(List.of(1.25f / 0.9375f - 1), right);
    }

    @Test
    void aFlyerMakesNoSteps() {
        PossessionWalkCycle flyer = new PossessionWalkCycle(3, 1, 1, true, false);
        for (int i = 0; i < 500; i++) {
            flyer.update(TPF, true);
            assertEquals(0, flyer.takeSteps());
        }
    }

    private static void walk(PossessionWalkCycle walkCycle, float seconds, List<Float> right, List<Float> left) {
        float time = 0;
        for (int i = 0; i < Math.round(seconds / TPF); i++) {
            walkCycle.update(TPF, true);
            time += TPF;
            int steps = walkCycle.takeSteps();
            if ((steps & PossessionWalkCycle.RIGHT_STEP) != 0) {
                right.add(time);
            }
            if ((steps & PossessionWalkCycle.LEFT_STEP) != 0) {
                left.add(time);
            }
        }
    }

    private static void assertSteps(List<Float> expected, List<Float> actual) {
        assertEquals(expected.size(), actual.size(), "steps " + actual);
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), actual.get(i), TPF * 1.5f, "steps " + actual);
        }
    }
}
