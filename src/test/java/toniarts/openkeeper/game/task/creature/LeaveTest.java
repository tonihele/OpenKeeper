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
package toniarts.openkeeper.game.task.creature;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.LeavingDungeon;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.logic.CreatureLeaveSystem;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaveTest {

    private static final short PLAYER_ID = 3;

    private EntityData entityData;
    private EntityId entity;
    private ICreatureController creature;
    private boolean portalRemoved;
    private Leave leave;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        entity = entityData.createEntity();
        creature = (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getEntityId" -> entity;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "creature " + entity;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        IRoomController portal = (IRoomController) Proxy.newProxyInstance(IRoomController.class.getClassLoader(),
                new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isRemoved" -> portalRemoved;
                    default -> throw new UnsupportedOperationException(method.getName());
                });

        leave = new Leave(null, null, new Point(5, 4), PLAYER_ID, portal, entityData);
        leave.assign(creature, false);
    }

    @Test
    void creatureWalkingToThePortalIsStillThere() {
        assertTrue(leave.isValid(creature));
        assertNull(entityData.getComponent(entity, LeavingDungeon.class));
    }

    @Test
    void creatureAtThePortalLeavesTheDungeon() {
        List<EntityId> left = new ArrayList<>();
        ICreaturesController creaturesController = (ICreaturesController) Proxy.newProxyInstance(
                ICreaturesController.class.getClassLoader(), new Class<?>[]{ICreaturesController.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "leaveDungeon" -> left.add((EntityId) args[0]);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        CreatureLeaveSystem leaveSystem = new CreatureLeaveSystem(entityData, creaturesController);

        leave.executeTask(creature, 0.25f);
        leaveSystem.processTick(0.25f);

        assertEquals(List.of(entity), left);
    }

    @Test
    void creatureStaysWhenThePortalIsGone() {
        portalRemoved = true;

        assertFalse(leave.isValid(creature));
    }

}
