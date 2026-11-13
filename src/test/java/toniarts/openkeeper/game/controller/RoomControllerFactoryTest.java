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

import com.simsilica.es.EntityData;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.BridgeController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.BridgeRoomFixtures;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class RoomControllerFactoryTest {

    private final EntityData entityData = new DefaultEntityData();

    @Test
    void stoneBridgeGetsBridgeController() {
        assertInstanceOf(BridgeController.class, construct(BridgeRoomFixtures.stoneBridge()));
    }

    @Test
    void woodenBridgeGetsBridgeController() {
        assertInstanceOf(BridgeController.class, construct(BridgeRoomFixtures.woodenBridge()));
    }

    @Test
    void heroStoneBridgeGetsBridgeController() {
        assertInstanceOf(BridgeController.class, construct(BridgeRoomFixtures.heroStoneBridge()));
    }

    @Test
    void bridgeHasNoFurniturePillarsOrStorage() {
        IRoomController bridge = construct(BridgeRoomFixtures.stoneBridge());

        bridge.construct();

        assertEquals(0, bridge.getFloorFurnitureCount());
        assertEquals(0, bridge.getWallFurnitureCount());
        assertFalse(bridge.canStoreGold());
        assertEquals(0, bridge.getMaxCapacity(ObjectType.GOLD));
    }

    private IRoomController construct(Room room) {
        RoomInstance roomInstance = new RoomInstance(room);
        for (int x = 4; x <= 6; x++) {
            for (int y = 4; y <= 6; y++) {
                roomInstance.addCoordinate(new Point(x, y));
            }
        }
        return RoomControllerFactory.constructRoom(entityData, null, roomInstance, noObjects(),
                Collections.emptyMap(), null);
    }

    private static IObjectsController noObjects() {
        return (IObjectsController) Proxy.newProxyInstance(IObjectsController.class.getClassLoader(),
                new Class<?>[]{IObjectsController.class}, (proxy, method, args) -> {
                    throw new AssertionError("A bridge has no objects, but " + method.getName() + " was called");
                });
    }
}
