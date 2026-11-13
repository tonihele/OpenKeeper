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

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TortureChamberControllerTest {

    private static final short TORTURE_CHAMBER = 12;
    private static final short TORTURE_WHEEL = 8;

    private final EntityData entityData = new DefaultEntityData();

    @Test
    void tortureWheelsSpinAndTheOtherFurnitureDoesNot() throws ReflectiveOperationException {

        // The DK II objects of the room, a 5x5 room gets furniture on its 9 inner tiles
        Room room = RoomFixtures.room(TORTURE_CHAMBER, 0);
        Field objects = Room.class.getDeclaredField("objects");
        objects.setAccessible(true);
        objects.set(room, List.of((short) 8, (short) 8, (short) 61, (short) 8, (short) 62, (short) 63, (short) 0, (short) 0));
        RoomInstance roomInstance = new RoomInstance(room);
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                roomInstance.addCoordinate(new Point(x, y));
            }
        }

        TortureChamberController chamber = new TortureChamberController(entityData.createEntity(), entityData,
                null, roomInstance, objectsController(), null);
        chamber.construct();

        assertFalse(chamber.getFloorFurniture().isEmpty());
        for (EntityId furniture : chamber.getFloorFurniture()) {
            ObjectViewState viewState = entityData.getComponent(furniture, ObjectViewState.class);
            GameObject.State expected = viewState.objectId == TORTURE_WHEEL ? GameObject.State.TORTURE_WHEEL_SPIN : GameObject.State.NONE;
            assertEquals(expected, viewState.state, "Object " + viewState.objectId);
        }
    }

    private IObjectsController objectsController() {
        return (IObjectsController) Proxy.newProxyInstance(IObjectsController.class.getClassLoader(),
                new Class<?>[]{IObjectsController.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("loadObject")) {
                        return null;
                    }
                    EntityId object = entityData.createEntity();
                    entityData.setComponents(object,
                            new ObjectViewState((short) args[0], GameObject.State.NONE, ObjectViewState.GameObjectAnimState.MESH_RESOURCE, true),
                            new Position(0, new Vector3f((int) args[2], 0, (int) args[3])));
                    return object;
                });
    }
}
