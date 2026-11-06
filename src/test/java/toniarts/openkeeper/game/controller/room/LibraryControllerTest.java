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
package toniarts.openkeeper.game.controller.room;

import com.simsilica.es.EntityData;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static toniarts.openkeeper.tools.convert.map.RoomFixtures.roomWithObjects;

class LibraryControllerTest {

    @Test
    void roomWithOneBookCaseTakesTwoResearchers() {
        LibraryController room = buildLibrary(3);

        assertEquals(1, room.getFurnitureCount());
        assertEquals(2, room.getMaxCapacity(ObjectType.RESEARCHER));
    }

    @Test
    void roomWithoutBookCasesTakesNoResearchers() {
        LibraryController room = buildLibrary(2);

        assertEquals(0, room.getFurnitureCount());
        assertEquals(0, room.getMaxCapacity(ObjectType.RESEARCHER));
    }

    private static LibraryController buildLibrary(int size) {
        EntityData entityData = new DefaultEntityData();
        IObjectsController objectsController = (IObjectsController) Proxy.newProxyInstance(
                IObjectsController.class.getClassLoader(),
                new Class<?>[]{IObjectsController.class},
                (proxy, method, args) -> method.getName().equals("loadObject") ? entityData.createEntity() : null);

        // Library objects in DK II: Book Case on the floor, Library Book
        RoomInstance roomInstance = new RoomInstance(roomWithObjects((short) 6, (short) 64, (short) 0, (short) 0, (short) 58, (short) 0, (short) 0));
        for (int x = 1; x <= size; x++) {
            for (int y = 1; y <= size; y++) {
                roomInstance.addCoordinate(new Point(x, y));
            }
        }

        LibraryController room = new LibraryController(entityData.createEntity(), entityData, null, roomInstance, objectsController, null);
        room.construct();
        return room;
    }
}
