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
package toniarts.openkeeper.game.controller.shot;

import com.jme3.math.Vector3f;
import toniarts.openkeeper.tools.convert.map.Shot;

/**
 * How a shot flies, as DKII.EXE moves its shots in the shot's turn (0x4AB180).
 * All the distances are in tiles and the velocities in tiles per turn.
 */
public final class ShotFlightRules {

    /**
     * The move of a turn is split into steps of at most a quarter tile, at
     * most 16 of them (0x4B2030)
     */
    private static final float MAX_STEP = 0.25f;
    private static final int MAX_STEPS = 16;

    private ShotFlightRules() {
        // Nope
    }

    /**
     * Whether the shot flies to its target. The others hit at once.
     *
     * @param shot the shot
     * @return does the shot fly
     */
    public static boolean isFlying(Shot shot) {
        return shot.getFlags().contains(Shot.ShotFlag.MOVE_TOWARDS_TARGET) && shot.getSpeed() > 0;
    }

    /**
     * The velocity of a shot aimed at a spot. The shot's speed is how far it
     * gets in one turn (0x4AB8A0).
     *
     * @param from where the shot is
     * @param to where the shot is aimed at
     * @param speed the shot's speed
     * @return the velocity, in tiles per turn
     */
    public static Vector3f aim(Vector3f from, Vector3f to, float speed) {
        Vector3f direction = to.subtract(from);
        if (direction.lengthSquared() == 0) {
            return direction;
        }

        return direction.normalizeLocal().multLocal(speed);
    }

    /**
     * In how many steps the shot moves in one turn
     *
     * @param velocity the shot's velocity
     * @return the number of steps
     */
    public static int getSteps(Vector3f velocity) {
        return Math.min(MAX_STEPS, (int) (velocity.length() / MAX_STEP) + 1);
    }

    /**
     * Whether a shot touches a thing. Both are boxes: the shot is twice its
     * radius wide and high, the thing stands on its position (0x4B5590).
     *
     * @param shotPosition where the shot is
     * @param shotRadius the shot's radius
     * @param thingPosition where the thing stands
     * @param thingWidth how wide the thing is
     * @param thingHeight how high the thing is
     * @return does the shot touch the thing
     */
    public static boolean touches(Vector3f shotPosition, float shotRadius, Vector3f thingPosition, float thingWidth, float thingHeight) {
        float shotSize = shotRadius * 2;
        float reach = (shotSize + thingWidth) / 2;

        return Math.abs(shotPosition.x - thingPosition.x) < reach
                && Math.abs(shotPosition.z - thingPosition.z) < reach
                && Math.abs(shotPosition.y - thingPosition.y) < shotSize + thingHeight;
    }

    /**
     * A creature's shot dies once it is further from where it was fired than
     * the creature's spell reaches. Only the distance over the floor counts
     * (0x4AB180).
     *
     * @param start where the shot was fired
     * @param position where the shot is
     * @param maxRange the range, 0 for none
     * @return is the shot out of its range
     */
    public static boolean isOutOfRange(Vector3f start, Vector3f position, float maxRange) {
        if (maxRange <= 0) {
            return false;
        }
        float dx = position.x - start.x;
        float dz = position.z - start.z;

        return dx * dx + dz * dz > maxRange * maxRange;
    }

}
