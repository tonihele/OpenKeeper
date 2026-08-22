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
import toniarts.openkeeper.utils.TextParameter;
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
        return TextUtils.parseText(text, (parameter) -> {
            return getReplacement(parameter, roomsInformation.getRoomInformation(room));
        });
    }

    protected String getReplacement(TextParameter parameter, IRoomInformation room) {
        switch (parameter) {
            case HEALTH_PERCENTAGE:
                return Integer.toString(room.getHealthPercent()); // Health
            case ROOM_USED_CAPACITY:
                return Integer.toString(room.getUsedCapacity()); // Used capacity
            case ROOM_MAX_CAPACITY:
                return Integer.toString(room.getMaxCapacity()); // Max capacity
            case PORTAL_COUNT:
                return getRoomAmount(room, 3); // Portal
            case LAIR_COUNT:
                return getRoomAmount(room, 2); // Lair
            case HATCHERY_COUNT:
                return getRoomAmount(room, 4); // Hatchery
            case TREASURY_COUNT:
                return getRoomAmount(room, 1); // Treasury
            case LIBRARY_COUNT:
                return getRoomAmount(room, 6); // Library
            case TRAINING_ROOM_COUNT:
                return getRoomAmount(room, 7); // Training Room
            case WORKSHOP_COUNT:
                return getRoomAmount(room, 10); // Workshop
            case GUARD_ROOM_COUNT:
                return getRoomAmount(room, 9); // Guard Room
            case COMBAT_PIT_COUNT:
                return getRoomAmount(room, 16); // Combat Pit
            case TORTURE_CHAMBER_COUNT:
                return getRoomAmount(room, 12); // Torture
            case PRISON_COUNT:
                return getRoomAmount(room, 11); // Prison
            case GRAVEYARD_COUNT:
                return getRoomAmount(room, 14); // Graveyard
            case TEMPLE_COUNT:
                return getRoomAmount(room, 13); // Temple
            case CASINO_COUNT:
                return getRoomAmount(room, 15); // Casino
            default:
                return TextUtils.getUnsupportedParameterMessage(parameter, getClass());
        }
    }

    private String getRoomAmount(IRoomInformation room, int roomId) {
        return Integer.toString(roomsInformation.getRoomCount(room.getOwnerId(), (short) roomId));
    }

}
