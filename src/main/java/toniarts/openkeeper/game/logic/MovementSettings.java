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
package toniarts.openkeeper.game.logic;

import toniarts.openkeeper.utils.WorldUtils;

/**
 * Runtime tuning for authoritative movement and client presentation.
 * Simulation time comes from the game level tick rate; these settings control
 * tolerances and view correction without changing that rate.
 *
 * @param waypointTolerance distance at which a route waypoint is considered reached
 * @param facingTolerance angular error at which a facing request is complete, in radians
 * @param turnRate maximum turn rate while stationary, in radians per second
 * @param wallApproachGap distance held back from an impassable tile edge
 * @param collisionStep maximum axis distance checked at one time by the grid sweep
 * @param interpolationDelay seconds behind the latest local receive time to render
 * @param maxExtrapolation maximum time to project beyond the newest received position
 * @param teleportDistance corrections larger than this reset interpolation history
 */
public record MovementSettings(float waypointTolerance, float facingTolerance, float turnRate,
        float wallApproachGap, float collisionStep, double interpolationDelay,
        double maxExtrapolation, float teleportDistance) {

    /**
     * Defaults match the old steering tolerances and the movement-history
     * reference used by OpenKeeper's issue discussion.
     */
    public static final MovementSettings DEFAULT = new MovementSettings(
            0.1f,
            0.1f,
            20f,
            WorldUtils.TILE_WIDTH / 6f,
            WorldUtils.TILE_WIDTH / 4f,
            0.1,
            0.05,
            WorldUtils.TILE_WIDTH * 2f);

    public MovementSettings {
        if (!Float.isFinite(waypointTolerance) || waypointTolerance < 0f
                || !Float.isFinite(facingTolerance) || facingTolerance < 0f
                || !Float.isFinite(turnRate) || turnRate <= 0f
                || !Float.isFinite(wallApproachGap) || wallApproachGap < 0f
                || wallApproachGap >= WorldUtils.TILE_WIDTH / 2f
                || !Float.isFinite(collisionStep) || collisionStep <= 0f
                || collisionStep > WorldUtils.TILE_WIDTH / 2f
                || !Double.isFinite(interpolationDelay) || interpolationDelay < 0d
                || !Double.isFinite(maxExtrapolation) || maxExtrapolation < 0d
                || !Float.isFinite(teleportDistance) || teleportDistance <= 0f) {
            throw new IllegalArgumentException("Invalid movement settings");
        }
    }
}
