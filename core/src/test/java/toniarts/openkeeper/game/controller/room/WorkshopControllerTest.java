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
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static toniarts.openkeeper.tools.convert.map.RoomFixtures.room;

class WorkshopControllerTest {

    @Test
    void roomWithOneWorkBenchTakesTwoWorkers() {
        WorkshopController room = buildWorkshop(3);

        assertEquals(1, room.getFurnitureCount());
        assertEquals(2, room.getMaxCapacity(ObjectType.MANUFACTURER));
    }

    @Test
    void roomWithoutFurnitureTakesNoWorkers() {
        WorkshopController room = buildWorkshop(2);

        assertEquals(0, room.getFurnitureCount());
        assertEquals(0, room.getMaxCapacity(ObjectType.MANUFACTURER));
    }

    private static WorkshopController buildWorkshop(int size) {
        EntityData entityData = new DefaultEntityData();
        IObjectsController objectsController = (IObjectsController) Proxy.newProxyInstance(
                IObjectsController.class.getClassLoader(),
                new Class<?>[]{IObjectsController.class},
                (proxy, method, args) -> method.getName().equals("loadObject") ? furniture(entityData, (int) args[2], (int) args[3]) : null);

        // Workshop objects in DK II: Steel Bucket, Anvil, Work Bench, Anvil, Steel Bucket
        RoomInstance roomInstance = new RoomInstance(room((short) 10, List.of((short) 16, (short) 15, (short) 46, (short) 15, (short) 16, (short) 0)));
        for (int x = 1; x <= size; x++) {
            for (int y = 1; y <= size; y++) {
                roomInstance.addCoordinate(new Point(x, y));
            }
        }

        WorkshopController room = new WorkshopController(entityData.createEntity(), entityData, null, roomInstance, objectsController, null);
        room.construct();
        return room;
    }

    private static EntityId furniture(EntityData entityData, int x, int y) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponent(entityId, new Position(0, WorldUtils.pointToVector3f(x, y)));
        return entityId;
    }
}
