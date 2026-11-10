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
import toniarts.openkeeper.tools.convert.map.OwnedRoomFixtures;
import toniarts.openkeeper.tools.convert.map.Room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A room the level makes available only to its owners (DK II's availability
 * value 4) can be built while the keeper has a room of the type, e.g. one he
 * captured
 */
class RoomAvailableWhenOwnedTest {

    private final Room bridge = OwnedRoomFixtures.buildableRoom((short) 17, (short) 12);
    private final Keeper keeper = new Keeper(false, (short) 3);
    private final PlayerRoomControl roomControl = new PlayerRoomControl(keeper, List.of(bridge));

    @Test
    void notAvailableWithoutARoomOfTheType() {
        roomControl.setTypeAvailableWhenOwned(bridge);

        assertFalse(roomControl.getTypesAvailable().contains(bridge));
        assertTrue(keeper.getAvailableRooms().isEmpty());
    }

    @Test
    void availableWhileTheKeeperHasOne() {
        roomControl.setTypeAvailableWhenOwned(bridge);
        IRoomController captured = room(bridge);

        roomControl.onCaptured(captured);

        assertTrue(roomControl.getTypesAvailable().contains(bridge));
        assertEquals(1, keeper.getAvailableRooms().size());
        assertTrue(keeper.getAvailableRooms().get(0).isDiscovered());

        roomControl.onCapturedByEnemy(captured);

        assertFalse(roomControl.getTypesAvailable().contains(bridge));
        assertTrue(keeper.getAvailableRooms().isEmpty());
    }

    @Test
    void roomsOwnedAtTheStartMakeItAvailable() {
        roomControl.setTypeAvailableWhenOwned(bridge);

        roomControl.init(List.of(room(bridge)));

        assertTrue(roomControl.getTypesAvailable().contains(bridge));
    }

    @Test
    void availabilitySetLaterIsKept() {
        roomControl.setTypeAvailableWhenOwned(bridge);
        IRoomController built = room(bridge);
        roomControl.onBuild(built);

        // E.g. a trigger makes the room available to everyone
        roomControl.setTypeAvailable(bridge, true);
        roomControl.onSold(built);

        assertTrue(roomControl.getTypesAvailable().contains(bridge));
    }

    private static IRoomController room(Room type) {
        return (IRoomController) Proxy.newProxyInstance(IRoomController.class.getClassLoader(),
                new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getRoom" -> type;
                    case "isDungeonHeart" -> false;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

}
