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
        Thing.ActionPoint actionPoint = new Thing.ActionPoint();
        actionPoint.setId((short) id);
        actionPoint.setStartX(5);
        actionPoint.setStartY(5);
        actionPoint.setEndX(5);
        actionPoint.setEndY(5);
        actionPoint.setWaitDelay(waitDelay);
        actionPoint.setNextWaypointId((short) nextWaypointId);
        actionPoint.setFlags(EnumSet.noneOf(Thing.ActionPoint.ActionPointFlag.class));
        return actionPoint;
    }

    /**
     * Build an action point covering an area, with no wait delay and no next
     * waypoint
     *
     * @param id the action point id
     * @param startX the first column
     * @param startY the first row
     * @param endX the last column
     * @param endY the last row
     * @return the action point data
     */
    public static Thing.ActionPoint actionPoint(int id, int startX, int startY, int endX, int endY) {
        Thing.ActionPoint actionPoint = actionPoint(id, 0, 0);
        actionPoint.setStartX(startX);
        actionPoint.setStartY(startY);
        actionPoint.setEndX(endX);
        actionPoint.setEndY(endY);
        return actionPoint;
    }

}
