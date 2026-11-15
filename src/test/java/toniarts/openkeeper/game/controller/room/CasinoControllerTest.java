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
import com.simsilica.es.filter.FieldFilter;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The roulette tables of the Casino get their wheels as in the original
 */
class CasinoControllerTest {

    private final EntityData entityData = new DefaultEntityData();

    @Test
    void everyRouletteTableGetsTwoWheelsThatGoWithTheRoom() {
        RoomInstance roomInstance = new RoomInstance(RoomFixtures.room((short) 15,
                List.of((short) 87, (short) 59, (short) 67, (short) 68, (short) 0, (short) 0)));
        for (int x = 5; x <= 7; x++) {
            for (int y = 5; y <= 7; y++) {
                roomInstance.addCoordinate(new Point(x, y));
            }
        }
        CasinoController casino = new CasinoController(entityData.createEntity(), entityData, null, roomInstance,
                objectsController(), null);

        casino.construct();
        assertEquals(Set.of(new Vector3f(6, 1, 6 + 2000f / 4096), new Vector3f(6, 1, 6 - 2000f / 4096)), wheels());

        // Built again, as when the room grows
        casino.construct();
        assertEquals(2, wheels().size());

        casino.remove();
        assertTrue(wheels().isEmpty());
    }

    private Set<Vector3f> wheels() {
        Set<Vector3f> wheels = new HashSet<>();
        for (EntityId wheel : entityData.findEntities(new FieldFilter<>(ObjectComponent.class, "objectId",
                CasinoController.ROULETTE_WHEEL_OBJECT_ID), ObjectComponent.class, Position.class)) {
            wheels.add(entityData.getComponent(wheel, Position.class).position);
        }
        return wheels;
    }

    private IObjectsController objectsController() {
        return (IObjectsController) Proxy.newProxyInstance(IObjectsController.class.getClassLoader(), new Class<?>[]{IObjectsController.class}, (proxy, method, args) -> {
            if (!"loadObject".equals(method.getName())) {
                throw new UnsupportedOperationException(method.getName());
            }
            Vector3f position = args[2] instanceof Vector3f vector
                    ? new Vector3f(vector.x, 1, vector.z)
                    : new Vector3f((int) args[2], 1, (int) args[3]);
            EntityId object = entityData.createEntity();
            entityData.setComponents(object, new ObjectComponent((short) args[0], null), new Position(0, position));
            return object;
        });
    }

}
