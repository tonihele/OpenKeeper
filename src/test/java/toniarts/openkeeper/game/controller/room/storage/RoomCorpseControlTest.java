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
package toniarts.openkeeper.game.controller.room.storage;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomCorpseControlTest {

    private static final short ROOM_OWNER = 3;
    private static final short CORPSE_OWNER = 4;

    @Test
    void storedCorpseBelongsToTheGraveyardOwnerAndTakesOneTile() {
        EntityData entityData = new DefaultEntityData();
        EntityId roomEntity = entityData.createEntity();
        RoomCorpseControl control = createControl(entityData, roomEntity, new Point(1, 1), new Point(2, 1));
        EntityId corpse = createCorpse(entityData);

        assertNull(control.addItem(corpse, new Point(1, 1)));

        Stored stored = entityData.getComponent(corpse, Stored.class);
        assertNotNull(stored);
        assertEquals(roomEntity, stored.room);
        assertEquals(ObjectType.CORPSE, stored.objectType);
        assertEquals(ROOM_OWNER, entityData.getComponent(corpse, Owner.class).ownerId);
        assertEquals(1, control.getCurrentCapacity());
        assertEquals(2, control.getMaxCapacity());
        assertFalse(control.getAvailableCoordinates().contains(new Point(1, 1)));
        assertTrue(control.getItems(new Point(1, 1)).contains(corpse));
    }

    @Test
    void onlyOneCorpseFitsOnATile() {
        EntityData entityData = new DefaultEntityData();
        RoomCorpseControl control = createControl(entityData, entityData.createEntity(), new Point(1, 1), new Point(2, 1));
        EntityId first = createCorpse(entityData);
        EntityId second = createCorpse(entityData);
        control.addItem(first, new Point(1, 1));

        assertSame(second, control.addItem(second, new Point(1, 1)));
        assertNull(entityData.getComponent(second, Stored.class));
        assertEquals(1, control.getCurrentCapacity());
    }

    @Test
    void removingACorpseFreesTheTile() {
        EntityData entityData = new DefaultEntityData();
        RoomCorpseControl control = createControl(entityData, entityData.createEntity(), new Point(1, 1));
        EntityId corpse = createCorpse(entityData);
        control.addItem(corpse, new Point(1, 1));

        control.removeItem(corpse);
        control.removeItem(corpse);

        assertNull(entityData.getComponent(corpse, Stored.class));
        assertEquals(0, control.getCurrentCapacity());
        assertTrue(control.getAvailableCoordinates().contains(new Point(1, 1)));
    }

    @Test
    void destroyingTheGraveyardReleasesTheCorpses() {
        EntityData entityData = new DefaultEntityData();
        RoomCorpseControl control = createControl(entityData, entityData.createEntity(), new Point(1, 1));
        EntityId corpse = createCorpse(entityData);
        control.addItem(corpse, new Point(1, 1));

        control.destroy();

        assertNull(entityData.getComponent(corpse, Stored.class));
        assertNotNull(entityData.getComponent(corpse, Owner.class));
    }

    private static EntityId createCorpse(EntityData entityData) {
        EntityId corpse = entityData.createEntity();
        entityData.setComponent(corpse, new Owner(CORPSE_OWNER, CORPSE_OWNER));
        return corpse;
    }

    private static RoomCorpseControl createControl(EntityData entityData, EntityId roomEntity, Point... coordinates) {
        RoomInstance roomInstance = new RoomInstance(new Room());
        for (Point p : coordinates) {
            roomInstance.addCoordinate(p);
        }
        IRoomController room = (IRoomController) Proxy.newProxyInstance(
                IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getEntityId" -> roomEntity;
                    case "getOwnerId" -> ROOM_OWNER;
                    case "getRoomInstance" -> roomInstance;
                    case "isTileAccessible" -> true;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return new RoomCorpseControl(null, room, entityData, () -> 0) {
            @Override
            protected int getNumberOfAccessibleTiles() {
                return roomInstance.getCoordinates().size();
            }
        };
    }
}
