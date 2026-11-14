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
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IRoomInformation;
import toniarts.openkeeper.game.map.IRoomsInformation;

/**
 * Tests the room tooltip parameters, notably the Guard Room's patrol routes
 */
class RoomTextParserTest {

    private static final String GUARD_ROOM_TEXT = "Guard Rooms owned: %51.  Patrol Routes: %52.";
    private static final short OWNER = 2;

    @Test
    void patrolRoutesAreFilledWithTheOwnersCount() {
        RoomTextParser parser = new RoomTextParser(roomsInformation());
        parser.setPatrolRouteCounter(owner -> owner == OWNER ? 3 : 99);

        String result = parser.parseText(GUARD_ROOM_TEXT, new EntityId(1), new Keeper(false, OWNER));

        assertEquals("Guard Rooms owned: 2.  Patrol Routes: 3.", result);
        assertFalse(result.contains("not implemented"));
    }

    @Test
    void patrolRoutesDefaultToZeroWithoutACounter() {
        RoomTextParser parser = new RoomTextParser(roomsInformation());

        String result = parser.parseText(GUARD_ROOM_TEXT, new EntityId(1), new Keeper(false, OWNER));

        assertEquals("Guard Rooms owned: 2.  Patrol Routes: 0.", result);
    }

    @SuppressWarnings("unchecked")
    private static IRoomsInformation<IRoomInformation> roomsInformation() {
        IRoomInformation room = (IRoomInformation) Proxy.newProxyInstance(RoomTextParserTest.class.getClassLoader(),
                new Class<?>[]{IRoomInformation.class}, (proxy, method, args) -> {
                    if ("getOwnerId".equals(method.getName())) {
                        return OWNER;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        return (IRoomsInformation<IRoomInformation>) Proxy.newProxyInstance(RoomTextParserTest.class.getClassLoader(),
                new Class<?>[]{IRoomsInformation.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getRoomInformation":
                            return room;
                        case "getRoomCount":
                            // Two Guard Rooms (type 9) for the owner
                            return (short) args[0] == OWNER && (short) args[1] == 9 ? 2 : 0;
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

}
