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
package toniarts.openkeeper.game.controller.creature;

import java.util.Objects;
import toniarts.openkeeper.utils.Point;

/**
 * Keeps a creature from planning a route again and again to a place that it
 * found no way to. Planning is by far the most expensive thing a creature
 * does, and a hero sealed in with an objective would do it on every tick. The
 * original game's creatures also wait for a while before they look again
 * after finding nothing to do.
 */
final class PathRetryBackoff {

    /**
     * How long, in seconds, to wait before searching again for a goal that
     * had no way to it
     */
    static final double RETRY_DELAY = 5.0;

    private Point failedGoal;
    private double failedAt;

    /**
     * Tells whether a search for the given goal should be skipped for now
     *
     * @param goal where the creature wants to go
     * @param now the game time
     * @return true if the search for this goal failed lately
     */
    boolean isWaiting(Point goal, double now) {
        return failedGoal != null && Objects.equals(failedGoal, goal) && now - failedAt < RETRY_DELAY;
    }

    /**
     * Notes that there was no way to the goal
     *
     * @param goal where the creature wanted to go
     * @param now the game time
     */
    void failed(Point goal, double now) {
        failedGoal = goal;
        failedAt = now;
    }

    /**
     * Notes that a way was found, so nothing is held back
     */
    void succeeded() {
        failedGoal = null;
    }

}
