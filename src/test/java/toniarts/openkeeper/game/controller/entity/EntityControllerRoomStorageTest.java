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
package toniarts.openkeeper.game.controller.entity;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Removing an entity that is stored in a room, like a chicken being eaten
 */
class EntityControllerRoomStorageTest {

    private final EntityData entityData = new DefaultEntityData();
    private final List<EntityId> removedItems = new ArrayList<>();
    private final EntityId hatchery = entityData.createEntity();

    @Test
    void removesStoredEntityFromItsRoomEvenWhenStandingOnAnotherRoom() {
        EntityId chicken = entityData.createEntity();
        entityData.setComponent(chicken, new Stored(hatchery, ObjectType.FOOD));
        entityData.setComponent(chicken, new Position(0, new Vector3f(10.5f, 0, 10.5f)));

        // Any lookup by the coordinates would find a room that has no food control, if any room at all
        new EntityController(chicken, entityData, null, mapController()).remove();

        assertEquals(List.of(chicken), removedItems);
        assertNull(entityData.getComponent(chicken, Stored.class));
    }

    @Test
    void removesStoredEntityWhenRoomIsGone() {
        EntityId chicken = entityData.createEntity();
        entityData.setComponent(chicken, new Stored(entityData.createEntity(), ObjectType.FOOD));

        new EntityController(chicken, entityData, null, mapController()).remove();

        assertEquals(List.of(), removedItems);
    }

    private IMapController mapController() {
        IRoomObjectControl<?> objectControl = (IRoomObjectControl<?>) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IRoomObjectControl.class}, (proxy, method, args) -> {
                    if (method.getName().equals("removeItem")) {
                        removedItems.add((EntityId) args[0]);
                    }
                    return null;
                });
        IRoomController room = (IRoomController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IRoomController.class}, (proxy, method, args)
                -> method.getName().equals("getObjectControl") ? objectControl : null);
        return (IMapController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args)
                -> method.getName().equals("getRoomController") && hatchery.equals(args[0]) ? room : null);
    }

}
