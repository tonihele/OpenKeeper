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
package toniarts.openkeeper.tools.convert.map;

import java.util.EnumSet;

/**
 * Builds {@link Thing.ActionPoint} fixtures for tests. Lives in the map
 * package because the setters are {@code protected} (parser-only).
 */
public final class ActionPointFixtures {

    private ActionPointFixtures() {
    }

    /**
     * Build a one tile action point
     *
     * @param id the action point id
     * @param waitDelay the wait delay in seconds
     * @param nextWaypointId the next waypoint, 0 for none
     * @return the action point data
     */
    public static Thing.ActionPoint actionPoint(int id, int waitDelay, int nextWaypointId) {
        return rectangle(id, 5, 5, 5, 5, waitDelay, nextWaypointId);
    }

    /**
     * Build a rectangular action point for trigger tests.
     *
     * @param id the action point ID
     * @param startX left map coordinate, inclusive
     * @param startY top map coordinate, inclusive
     * @param endX right map coordinate, inclusive
     * @param endY bottom map coordinate, inclusive
     * @param waitDelay the wait delay in seconds
     * @param nextWaypointId the next waypoint, 0 for none
     * @return the action point data
     */
    public static Thing.ActionPoint rectangle(int id, int startX, int startY, int endX, int endY,
            int waitDelay, int nextWaypointId) {
        Thing.ActionPoint actionPoint = new Thing.ActionPoint();
        actionPoint.setId((short) id);
        actionPoint.setStartX(startX);
        actionPoint.setStartY(startY);
        actionPoint.setEndX(endX);
        actionPoint.setEndY(endY);
        actionPoint.setWaitDelay(waitDelay);
        actionPoint.setNextWaypointId((short) nextWaypointId);
        actionPoint.setFlags(EnumSet.noneOf(Thing.ActionPoint.ActionPointFlag.class));
        return actionPoint;
    }

}
