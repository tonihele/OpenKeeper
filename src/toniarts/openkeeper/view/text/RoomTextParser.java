/*
 * Copyright (C) 2014-2024 OpenKeeper
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
package toniarts.openkeeper.view.text;

import com.simsilica.es.EntityId;
import toniarts.openkeeper.game.map.IRoomInformation;
import toniarts.openkeeper.game.map.IRoomsInformation;
import toniarts.openkeeper.utils.TextUtils;

/**
 * Parses text and fills the replacements from room data
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class RoomTextParser {

    private final IRoomsInformation roomsInformation;

    public RoomTextParser(IRoomsInformation roomsInformation) {
        this.roomsInformation = roomsInformation;
    }

    public String parseText(String text, EntityId room) {
        return TextUtils.parseText(text, (index) -> {
            return getReplacement(index, roomsInformation.getRoomInformation(room));
        });
    }

    protected String getReplacement(int index, IRoomInformation room) {
        switch (index) {
            case 37:
                return Integer.toString(room.getHealthPercent()); // Health
            case 38:
                return ""; // Used capacity
            case 39:
                return ""; // Max capacity
            case 44: // Portal
            case 45: // Lairs
            case 46: // Hatchery
            case 47: // Treasure
            case 48: // Library
            case 49: // Training Room
            case 50: // Workshop
            case 51: // Guard Rooms
            case 53: // Combat Pit
            case 54: // Torture
            case 58: // Prison
            case 61: // Graveyard
            case 62: // Temple
            case 63: // Casino
                return "0"; // amount
        }

        return "Parameter " + index + " not implemented!";

//                if (roomInstance.getOwnerId() != playerId) {
//            return notOwnedTooltip;
//        }
    }

}
