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
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.utils.TextUtils;

/**
 * Parses text and fills the replacements from room data
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class RoomTextParser {

    private final KwdFile kwdFile;
    private final IRoomsInformation roomsInformation;

    public RoomTextParser(KwdFile kwdFile, IRoomsInformation roomsInformation) {
        this.kwdFile = kwdFile;
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
                return Integer.toString(room.getUsedCapacity()); // Used capacity
            case 39:
                return Integer.toString(room.getMaxCapacity()); // Max capacity
            case 44:
                return getRoomAmount(room, 3); // Portal
            case 45:
                return getRoomAmount(room, 2); // Lair
            case 46:
                return getRoomAmount(room, 4); // Hatchery
            case 47:
                return getRoomAmount(room, 1); // Treasury
            case 48:
                return getRoomAmount(room, 6); // Library
            case 49:
                return getRoomAmount(room, 7); // Training Room
            case 50:
                return getRoomAmount(room, 10); // Workshop
            case 51:
                return getRoomAmount(room, 9); // Guard Room
            case 53:
                return getRoomAmount(room, 16); // Combat Pit
            case 54:
                return getRoomAmount(room, 12); // Torture
            case 58:
                return getRoomAmount(room, 11); // Prison
            case 61:
                return getRoomAmount(room, 14); // Graveyard
            case 62:
                return getRoomAmount(room, 13); // Temple
            case 63:
                return getRoomAmount(room, 15); // Casino
        }

        return "Parameter " + index + " not implemented!";

//                if (roomInstance.getOwnerId() != playerId) {
//            return notOwnedTooltip;
//        }
    }

    private String getRoomAmount(IRoomInformation room, int roomId) {
        return Integer.toString(roomsInformation.getRoomCount(room.getOwnerId(), (short) roomId));
    }

}
