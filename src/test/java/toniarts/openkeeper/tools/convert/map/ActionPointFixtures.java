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

    /**
     * Build a claim or slab type condition with the trigger ID 1.
     *
     * @param type the condition type
     * @param comparison the comparison
     * @param playerId the tile owner, 0 for any
     * @param terrainId the terrain, 0 for none
     * @param value the value to compare the tile count with
     * @return the condition
     */
    public static TriggerGeneric tileCondition(TriggerGeneric.TargetType type,
            TriggerGeneric.ComparisonType comparison, int playerId, int terrainId, int value) {
        TriggerGeneric condition = new TriggerGeneric(null);
        condition.setId(1);
        condition.setRepeatTimes((short) 255);
        condition.setType(type);
        condition.setTargetValueComparison(comparison);
        condition.setUserData("playerId", (short) playerId);
        condition.setUserData("terrainId", (short) terrainId);
        condition.setUserData("value", value);
        return condition;
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
