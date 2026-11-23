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
package toniarts.openkeeper.game.component;

import com.simsilica.es.EntityComponent;
import java.util.Objects;
import toniarts.openkeeper.utils.Point;

/**
 * Marks a creature that found no way to a place. Planning is by far the most
 * expensive thing a creature does, and a hero sealed in with an objective would
 * do it on every tick, so it waits for a while before it plans a route to the
 * same place again. The original game's creatures also wait for a while before
 * they look again after finding nothing to do.
 */
public final class PathRetry implements EntityComponent {

    /**
     * How long, in seconds, to wait before searching again for a goal that had
     * no way to it
     */
    public static final double RETRY_DELAY = 5.0;

    public Point goal;
    public double failedAt;

    public PathRetry() {
        // For serialization
    }

    public PathRetry(Point goal, double failedAt) {
        this.goal = goal;
        this.failedAt = failedAt;
    }

    /**
     * Tells whether a search for the given goal should be skipped for now
     *
     * @param goal where the creature wants to go
     * @param now the game time
     * @return true if the search for this goal failed lately
     */
    public boolean isWaiting(Point goal, double now) {
        return Objects.equals(this.goal, goal) && now - failedAt < RETRY_DELAY;
    }

}
