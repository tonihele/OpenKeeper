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
import java.util.Map;
import java.util.function.ToIntFunction;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IRoomInformation;
import toniarts.openkeeper.game.map.IRoomsInformation;
import toniarts.openkeeper.utils.TextParameter;
import toniarts.openkeeper.utils.TextUtils;
import toniarts.openkeeper.utils.Utils;

/**
 * Parses room tooltip text using room and owning player data.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class RoomTextParser {

    private static final Map<TextParameter, Integer> ROOM_IDS = Map.ofEntries(
            Map.entry(TextParameter.PORTAL_COUNT, 3),
            Map.entry(TextParameter.LAIR_COUNT, 2),
            Map.entry(TextParameter.HATCHERY_COUNT, 4),
            Map.entry(TextParameter.TREASURY_COUNT, 1),
            Map.entry(TextParameter.LIBRARY_COUNT, 6),
            Map.entry(TextParameter.TRAINING_ROOM_COUNT, 7),
            Map.entry(TextParameter.WORKSHOP_COUNT, 10),
            Map.entry(TextParameter.GUARD_ROOM_COUNT, 9),
            Map.entry(TextParameter.COMBAT_PIT_COUNT, 16),
            Map.entry(TextParameter.TORTURE_CHAMBER_COUNT, 12),
            Map.entry(TextParameter.PRISON_COUNT, 11),
            Map.entry(TextParameter.PRISON_COUNT_ALTERNATIVE, 11),
            Map.entry(TextParameter.GRAVEYARD_COUNT, 14),
            Map.entry(TextParameter.TEMPLE_COUNT, 13),
            Map.entry(TextParameter.CASINO_COUNT, 15));

    private ToIntFunction<Short> patrolRouteCounter = owner -> 0;
    private final IRoomsInformation roomsInformation;

    public RoomTextParser(IRoomsInformation roomsInformation) {
        this.roomsInformation = roomsInformation;
    }

    public String parseText(String text, EntityId room, Keeper roomOwner) {
        return TextUtils.parseText(text, (parameter) -> {
            return getReplacement(parameter, roomsInformation.getRoomInformation(room), roomOwner);
        });
    }

    protected String getReplacement(TextParameter parameter, IRoomInformation room, Keeper roomOwner) {
        Integer roomId = ROOM_IDS.get(parameter);
        if (roomId != null) {
            return getRoomAmount(room, roomId);
        }

        switch (parameter) {
            case HEALTH_PERCENTAGE:
                return Integer.toString(room.getHealthPercent()); // Health
            case ROOM_USED_CAPACITY:
                return Integer.toString(room.getUsedCapacity(getCapacityType(room))); // Used capacity
            case ROOM_MAX_CAPACITY:
                return Integer.toString(room.getMaxCapacity(getCapacityType(room))); // Max capacity
            case TREASURY_GOLD:
                return Integer.toString(room.getUsedCapacity(ObjectType.GOLD));
            case TREASURY_MAX_GOLD:
                return Integer.toString(room.getMaxCapacity(ObjectType.GOLD));
            case LIBRARY_RESEARCHER_COUNT:
                return Integer.toString(room.getUsedCapacity(ObjectType.RESEARCHER));
            case LIBRARY_MAX_RESEARCHER_COUNT:
                return Integer.toString(room.getMaxCapacity(ObjectType.RESEARCHER));
            case GUARD_ROOM_GUARD_COUNT:
            case PRISONER_COUNT:
            case WORKSHOP_USED_CAPACITY:
                // DK II fills the guards and the captives from the room's capacity. The
                // Workshop's crates aren't counted yet, so they show its capacity too
                return Integer.toString(room.getUsedCapacity());
            case GUARD_ROOM_MAX_GUARD_COUNT:
            case MAX_PRISONER_COUNT:
            case MAX_SKELETON_COUNT:
            case GRAVEYARD_MAX_VAMPIRE_COUNT:
            case WORKSHOP_MAX_CAPACITY:
                return Integer.toString(room.getMaxCapacity());
            case PRISON_STATUS:
                // The Prison's door isn't opened in OpenKeeper, DK II shows "Closed." for a shut one
                return Utils.getMainTextResourceBundle().getString("2455");
            case CASINO_PAYOUT:
                // The pay-out lever can't be pulled yet, so the Casino is a generous one
                return Utils.getMainTextResourceBundle().getString("2508");
            case PATROL_ROUTE_COUNT:
                return Integer.toString(patrolRouteCounter.applyAsInt(room.getOwnerId()));
            case DUNGEON_HEART_MANA:
                return Integer.toString(roomOwner.getMana());
            case DUNGEON_HEART_MAX_MANA:
                return Integer.toString(roomOwner.getMaxMana());
            default:
                return TextUtils.getUnsupportedParameterMessage(parameter, getClass());
        }
    }

    /**
     * The storage the room's capacity line tells about. In the Library DK II
     * shows the shelf space there, the researchers have their own line
     *
     * @param room the room
     * @return the storage type of the capacity line
     */
    private static ObjectType getCapacityType(IRoomInformation room) {
        if (room.getDefaultStorageType() == ObjectType.RESEARCHER) {
            return ObjectType.SPELL_BOOK;
        }
        return room.getDefaultStorageType();
    }

    private String getRoomAmount(IRoomInformation room, int roomId) {
        return Integer.toString(roomsInformation.getRoomCount(room.getOwnerId(), (short) roomId));
    }

    /**
     * Sets how the patrol routes of a player are counted. Without this, the
     * count is always zero.
     *
     * @param patrolRouteCounter counts the patrol routes (Guard Posts) of a
     * player, by the player ID
     */
    public void setPatrolRouteCounter(ToIntFunction<Short> patrolRouteCounter) {
        this.patrolRouteCounter = patrolRouteCounter;
    }

}
