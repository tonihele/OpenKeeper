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
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.task.worker.CarryEnemyCreatureToPrison;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static toniarts.openkeeper.tools.convert.map.RoomFixtures.roomWithObjects;

class PrisonControllerTest {

    @Test
    void prisonWithOneInsideTileTakesTwoPrisoners() {
        assertEquals(2, buildPrison(3).getMaxCapacity(ObjectType.PRISONER));
    }

    @Test
    void prisonWithNineInsideTilesTakesTenPrisoners() {
        assertEquals(10, buildPrison(5).getMaxCapacity(ObjectType.PRISONER));
    }

    @Test
    void prisonWithoutInsideTilesTakesNoPrisoners() {
        assertEquals(0, buildPrison(2).getMaxCapacity(ObjectType.PRISONER));
    }

    @Test
    void newPrisonIsOpen() {
        PrisonController prison = buildPrison(3);

        assertTrue(prison.isPrisonOpen());
        assertFalse(prison.isFullCapacity());
    }

    @Test
    void closedPrisonTakesNoCaptives() {
        PrisonController prison = buildPrison(3);

        prison.setOpen(false);

        assertTrue(prison.isFullCapacity());
        assertTrue(prison.getObjectControl(ObjectType.PRISONER).isFullCapacity());
    }

    @Test
    void doorBarOpensAndClosesThePrison() {
        EntityData entityData = new DefaultEntityData();
        PrisonController prison = buildPrison(entityData, 3);
        EntityId doorBar = entityData.createEntity();
        entityData.setComponent(doorBar, new ObjectComponent((short) 116, null));

        assertTrue(prison.switchFurniture(doorBar));
        assertFalse(prison.isPrisonOpen());
        assertTrue(prison.switchFurniture(doorBar));
        assertTrue(prison.isPrisonOpen());
    }

    @Test
    void otherFurnitureIsNoSwitch() {
        EntityData entityData = new DefaultEntityData();
        PrisonController prison = buildPrison(entityData, 3);
        EntityId skull = entityData.createEntity();
        entityData.setComponent(skull, new ObjectComponent((short) 49, null));

        assertFalse(prison.switchFurniture(skull));
        assertTrue(prison.isPrisonOpen());
    }

    @Test
    void prisonStaysClosedWhenItGrows() {
        PrisonController prison = buildPrison(3);
        prison.setOpen(false);

        prison.construct();

        assertFalse(prison.isPrisonOpen());
    }

    @Test
    void prisonerCarriedInTakesASpace() {
        EntityData entityData = new DefaultEntityData();
        PrisonController prison = buildPrison(entityData, 3);
        EntityId prisonerId = entityData.createEntity();
        ICreatureController prisoner = (ICreatureController) Proxy.newProxyInstance(
                ICreatureController.class.getClassLoader(),
                new Class<?>[]{ICreatureController.class},
                (proxy, method, args) -> method.getName().equals("getEntityId") ? prisonerId : null);

        new CarryEnemyCreatureToPrison(null, null, new Point(2, 2), (short) 3, prison, null, prisoner).executeTask(null, 0);

        assertEquals(1, prison.getUsedCapacity(ObjectType.PRISONER));
    }

    private static PrisonController buildPrison(int size) {
        return buildPrison(new DefaultEntityData(), size);
    }

    private static PrisonController buildPrison(EntityData entityData, int size) {
        IObjectsController objectsController = (IObjectsController) Proxy.newProxyInstance(
                IObjectsController.class.getClassLoader(),
                new Class<?>[]{IObjectsController.class},
                (proxy, method, args) -> method.getName().equals("loadObject") ? entityData.createEntity() : null);

        // Prison objects in DK II: Skull, Bone and Lavatory
        RoomInstance roomInstance = new RoomInstance(roomWithObjects((short) 11, (short) 49, (short) 50, (short) 51, (short) 0, (short) 0, (short) 0));
        for (int x = 1; x <= size; x++) {
            for (int y = 1; y <= size; y++) {
                roomInstance.addCoordinate(new Point(x, y));
            }
        }

        PrisonController room = new PrisonController(entityData.createEntity(), entityData, null, roomInstance, objectsController, null);
        room.construct();
        return room;
    }
}
