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
package toniarts.openkeeper.game.controller.creature;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Fearless;
import toniarts.openkeeper.game.component.Objective;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.task.ITaskManager;
import toniarts.openkeeper.game.task.creature.Rebel;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureFlagFixtures;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Thing;

/**
 * A rebel that can't get to a portal turns on its keeper
 */
class CreatureRebelTest {

    private static final int TICKS_PER_SECOND = 4;

    private EntityData entityData;
    private EntityId entityId;
    private List<Object[]> events;
    private List<Thing.HeroParty.Objective> followed;
    private CreatureController controller;
    private Rebel rebel;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        entityId = entityData.createEntity();
        entityData.setComponent(entityId, new Owner(Player.KEEPER1_ID, Player.KEEPER1_ID));
        entityData.setComponent(entityId, new CreatureComponent());
        entityData.setComponent(entityId, new Position(0, new Vector3f(5, 0, 4)));
        entityData.setComponent(entityId, new Fearless());
        events = new ArrayList<>();
        GameEventService eventService = (GameEventService) Proxy.newProxyInstance(GameEventService.class.getClassLoader(),
                new Class<?>[]{GameEventService.class}, (proxy, method, args) -> events.add(args));
        followed = new ArrayList<>();
        ITaskManager taskManager = (ITaskManager) Proxy.newProxyInstance(ITaskManager.class.getClassLoader(),
                new Class<?>[]{ITaskManager.class}, (proxy, method, args) -> switch (method.getName()) {
            case "assignObjectiveTask" -> followed.add((Thing.HeroParty.Objective) args[1]);
            default -> throw new UnsupportedOperationException(method.getName());
        });
        IEntityPositionLookup lookup = (IEntityPositionLookup) Proxy.newProxyInstance(IEntityPositionLookup.class.getClassLoader(),
                new Class<?>[]{IEntityPositionLookup.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getSensedEntities" -> Set.of();
            default -> throw new UnsupportedOperationException(method.getName());
        });
        controller = new CreatureController(entityId, entityData,
                CreatureFlagFixtures.creature((short) 5, EnumSet.noneOf(Creature.CreatureFlag.class)), null, taskManager, () -> 0, null,
                null, lookup, null, null, null, null, eventService);
        rebel = new Rebel(null, null, controller, TICKS_PER_SECOND);
    }

    @AfterEach
    void tearDown() {
        entityData.close();
    }

    @Test
    void rebelStaysWithItsKeeperForThirtyTicks() {
        rebel.executeTask(controller, 30f / TICKS_PER_SECOND);

        assertEquals(Player.KEEPER1_ID, entityData.getComponent(entityId, Owner.class).ownerId);
        assertNull(entityData.getComponent(entityId, Objective.class));
        assertTrue(events.isEmpty());
    }

    @Test
    void rebelJoinsTheHeroesAndGoesAfterItsKeeper() {
        rebel.executeTask(controller, 31f / TICKS_PER_SECOND);

        assertEquals(Player.GOOD_PLAYER_ID, entityData.getComponent(entityId, Owner.class).ownerId);
        Objective objective = entityData.getComponent(entityId, Objective.class);
        assertEquals(Thing.HeroParty.Objective.KILL_PLAYER, objective.objective);
        assertEquals(Player.KEEPER1_ID, objective.objectiveTargetPlayerId);
        assertEquals(1, events.size());
        assertArrayEquals(new Object[]{GameEventType.CREATURE_REBELLED, Player.KEEPER1_ID}, events.getFirst());
        assertEquals(List.of(Thing.HeroParty.Objective.KILL_PLAYER), followed);
    }

}
