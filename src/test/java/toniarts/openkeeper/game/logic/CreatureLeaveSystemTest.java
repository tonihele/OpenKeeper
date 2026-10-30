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
package toniarts.openkeeper.game.logic;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureFall;
import toniarts.openkeeper.game.component.LeavingDungeon;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatureLeaveSystemTest {

    private final EntityData entityData = new DefaultEntityData();
    private final List<EntityId> left = new ArrayList<>();
    private final List<GameEventType> events = new ArrayList<>();
    private final CreatureFallSystem fallSystem = new CreatureFallSystem(entityData, () -> 0,
            proxy(GameEventService.class, args -> events.add((GameEventType) args[0])));
    private final CreatureLeaveSystem leaveSystem = new CreatureLeaveSystem(entityData,
            proxy(ICreaturesController.class, args -> {
                left.add((EntityId) args[0]);
                entityData.removeEntity((EntityId) args[0]);
            }));

    @Test
    void creatureDroppedOnThePortalLeavesWhenItLands() {
        EntityId creature = dropCreature(true);

        // Still falling
        leaveSystem.processTick(0.1f);
        assertTrue(left.isEmpty());

        land();
        assertEquals(List.of(GameEventType.CREATURE_LAND), events);
        assertNull(entityData.getComponent(creature, CreatureFall.class));
        assertNull(entityData.getComponent(creature, CreatureAi.class), "it doesn't get up");

        leaveSystem.processTick(0.1f);
        leaveSystem.processTick(0.1f);
        assertEquals(List.of(creature), left);
    }

    @Test
    void otherCreaturesGetUpAndStay() {
        EntityId creature = dropCreature(false);

        land();
        leaveSystem.processTick(0.1f);

        assertNotNull(entityData.getComponent(creature, CreatureAi.class));
        assertTrue(left.isEmpty());
    }

    private EntityId dropCreature(boolean leaving) {
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature, new CreatureComponent(), new CreatureFall(),
                new Position(0, new Vector3f(3, WorldUtils.DROP_HEIGHT, 4)));
        if (leaving) {
            entityData.setComponent(creature, new LeavingDungeon());
        }
        return creature;
    }

    private void land() {
        fallSystem.processTick(1f);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.util.function.Consumer<Object[]> call) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            call.accept(args);
            return null;
        });
    }
}
