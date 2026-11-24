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
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureFall;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * A creature dropped from the hand lands stunned, a conjured one lands ready to
 * go and one dropped on the middle of its keeper's portal falls through the
 * floor and leaves the dungeon
 */
class CreatureFallSystemTest {

    private final DefaultEntityData entityData = new DefaultEntityData();
    private final List<EntityId> left = new ArrayList<>();
    private final List<GameEventType> events = new ArrayList<>();
    private final CreatureFallSystem fallSystem = new CreatureFallSystem(entityData, () -> 0,
            proxy(GameEventService.class, args -> events.add((GameEventType) args[0])),
            proxy(ICreaturesController.class, args -> left.add((EntityId) args[0])));

    @Test
    void droppedCreatureLandsStunned() {
        EntityId creature = fall(new CreatureFall(), 3f);

        assertEquals(CreatureState.FALLEN, entityData.getComponent(creature, CreatureAi.class).getCreatureState());
        assertNull(entityData.getComponent(creature, CreatureFall.class));
        assertEquals(WorldUtils.FLOOR_HEIGHT, entityData.getComponent(creature, Position.class).position.y);
        assertTrue(left.isEmpty(), "it stays");
    }

    @Test
    void conjuredCreatureIsNotStunned() {
        EntityId creature = fall(new CreatureFall(CreatureFall.FallType.CONJURED), 3f);

        assertEquals(CreatureState.IDLE, entityData.getComponent(creature, CreatureAi.class).getCreatureState());
    }

    @Test
    void droppedCreatureWithoutStunDurationIsNotStunned() {
        EntityId creature = fall(new CreatureFall(), 0f);

        assertEquals(CreatureState.IDLE, entityData.getComponent(creature, CreatureAi.class).getCreatureState());
    }

    @Test
    void creatureDroppedOnThePortalLeavesWhenItLands() {
        EntityId creature = drop(new CreatureFall(CreatureFall.FallType.PORTAL_DROP), 3f);

        // Still falling
        fallSystem.processTick(0.1f);
        assertTrue(left.isEmpty());

        fallSystem.processTick(10f);
        assertEquals(List.of(GameEventType.CREATURE_LAND), events);
        assertEquals(WorldUtils.UNDERFLOOR_HEIGHT, entityData.getComponent(creature, Position.class).position.y,
                "it falls through the floor");
        assertNull(entityData.getComponent(creature, CreatureAi.class), "it doesn't get up");
        assertEquals(List.of(creature), left);
    }

    private EntityId fall(CreatureFall creatureFall, float stunDuration) {
        EntityId creature = drop(creatureFall, stunDuration);
        fallSystem.processTick(10f);

        return creature;
    }

    private EntityId drop(CreatureFall creatureFall, float stunDuration) {
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.stunDuration = stunDuration;

        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature, creatureComponent, creatureFall,
                new Position(0, new Vector3f(1, WorldUtils.DROP_HEIGHT, 1)));

        return creature;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Consumer<Object[]> call) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            call.accept(args);
            return null;
        });
    }

}
