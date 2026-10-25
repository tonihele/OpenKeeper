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
package toniarts.openkeeper.game.controller;

import toniarts.openkeeper.tools.convert.map.Room;

/**
 * What selling gives back, shared by the server that sells and the client
 * that shows the value next to the hand
 */
public final class SellValue {

    private SellValue() {
        // Nope
    }

    /**
     * The gold one sold room tile gives back
     *
     * @param room the room
     * @param percentageOfCost the level's room sell value, percentage of the
     * cost
     * @return the gold for the tile
     */
    public static int ofRoomTile(Room room, float percentageOfCost) {
        return (int) (room.getCost() * (percentageOfCost / 100));
    }

}
