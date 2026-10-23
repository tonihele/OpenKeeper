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
package toniarts.openkeeper.game.controller;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Crate;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.data.ResearchableType;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.DoorFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.tools.convert.map.TrapFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WorkshopOrdersTest {

    private static final short PLAYER = 3;
    private static final short ENEMY = 4;
    private static final short SENTRY = 3;
    private static final short WOODEN_DOOR = 1;

    private static final Trap SENTRY_TRAP = TrapFixtures.trapToManufacture(SENTRY, 1500);
    private static final Door WOODEN = DoorFixtures.doorToManufacture(WOODEN_DOOR, 1000, (short) 136);

    private final EntityData entityData = new DefaultEntityData();
    private WorkshopOrders orders;

    @BeforeEach
    void setUp() {
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getTrapById" -> SENTRY_TRAP;
            case "getDoorById" -> WOODEN;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        orders = new WorkshopOrders(entityData, kwdFile);
    }

    @AfterEach
    void tearDown() {
        orders.release();
    }

    @Test
    void doorBlueprintIsAnOrder() {
        doorBlueprint(PLAYER);

        WorkshopOrders.Order order = orders.getNext(PLAYER);

        assertEquals(ResearchableType.DOOR, order.type());
        assertEquals(WOODEN_DOOR, order.typeId());
        assertEquals(1000, order.manufToBuild());
        assertEquals(136, order.crateObjectId());
    }

    @Test
    void trapsAndDoorsAreManufacturedInTheOrderTheyWerePlaced() {
        doorBlueprint(PLAYER);
        trapBlueprint(PLAYER);

        assertEquals(ResearchableType.DOOR, orders.getNext(PLAYER).type());

        // The door has its crate, next is the trap
        crate(ResearchableType.DOOR, WOODEN_DOOR, PLAYER);

        assertEquals(ResearchableType.TRAP, orders.getNext(PLAYER).type());
        assertEquals(SENTRY, orders.getNext(PLAYER).typeId());
    }

    @Test
    void trapCrateDoesntFulfillADoorOrder() {
        doorBlueprint(PLAYER);
        crate(ResearchableType.TRAP, WOODEN_DOOR, PLAYER);

        assertEquals(ResearchableType.DOOR, orders.getNext(PLAYER).type());
    }

    @Test
    void builtDoorIsNoOrder() {
        EntityId door = doorBlueprint(PLAYER);
        entityData.setComponent(door, new DoorComponent(WOODEN_DOOR, false, false));

        assertNull(orders.getNext(PLAYER));
    }

    @Test
    void blueprintsOfOtherKeepersAreNoOrders() {
        doorBlueprint(ENEMY);
        trapBlueprint(ENEMY);

        assertNull(orders.getNext(PLAYER));
    }

    private EntityId doorBlueprint(short ownerId) {
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity, new DoorComponent(WOODEN_DOOR, false, true), new Owner(ownerId, ownerId));
        return entity;
    }

    private EntityId trapBlueprint(short ownerId) {
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity, new TrapComponent(SENTRY), new TrapViewState(SENTRY, true), new Owner(ownerId, ownerId));
        return entity;
    }

    private void crate(ResearchableType type, short typeId, short ownerId) {
        entityData.setComponents(entityData.createEntity(), new Crate(type, typeId), new Owner(ownerId, ownerId));
    }

}
