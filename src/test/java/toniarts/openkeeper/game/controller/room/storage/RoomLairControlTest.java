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

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomLairControlTest {

    @Test
    void detachedBedCanMoveToResultingRoomWithoutChangingEntity() {
        EntityData entityData = new DefaultEntityData();
        Point location = new Point(3, 4);
        RoomLairControl source = createControl(entityData, entityData.createEntity(), location, null, null);
        EntityId destinationRoom = entityData.createEntity();
        RoomLairControl destination = createControl(entityData, destinationRoom, location, null, null);
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

    @Test
    void soldRoomContainsNothing() {
        EntityData entityData = new DefaultEntityData();
        Point location = new Point(3, 4);
        RoomLairControl control = createControl(entityData, entityData.createEntity(), location, null, null);
        EntityId bed = entityData.createEntity();
        entityData.setComponent(bed, new Position(0, new Vector3f(location.x, 1, location.y)));
        control.addExistingItem(bed, location);
        Stored stored = entityData.getComponent(bed, Stored.class);

        // A guard still carries its mark while its task lets go of the sold room
        control.destroy();
        entityData.setComponent(bed, stored);

        assertFalse(control.contains(bed));
    }

    @Test
    void newBedFacesARandomOneOf2048Directions() {
        EntityData entityData = new DefaultEntityData();
        List<Float> rotations = new ArrayList<>();
        IObjectsController objects = (IObjectsController) Proxy.newProxyInstance(
                IObjectsController.class.getClassLoader(), new Class<?>[]{IObjectsController.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("loadObject") || args.length != 5) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    rotations.add((Float) args[4]);
                    EntityId bed = entityData.createEntity();
                    entityData.setComponent(bed, new ObjectViewState((short) 1, null, ObjectViewState.GameObjectAnimState.MESH_RESOURCE, true));
                    return bed;
                });
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(
                IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getCreature" -> CreatureSleepFixtures.sleeper(9);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        RoomLairControl control = createControl(entityData, entityData.createEntity(), new Point(0, 0), kwdFile, objects);
        EntityId creature = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = 6;
        entityData.setComponents(creature, creatureComponent, new Owner((short) 1, (short) 1));

        for (int x = 0; x < 8; x++) {
            control.addItem(creature, new Point(x, 0));
        }

        // DK II's facing is a random 0..2047 out of a full turn (0x48AD50)
        assertEquals(8, rotations.size());
        for (float rotation : rotations) {
            float step = rotation / (FastMath.TWO_PI / 2048);
            assertTrue(rotation >= 0 && rotation < FastMath.TWO_PI);
            assertEquals(Math.round(step), step, 0.001f);
        }
        assertTrue(rotations.stream().distinct().count() > 1);
    }

    private static RoomLairControl createControl(EntityData entityData, EntityId roomEntity, Point location,
            IKwdFile kwdFile, IObjectsController objectsController) {
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
        return new RoomLairControl(kwdFile, room, entityData, () -> 0, objectsController) {
            @Override
            protected int getNumberOfAccessibleTiles() {
                return roomInstance.getCoordinates().size();
            }
        };
    }
}
