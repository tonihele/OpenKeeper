package toniarts.openkeeper.game.sound;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FootstepSoundsTest {

    private static final float LENGTH = 1f; // 30 frames
    private static final int[] MARKS = {6, 21}; // 0.2 s and 0.7 s

    @Test
    void stepWhenMarkIsPassed() {
        assertEquals(FootstepSounds.STEP_A, FootstepSounds.getStep(MARKS, 0.15f, 0.25f, LENGTH));
        assertEquals(FootstepSounds.STEP_A, FootstepSounds.getStep(MARKS, 0.65f, 0.75f, LENGTH));
    }

    @Test
    void noStepBetweenMarks() {
        assertEquals(FootstepSounds.NO_STEP, FootstepSounds.getStep(MARKS, 0.3f, 0.5f, LENGTH));
        assertEquals(FootstepSounds.NO_STEP, FootstepSounds.getStep(MARKS, 0.5f, 0.5f, LENGTH));
    }

    @Test
    void wrappedLoopReportsWhichSideHadTheStep() {
        assertEquals(FootstepSounds.STEP_A, FootstepSounds.getStep(MARKS, 0.65f, 0.1f, LENGTH));
        assertEquals(FootstepSounds.STEP_B, FootstepSounds.getStep(MARKS, 0.75f, 0.25f, LENGTH));
        assertEquals(FootstepSounds.NO_STEP, FootstepSounds.getStep(MARKS, 0.75f, 0.1f, LENGTH));
    }
}
