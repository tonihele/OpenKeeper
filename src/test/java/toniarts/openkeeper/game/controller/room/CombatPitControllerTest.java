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
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static toniarts.openkeeper.tools.convert.map.CombatPitFixtures.pitRoom;

/**
 * The Combat Pit is walled in, its door is the way in and out
 */
class CombatPitControllerTest {

    @Test
    void doorIsTheOnlyWayInAndOut() {
        CombatPitController pit = buildPit(5);

        // The inside is 2..4, the door is one of the edge tiles north of it
        int doors = 0;
        for (int x = 2; x <= 4; x++) {
            if (pit.isTileAccessible(x, 2, x, 1) && pit.isTileAccessible(x, 1, x, 2)) {
                doors++;
            }
        }
        assertEquals(1, doors);
        assertFalse(pit.isTileAccessible(1, 3, 2, 3));
        assertFalse(pit.isTileAccessible(3, 4, 3, 5));
    }

    private static CombatPitController buildPit(int size) {
        EntityData entityData = new DefaultEntityData();
        IObjectsController objectsController = (IObjectsController) Proxy.newProxyInstance(
                IObjectsController.class.getClassLoader(),
                new Class<?>[]{IObjectsController.class},
                (proxy, method, args) -> method.getName().equals("loadObject") ? entityData.createEntity() : null);
        RoomInstance roomInstance = new RoomInstance(pitRoom());
        for (int x = 1; x <= size; x++) {
            for (int y = 1; y <= size; y++) {
                roomInstance.addCoordinate(new Point(x, y));
            }
        }
        CombatPitController pit = new CombatPitController(entityData.createEntity(), entityData, null, roomInstance, objectsController);
        pit.construct();
        return pit;
    }
}
