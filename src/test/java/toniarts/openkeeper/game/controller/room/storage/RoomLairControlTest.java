/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.game.controller.room.storage;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.Collection;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class RoomLairControlTest {

    @Test
    void detachedBedCanMoveToResultingRoomWithoutChangingEntity() {
        EntityData entityData = new DefaultEntityData();
        Point location = new Point(3, 4);
        RoomLairControl source = createControl(entityData, entityData.createEntity(), location);
        EntityId destinationRoom = entityData.createEntity();
        RoomLairControl destination = createControl(entityData, destinationRoom, location);
        EntityId bed = entityData.createEntity();
        entityData.setComponent(bed, new Position(0, new Vector3f(location.x, 1, location.y)));
        source.addExistingItem(bed, location);

        Collection<EntityId> detached = source.detachAllItems();
        destination.addExistingItem(detached.iterator().next(), location);

        assertEquals(1, detached.size());
        assertSame(bed, detached.iterator().next());
        assertEquals(destinationRoom, entityData.getComponent(bed, Stored.class).room);
        assertEquals(1, destination.getCurrentCapacity());
        assertEquals(0, source.getCurrentCapacity());

        source.destroy();
        destination.destroy();

        assertNull(entityData.getComponent(bed, Position.class));
    }

    private static RoomLairControl createControl(EntityData entityData, EntityId roomEntity, Point location) {
        RoomInstance roomInstance = new RoomInstance(new Room());
        roomInstance.addCoordinate(location);
        IRoomController room = (IRoomController) Proxy.newProxyInstance(
                IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getEntityId" -> roomEntity;
                    case "getOwnerId" -> (short) 1;
                    case "getRoomInstance" -> roomInstance;
                    case "isTileAccessible" -> true;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return new RoomLairControl(null, room, entityData, () -> 0, null) {
            @Override
            protected int getNumberOfAccessibleTiles() {
                return roomInstance.getCoordinates().size();
            }
        };
    }
}
