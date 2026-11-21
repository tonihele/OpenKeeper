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
import toniarts.openkeeper.game.component.Decay;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.ObjectsController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.GameObjectFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Every chicken standing on a Hatchery belongs to it (DKII.EXE 0x4DEB70)
 */
class HatcheryControllerTest {

    private static final short KEEPER = 3;
    private static final short OTHER_KEEPER = 4;

    private final EntityData entityData = new DefaultEntityData();
    private final RoomInstance roomInstance = new RoomInstance(RoomFixtures.room((short) 7,
            List.of((short) 0, (short) 0, (short) 0, (short) 0, (short) 0, (short) 0)));

    @Test
    void chickensOnTheTilesOfABuiltHatcheryJoinIt() {
        EntityId onTheHatchery = looseChicken(ObjectsController.OBJECT_CHICKEN_ID, 1, 1);
        EntityId besideIt = looseChicken(ObjectsController.OBJECT_CHICKEN_ID, 3, 1);
        EntityId egg = looseChicken(ObjectsController.OBJECT_EGG_ID, 1, 1);

        HatcheryController hatchery = buildHatchery(2);

        // The other keeper's chicken is the Hatchery's now, and doesn't die out of a room anymore
        assertEquals(hatchery.getEntityId(), entityData.getComponent(onTheHatchery, Stored.class).room);
        assertEquals(KEEPER, entityData.getComponent(onTheHatchery, Owner.class).ownerId);
        assertNull(entityData.getComponent(onTheHatchery, Decay.class));
        assertEquals(1, hatchery.getObjectControl(ObjectType.FOOD).getCurrentCapacity());

        assertNull(entityData.getComponent(besideIt, Stored.class));
        assertEquals(OTHER_KEEPER, entityData.getComponent(besideIt, Owner.class).ownerId);
        assertNull(entityData.getComponent(egg, Stored.class));
    }

    @Test
    void chickensOnTheNewTilesOfAnExtendedHatcheryJoinItOnce() {
        EntityId chicken = looseChicken(ObjectsController.OBJECT_CHICKEN_ID, 3, 1);
        HatcheryController hatchery = buildHatchery(2);
        assertNull(entityData.getComponent(chicken, Stored.class));

        roomInstance.addCoordinate(new Point(3, 1));
        hatchery.construct();
        assertNotNull(entityData.getComponent(chicken, Stored.class));

        // Changing the room again doesn't take the same chicken in twice
        hatchery.construct();
        assertEquals(1, hatchery.getObjectControl(ObjectType.FOOD).getCurrentCapacity());
    }

    @Test
    void chickensOnTheTilesOfACapturedHatcheryJoinIt() {
        HatcheryController hatchery = buildHatchery(2);
        EntityId chicken = looseChicken(ObjectsController.OBJECT_CHICKEN_ID, 2, 2);

        hatchery.captured(OTHER_KEEPER);

        assertEquals(hatchery.getEntityId(), entityData.getComponent(chicken, Stored.class).room);
        assertEquals(OTHER_KEEPER, entityData.getComponent(chicken, Owner.class).ownerId);
    }

    private EntityId looseChicken(short objectId, int x, int y) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponents(entityId, new ObjectComponent(objectId, ObjectType.FOOD), new Owner(OTHER_KEEPER, OTHER_KEEPER),
                new Decay(0, 60), new Position(0, WorldUtils.pointToVector3f(x, y)));
        return entityId;
    }

    private HatcheryController buildHatchery(int size) {
        IObjectsController objectsController = (IObjectsController) Proxy.newProxyInstance(
                IObjectsController.class.getClassLoader(), new Class<?>[]{IObjectsController.class},
                (proxy, method, args) -> method.getName().equals("loadObject") ? entityData.createEntity() : null);
        GameObject chicken = GameObjectFixtures.gameObject("Chicken", GameObject.ObjectFlag.DIE_OVER_TIME_IF_NOT_IN_ROOM);
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> method.getName().equals("getObject") ? chicken : null);
        roomInstance.setOwnerId(KEEPER);
        for (int x = 1; x <= size; x++) {
            for (int y = 1; y <= size; y++) {
                roomInstance.addCoordinate(new Point(x, y));
            }
        }

        HatcheryController room = new HatcheryController(entityData.createEntity(), entityData, kwdFile, roomInstance, objectsController, () -> 0.0);
        room.construct();
        return room;
    }
}
