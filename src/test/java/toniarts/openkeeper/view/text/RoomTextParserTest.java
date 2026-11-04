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
package toniarts.openkeeper.view.text;

import com.simsilica.es.EntityId;
import java.lang.reflect.Proxy;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IRoomInformation;
import toniarts.openkeeper.game.map.IRoomsInformation;

/**
 * Tests the room tooltip parameters, notably the Guard Room's patrol routes
 * and the fields of DK II's room status texts
 */
class RoomTextParserTest {

    private static final String GUARD_ROOM_TEXT = "Guard Rooms owned: %51.  Patrol Routes: %52.";
    private static final short OWNER = 2;

    @Test
    void patrolRoutesAreFilledWithTheOwnersCount() {
        RoomTextParser parser = new RoomTextParser(roomsInformation(null, Map.of()));
        parser.setPatrolRouteCounter(owner -> owner == OWNER ? 3 : 99);

        String result = parser.parseText(GUARD_ROOM_TEXT, new EntityId(1), new Keeper(false, OWNER));

        assertEquals("Guard Rooms owned: 2.  Patrol Routes: 3.", result);
        assertFalse(result.contains("not implemented"));
    }

    @Test
    void patrolRoutesDefaultToZeroWithoutACounter() {
        RoomTextParser parser = new RoomTextParser(roomsInformation(null, Map.of()));

        String result = parser.parseText(GUARD_ROOM_TEXT, new EntityId(1), new Keeper(false, OWNER));

        assertEquals("Guard Rooms owned: 2.  Patrol Routes: 0.", result);
    }

    @Test
    void treasuryShowsTheGoldStoredAndTheMaximum() {
        RoomTextParser parser = new RoomTextParser(roomsInformation(ObjectType.GOLD,
                Map.of(ObjectType.GOLD, new int[]{1200, 3000})));

        String result = parser.parseText("Gold storage used: %75 / %76 maximum.", new EntityId(1), new Keeper(false, OWNER));

        assertEquals("Gold storage used: 1200 / 3000 maximum.", result);
    }

    @Test
    void libraryShowsTheResearchersAndTheShelfSpace() {
        RoomTextParser parser = new RoomTextParser(roomsInformation(ObjectType.RESEARCHER,
                Map.of(ObjectType.RESEARCHER, new int[]{2, 5}, ObjectType.SPELL_BOOK, new int[]{3, 8})));

        String result = parser.parseText("Researchers: %77 / %78.  Shelf space filled: %38 / %39.", new EntityId(1),
                new Keeper(false, OWNER));

        assertEquals("Researchers: 2 / 5.  Shelf space filled: 3 / 8.", result);
    }

    @Test
    void prisonShowsTheCaptivesAndTheMaximumSkeletons() {
        RoomTextParser parser = new RoomTextParser(roomsInformation(ObjectType.PRISONER,
                Map.of(ObjectType.PRISONER, new int[]{1, 5})));

        String result = parser.parseText("Captives held: %83 / %84.  Maximum Skeletons: %56.  Prisons owned: %85.",
                new EntityId(1), new Keeper(false, OWNER));

        assertEquals("Captives held: 1 / 5.  Maximum Skeletons: 5.  Prisons owned: 1.", result);
    }

    @Test
    void guardRoomShowsTheGuardsFromItsCapacity() {
        RoomTextParser parser = new RoomTextParser(roomsInformation(null, Map.of()));

        String result = parser.parseText("Guards assigned: %81 / %82.", new EntityId(1), new Keeper(false, OWNER));

        assertEquals("Guards assigned: 0 / 0.", result);
    }

    /**
     * Rooms of the owner: two Guard Rooms and one Prison, the room asked for
     * has the given storages
     *
     * @param defaultStorageType the room's default storage
     * @param capacities used and maximum capacity by storage type
     */
    @SuppressWarnings("unchecked")
    private static IRoomsInformation<IRoomInformation> roomsInformation(ObjectType defaultStorageType,
            Map<ObjectType, int[]> capacities) {
        IRoomInformation room = (IRoomInformation) Proxy.newProxyInstance(RoomTextParserTest.class.getClassLoader(),
                new Class<?>[]{IRoomInformation.class}, (proxy, method, args) -> {
                    ObjectType type = args == null ? defaultStorageType : (ObjectType) args[0];
                    int[] capacity = type == null ? new int[2] : capacities.getOrDefault(type, new int[2]);
                    switch (method.getName()) {
                        case "getOwnerId":
                            return OWNER;
                        case "getDefaultStorageType":
                            return defaultStorageType;
                        case "getUsedCapacity":
                            return capacity[0];
                        case "getMaxCapacity":
                            return capacity[1];
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
        return (IRoomsInformation<IRoomInformation>) Proxy.newProxyInstance(RoomTextParserTest.class.getClassLoader(),
                new Class<?>[]{IRoomsInformation.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getRoomInformation":
                            return room;
                        case "getRoomCount":
                            // Two Guard Rooms (type 9) and a Prison (type 11) for the owner
                            if ((short) args[0] != OWNER) {
                                return 0;
                            }
                            return (short) args[1] == 9 ? 2 : (short) args[1] == 11 ? 1 : 0;
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

}
