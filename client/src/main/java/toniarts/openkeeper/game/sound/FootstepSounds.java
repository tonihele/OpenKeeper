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
package toniarts.openkeeper.game.sound;

import javax.annotation.Nullable;
import toniarts.openkeeper.tools.convert.map.Creature;

/**
 * Third person creature footsteps. Every walk and run animation has up to two
 * footstep marks (frames of the animation, from {@code Creatures.kwd}). A step is
 * heard when the animation passes a mark.
 */
public final class FootstepSounds {

    public static final int STEP_A = 0x314;
    public static final int STEP_B = 0x315;
    public static final int WADING_A = 0x337;
    public static final int WADING_B = 0x338;
    public static final int NO_STEP = -1;

    // The animation frames are 1/30 s apart
    private static final float FRAME_DURATION = 1f / 30f;

    // A crowd of creatures must not flood the sound engine, so only a few steps may start per window
    public static final long WINDOW_MS = 200;
    public static final int MAX_STEPS_PER_WINDOW = 3;

    private static long windowStart;
    private static int windowSteps;

    private FootstepSounds() {
        // Static helper
    }

    /**
     * @return the index of the footstep marks of the animation in
     * {@link Creature#getFootstepFrames()}, {@code -1} if the animation has no steps
     */
    public static int getMarkSlot(@Nullable Creature.AnimationType animation) {
        if (animation == null) {
            return -1;
        }

        // The slots follow the order of the animations in Creatures.kwd, but start from 1
        return switch (animation) {
            case WALK ->
                1;
            case RUN ->
                2;
            case RECOIL_FORWARDS ->
                4;
            case ANGRY ->
                9;
            case BACK_OFF ->
                38;
            default ->
                -1;
        };
    }

    /**
     * @return the footstep marks (left, right) of the animation in frames,
     * {@code null} if the animation has none
     */
    @Nullable
    public static int[] getMarks(Creature creature, @Nullable Creature.AnimationType animation) {
        int slot = getMarkSlot(animation);
        Creature.Footstep[] frames = creature.getFootstepFrames();
        if (slot < 0 || frames == null || slot >= frames.length) {
            return null;
        }
        int left = frames[slot].getLeft();
        int right = frames[slot].getRight();
        if (left == 0 && right == 0) {
            return null;
        }
        return new int[]{left, right != 0 ? right : left}; // One step per loop
    }

    /**
     * Checks if the animation passed a footstep mark.
     *
     * @param marks the marks in frames, see {@link #getMarks}
     * @param previousTime the animation time at the previous update, seconds
     * @param currentTime the animation time now, seconds
     * @param length the length of the animation, seconds
     * @return {@link #STEP_A} for a step before the loop wrapped between the two
     * times (all steps if it did not wrap), {@link #STEP_B} for a step after the
     * wrap, {@link #NO_STEP} for none
     */
    public static int getStep(int[] marks, float previousTime, float currentTime, float length) {
        if (length <= 0 || currentTime == previousTime) {
            return NO_STEP;
        }
        float delta = currentTime - previousTime;
        if (Math.abs(delta) < length / 2) {

            // Plain move, also backwards when the animation cycles back and forth
            return isCrossed(marks, Math.min(previousTime, currentTime), Math.max(previousTime, currentTime)) ? STEP_A : NO_STEP;
        }
        if (delta < 0) {

            // Wrapped forwards
            if (isCrossed(marks, previousTime, length)) {
                return STEP_A;
            }
            return isCrossed(marks, -FRAME_DURATION, currentTime) ? STEP_B : NO_STEP;
        }

        // Wrapped backwards
        if (isCrossed(marks, -FRAME_DURATION, previousTime)) {
            return STEP_A;
        }
        return isCrossed(marks, currentTime, length) ? STEP_B : NO_STEP;
    }

    /**
     * Asks for the permission to play a step, steps over the limit of the
     * current window are dropped.
     *
     * @param nowMs current time, milliseconds
     */
    public static synchronized boolean tryAcquire(long nowMs) {
        if (nowMs - windowStart >= WINDOW_MS) {
            windowStart = nowMs;
            windowSteps = 0;
        }
        if (windowSteps >= MAX_STEPS_PER_WINDOW) {
            return false;
        }
        windowSteps++;
        return true;
    }

    /**
     * @return the event of a step, in the water it is the wading one
     */
    public static int getEvent(int step, boolean inWater) {
        if (!inWater) {
            return step;
        }
        return step == STEP_B ? WADING_B : WADING_A;
    }

    private static boolean isCrossed(int[] marks, float from, float to) {
        for (int mark : marks) {
            float time = mark * FRAME_DURATION;
            if (time > from && time <= to) {
                return true;
            }
        }
        return false;
    }
}
