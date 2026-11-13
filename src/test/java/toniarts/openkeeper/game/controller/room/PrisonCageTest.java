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
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Like in DK II, the Prison holds its prisoners inside the cage, the tiles
 * with the room on all eight sides
 */
class PrisonCageTest {

    @Test
    void onlyTheInsideOfThePrisonIsTheCage() {
        EntityData entityData = new DefaultEntityData();
        IObjectsController objectsController = (IObjectsController) Proxy.newProxyInstance(
                IObjectsController.class.getClassLoader(),
                new Class<?>[]{IObjectsController.class},
                (proxy, method, args) -> method.getName().equals("loadObject") ? entityData.createEntity() : null);

        // Prison objects in DK II: Skull, Bone and Lavatory
        RoomInstance roomInstance = new RoomInstance(RoomFixtures.room((short) 11,
                List.of((short) 49, (short) 50, (short) 51, (short) 0, (short) 0, (short) 0)));
        for (int x = 1; x <= 5; x++) {
            for (int y = 1; y <= 5; y++) {
                roomInstance.addCoordinate(new Point(x, y));
            }
        }
        PrisonController prison = new PrisonController(entityData.createEntity(), entityData, null, roomInstance, objectsController, null);
        prison.construct();

        for (Point tile : roomInstance.getCoordinates()) {
            boolean inside = tile.x > 1 && tile.x < 5 && tile.y > 1 && tile.y < 5;
            if (inside) {
                assertTrue(prison.isCageTile(tile));
            } else {
                assertFalse(prison.isCageTile(tile));
            }
        }
    }

}
