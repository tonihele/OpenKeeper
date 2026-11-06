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
 * Builds {@link Thing.ActionPoint} fixtures for the action point trigger
 * tests. Lives in the map package because the setters are {@code protected}
 * (parser-only).
 */
public final class ApTriggerFixtures {

    private ApTriggerFixtures() {
    }

    /**
     * Build an action point
     *
     * @param id the action point id
     * @param startX first tile column
     * @param startY first tile row
     * @param endX last tile column
     * @param endY last tile row
     * @return the action point data
     */
    public static Thing.ActionPoint actionPoint(int id, int startX, int startY, int endX, int endY) {
        Thing.ActionPoint actionPoint = new Thing.ActionPoint();
        actionPoint.setId((short) id);
        actionPoint.setStartX(startX);
        actionPoint.setStartY(startY);
        actionPoint.setEndX(endX);
        actionPoint.setEndY(endY);
        actionPoint.setFlags(EnumSet.noneOf(Thing.ActionPoint.ActionPointFlag.class));
        return actionPoint;
    }

}
