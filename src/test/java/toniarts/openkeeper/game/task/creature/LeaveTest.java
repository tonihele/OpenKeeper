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

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureFall;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
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
    private Vector3f position = new Vector3f(5, 0, 2);
    private Leave leave;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        entity = entityData.createEntity();
        entityData.setComponents(entity, new CreatureComponent(), new Owner(PLAYER_ID, PLAYER_ID));
        creature = (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getEntityId" -> entity;
                    case "getPosition" -> position;
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
        assertNull(entityData.getComponent(entity, CreatureFall.class));
    }

    @Test
    void creatureAtThePortalLeavesTheDungeon() {
        leave.executeTask(creature, 0.25f);

        // It falls through the middle of the portal like one dropped there, and leaves when it lands
        assertEquals(CreatureFall.FallType.PORTAL_DROP, entityData.getComponent(entity, CreatureFall.class).fallType);
    }

    @Test
    void creatureWalksToTheTileAboveTheMiddleAndStepsIn() {

        // The middle of the portal can't be walked onto, so the walk ends next to it whichever side the creature is on
        position = new Vector3f(5, 0, 6);

        assertEquals(new Vector2f(5, 3), leave.getTarget(creature));
        assertEquals(new Vector2f(5, 3.75f), leave.getWorkPosition(creature));
    }

    @Test
    void creatureStaysWhenThePortalIsGone() {
        portalRemoved = true;

        assertFalse(leave.isValid(creature));
    }

}
