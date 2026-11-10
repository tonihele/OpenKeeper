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

import com.jme3.math.FastMath;

/**
 * The first person walk cycle (head bob, sway roll) of a possessed creature.
 * Everything is a pure function of the walk time, the clock is only advanced
 * while the creature is moving (or always for flyers, so they hover).
 * Presentation only, see possession_walk_cycle_design.md.
 */
public final class PossessionWalkCycle {

    /**
     * Gait cycles per second for each unit of the walk cycle scale
     */
    private static final float BASE_HZ = 0.3125f;
    /**
     * The bob runs at twice the sway frequency (once per step)
     */
    private static final float BOB_FREQUENCY_MULTIPLIER = 2f;
    /**
     * Sway roll in degrees for a waddle scale of 1
     */
    private static final float ROLL_DEG_PER_WADDLE = 2.8f;
    /**
     * Tunable, not verified against the original: bob height in tiles for an
     * oscillate scale of 1
     */
    private static final float BOB_HEIGHT_PER_OSCILLATE = 0.125f;
    private static final float BACKWARD_BOB_FACTOR = 0.5f;

    private static final float TURN_ROLL_MAX_DEG = 11.25f;
    /**
     * The turn roll decays by 50% per 1/30 s
     */
    private static final float TURN_ROLL_DECAY_HALF_LIFE = 1f / 30f;

    private final float walkCycleScale;
    private final float waddleScale;
    private final float oscillateScale;
    private final boolean flying;
    private final boolean rollsWhenTurning;

    private float time;
    private float turnRoll;

    public PossessionWalkCycle(float walkCycleScale, float waddleScale, float oscillateScale, boolean flying,
            boolean rollsWhenTurning) {
        this.walkCycleScale = walkCycleScale;
        this.waddleScale = waddleScale;
        this.oscillateScale = oscillateScale;
        this.flying = flying;
        this.rollsWhenTurning = rollsWhenTurning;
    }

    /**
     * Advance the walk clock. Standing still resets the cycle immediately
     * (flyers keep hovering).
     */
    public void update(float tpf, boolean moving) {
        if (moving || flying) {
            time += tpf;
        } else {
            time = 0;
        }

        // Exponential return to level
        turnRoll *= FastMath.pow(0.5f, tpf / TURN_ROLL_DECAY_HALF_LIFE);
    }

    /**
     * Add to the turn roll, if the creature is of the type that rolls when
     * turning
     *
     * @param degrees roll to add, positive is the same direction as the walk
     * roll
     */
    public void addTurnRoll(float degrees) {
        if (rollsWhenTurning) {
            turnRoll = FastMath.clamp(turnRoll + degrees, -TURN_ROLL_MAX_DEG, TURN_ROLL_MAX_DEG);
        }
    }

    /**
     * Sway phase in radians, the frequency depends only on the walk cycle
     * scale, not on the speed
     */
    public float getSwayPhase() {
        return FastMath.TWO_PI * BASE_HZ * walkCycleScale * time;
    }

    /**
     * @return the sway, -1..1
     */
    public float getSway() {
        return FastMath.sin(getSwayPhase());
    }

    /**
     * @return the current camera roll in degrees, walk and turn roll summed
     */
    public float getRollDegrees() {
        float walkRoll = flying ? 0 : getSway() * waddleScale * ROLL_DEG_PER_WADDLE;
        return walkRoll + turnRoll;
    }

    /**
     * @param movingBackward whether the creature is moving backwards, halves
     * the amplitude
     * @return the vertical camera offset from the eye height, in tiles
     */
    public float getBobHeight(boolean movingBackward) {
        float bob = FastMath.sin(getSwayPhase() * BOB_FREQUENCY_MULTIPLIER) * oscillateScale * BOB_HEIGHT_PER_OSCILLATE;
        return movingBackward ? bob * BACKWARD_BOB_FACTOR : bob;
    }

    public void reset() {
        time = 0;
        turnRoll = 0;
    }

}
