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
package toniarts.openkeeper.game.controller.player;

import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerRoomControlTest {

    private final Room lair = RoomFixtures.room((short) 2, 100, Room.RoomFlag.PLACEABLE_ON_LAND, Room.RoomFlag.BUILDABLE);
    private final Room bridge = RoomFixtures.room((short) 8, 100, Room.RoomFlag.PLACEABLE_ON_WATER, Room.RoomFlag.PLACEABLE_ON_LAVA, Room.RoomFlag.BUILDABLE);
    private final PlayerRoomControl roomControl = new PlayerRoomControl(new Keeper(false, (short) 3), List.of(lair, bridge));

    @Test
    void countsCapturedRooms() {
        roomControl.onCaptured(roomController(lair));
        roomControl.onCaptured(roomController(lair));

        assertEquals(2, roomControl.getRoomsCaptured());
    }

    @Test
    void builtRoomsAreNotCaptured() {
        roomControl.onBuild(roomController(lair));

        assertEquals(0, roomControl.getRoomsCaptured());
    }

    @Test
    void capturedBridgesDontCount() {
        roomControl.onCaptured(roomController(bridge));

        assertEquals(0, roomControl.getRoomsCaptured());
        assertEquals(1, roomControl.getTypeCount(bridge));
    }

    private static IRoomController roomController(Room room) {
        return (IRoomController) Proxy.newProxyInstance(IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getRoom" -> room;
                    case "isDungeonHeart" -> false;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                });
    }
}
