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
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Like in DK II, each torture device holds one victim
 */
class RoomTortureeControlTest {

    private final EntityData entityData = new DefaultEntityData();
    private final EntityId roomEntity = entityData.createEntity();
    private final Set<EntityId> devices = new LinkedHashSet<>();
    private final EntityId wheel = device(new Point(3, 3));
    private final EntityId chair = device(new Point(6, 3));
    private final RoomTortureeControl control = new RoomTortureeControl(kwdFile(), room(), entityData, null) {

        @Override
        protected int getNumberOfAccessibleTiles() {
            return devices.size();
        }
    };

    @Test
    void victimGoesOnTheClosestFreeDevice() {
        assertEquals(chair, control.addItem(entityData.createEntity(), new Point(5, 3)));
        assertEquals(wheel, control.addItem(entityData.createEntity(), new Point(5, 3)));
    }

    @Test
    void fullChamberTakesNoMoreVictims() {
        control.addItem(entityData.createEntity(), new Point(3, 3));
        control.addItem(entityData.createEntity(), new Point(3, 3));
        EntityId third = entityData.createEntity();

        assertNull(control.addItem(third, new Point(3, 3)));
        assertTrue(control.isFullCapacity());
        assertNull(entityData.getComponent(third, Stored.class));
    }

    @Test
    void removedVictimFreesItsDevice() {
        EntityId victim = entityData.createEntity();
        control.addItem(victim, new Point(3, 3));

        control.removeItem(victim);

        assertEquals(0, control.getCurrentCapacity());
        assertEquals(wheel, control.addItem(entityData.createEntity(), new Point(3, 3)));
    }

    @Test
    void victimIsStoredInTheRoom() {
        EntityId victim = entityData.createEntity();
        control.addItem(victim, new Point(3, 3));

        Stored stored = entityData.getComponent(victim, Stored.class);
        assertNotNull(stored);
        assertEquals(ObjectType.TORTUREE, stored.objectType);
        assertEquals(1, control.getCurrentCapacity());
    }

    @Test
    void destroyedChamberSetsTheVictimsFree() {
        EntityId victim = entityData.createEntity();
        entityData.setComponent(victim, new CreatureTortured(5, 0, 0));
        control.addItem(victim, new Point(3, 3));

        control.destroy();

        assertNull(entityData.getComponent(victim, Stored.class));
        assertNull(entityData.getComponent(victim, CreatureTortured.class));
    }

    @Test
    void victimGetsTheNewDeviceWhenTheFurnitureIsBuiltAgain() {
        EntityId victim = entityData.createEntity();
        entityData.setComponent(victim, new CreatureTortured(5, 0, 0, control.addItem(victim, new Point(3, 3))));

        devices.remove(wheel);
        EntityId newWheel = device(new Point(3, 3));
        control.refreshDevices();

        assertEquals(newWheel, entityData.getComponent(victim, CreatureTortured.class).device);
    }

    @Test
    void victimWhoseDeviceMovedGoesOnTheClosestFreeDevice() {
        EntityId victim = entityData.createEntity();
        entityData.setComponent(victim, new CreatureTortured(5, 0, 0, control.addItem(victim, new Point(3, 3))));

        // The room grew and its wheel now stands elsewhere
        devices.remove(wheel);
        EntityId newWheel = device(new Point(3, 4));
        control.refreshDevices();

        assertEquals(newWheel, entityData.getComponent(victim, CreatureTortured.class).device);
        assertEquals(new Point(3, 4), WorldUtils.vectorToPoint(entityData.getComponent(victim, Position.class).position));
        assertEquals(1, control.getCurrentCapacity());
    }

    @Test
    void victimWithNoDeviceLeftIsSetFree() {
        EntityId first = entityData.createEntity();
        entityData.setComponent(first, new CreatureTortured(5, 0, 0, control.addItem(first, new Point(6, 3))));
        EntityId second = entityData.createEntity();
        entityData.setComponent(second, new CreatureTortured(5, 0, 0, control.addItem(second, new Point(3, 3))));

        devices.remove(wheel);
        control.refreshDevices();

        assertNull(entityData.getComponent(second, CreatureTortured.class));
        assertNull(entityData.getComponent(second, Stored.class));
        assertEquals(chair, entityData.getComponent(first, CreatureTortured.class).device);
        assertEquals(1, control.getCurrentCapacity());
    }

    @Test
    void creatureThatCantBeElectrocutedSkipsTheChair() {
        entityData.setComponent(chair, new ObjectComponent((short) 61, ObjectType.TORTUREE));
        CreatureComponent creature = new CreatureComponent();
        creature.creatureId = 6;
        EntityId victim = entityData.createEntity();
        entityData.setComponent(victim, creature);

        // The chair is closer, but the test creature isn't flagged CAN_BE_ELECTROCUTED
        assertEquals(wheel, control.addItem(victim, new Point(5, 3)));
    }

    @Test
    void victimSkipsTheDeviceACreatureTorturesItselfOn() {
        EntityId mistress = entityData.createEntity();
        entityData.setComponent(mistress, new TaskComponent(1, wheel, new Point(3, 3), TaskType.TORTURE, 0, true));
        EntityId onItsWay = entityData.createEntity();
        entityData.setComponent(onItsWay, new TaskComponent(2, chair, new Point(6, 3), TaskType.TORTURE, 0, false));

        // The wheel is closer, but the Mistress is on it. The chair is only her colleague's destination
        assertEquals(chair, control.addItem(entityData.createEntity(), new Point(3, 3)));
        assertNull(control.addItem(entityData.createEntity(), new Point(3, 3)));
    }

    private IKwdFile kwdFile() {
        return (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getCreature" -> CreatureSleepFixtures.sleeper(9);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private EntityId device(Point p) {
        EntityId device = entityData.createEntity();
        entityData.setComponent(device, new Position(0, WorldUtils.pointToVector3f(p)));
        devices.add(device);
        return device;
    }

    private IRoomController room() {
        return (IRoomController) Proxy.newProxyInstance(IRoomController.class.getClassLoader(),
                new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getEntityId" -> roomEntity;
                    case "getOwnerId" -> (short) 3;
                    case "getFloorFurniture" -> devices;
                    case "getWallFurniture" -> Set.of();
                    case "getFloorFurnitureCount" -> devices.size();
                    case "getWallFurnitureCount" -> 0;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

}
