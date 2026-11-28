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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureFall;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatureFallSystemTest {

    /**
     * A tile per turn per turn
     */
    private static final float GRAVITY_CONSTANT = 256;
    private static final float TURN = 1f / 4;

    private DefaultEntityData entityData;
    private final Set<Point> solidTiles = new HashSet<>();
    private final List<Integer> effects = new ArrayList<>();
    private final List<EntityId> left = new ArrayList<>();
    private final List<GameEventType> events = new ArrayList<>();
    private CreatureFallSystem fallSystem;
    private CreatureLandingSystem landingSystem;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        IGameTimer gameTimer = (IGameTimer) Proxy.newProxyInstance(IGameTimer.class.getClassLoader(),
                new Class<?>[]{IGameTimer.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getGameTime" -> 0d;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        FakeMapData mapData = new FakeMapData(10, 10);
        IMapInformation<?> mapInformation = (IMapInformation<?>) Proxy.newProxyInstance(IMapInformation.class.getClassLoader(),
                new Class<?>[]{IMapInformation.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getMapData" -> mapData;
            case "isSolid" -> solidTiles.contains((Point) args[0]);
            default -> throw new UnsupportedOperationException(method.getName());
        });

        GameEventService gameEventService = (GameEventService) Proxy.newProxyInstance(GameEventService.class.getClassLoader(),
                new Class<?>[]{GameEventService.class}, (proxy, method, args) -> {
            if (method.getName().equals("emitEffect")) {
                effects.add((Integer) args[0]);
            } else if (method.getName().equals("emit")) {
                events.add((GameEventType) args[0]);
            }
            return null;
        });
        ICreaturesController creaturesController = (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(),
                new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> {
            if (method.getName().equals("leaveDungeon")) {
                left.add((EntityId) args[0]);
            }
            return null;
        });
        fallSystem = new CreatureFallSystem(entityData);
        landingSystem = new CreatureLandingSystem(entityData, mapInformation, GRAVITY_CONSTANT, gameTimer, gameEventService, creaturesController);
    }

    @AfterEach
    void tearDown() {
        fallSystem.stop();
        landingSystem.stop();
        entityData.close();
    }

    @Test
    void thrownCreatureFliesAWholeTurnOnEachLogicTickAndLandsWhereItsVelocityTakesIt() {
        EntityId creature = createThrownCreature(new Vector3f(0, 1, 1));

        // The animation loop leaves a thrown creature to the game logic
        fallSystem.processTick(TURN);
        assertEquals(new Vector3f(2, WorldUtils.FLOOR_HEIGHT + 1, 2), entityData.getComponent(creature, Position.class).position);

        landingSystem.processTick(TURN);

        assertEquals(new Vector3f(2, WorldUtils.FLOOR_HEIGHT + 1, 3), entityData.getComponent(creature, Position.class).position);
        assertEquals(new Vector3f(0, 0, 1), entityData.getComponent(creature, CreatureFall.class).velocity);
        assertNull(entityData.getComponent(creature, CreatureAi.class));
        assertNull(entityData.getComponent(creature, Damage.class));

        // It lands on the tick it comes down
        landingSystem.processTick(TURN);

        assertEquals(new Vector3f(2, WorldUtils.FLOOR_HEIGHT, 4), entityData.getComponent(creature, Position.class).position);
        assertNull(entityData.getComponent(creature, CreatureFall.class));
        assertNotNull(entityData.getComponent(creature, CreatureAi.class));
    }

    @Test
    void thrownCreatureBouncesOffAWall() {
        solidTiles.add(new Point(2, 4));
        EntityId creature = createThrownCreature(new Vector3f(0, 1, 2));

        landingSystem.processTick(TURN);
        landingSystem.processTick(TURN);

        assertEquals(new Vector3f(2, WorldUtils.FLOOR_HEIGHT, 1), entityData.getComponent(creature, Position.class).position);
    }

    @Test
    void thrownCreatureTakesTheDamageAndRaisesDustAsItLands() {
        EntityId creature = createThrownCreature(new Vector3f(0, 0, 0));

        landingSystem.processTick(TURN);

        assertEquals(WorldUtils.FLOOR_HEIGHT, entityData.getComponent(creature, Position.class).position.y);
        assertEquals(50, entityData.getComponent(creature, Damage.class).damage);
        assertEquals(List.of(117), effects);
    }

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
        landingSystem.processTick(TURN);
        assertTrue(left.isEmpty());

        fallSystem.processTick(10f);
        landingSystem.processTick(TURN);
        assertEquals(List.of(GameEventType.CREATURE_LAND), events);
        assertEquals(WorldUtils.UNDERFLOOR_HEIGHT, entityData.getComponent(creature, Position.class).position.y,
                "it falls through the floor");
        assertNull(entityData.getComponent(creature, CreatureAi.class), "it doesn't get up");
        assertEquals(List.of(creature), left);
    }

    private EntityId fall(CreatureFall creatureFall, float stunDuration) {
        EntityId creature = drop(creatureFall, stunDuration);
        fallSystem.processTick(10f);
        landingSystem.processTick(TURN);

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

    private EntityId createThrownCreature(Vector3f velocity) {
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity, new CreatureComponent(), new Position(0, new Vector3f(2, WorldUtils.FLOOR_HEIGHT + 1, 2)),
                new CreatureFall(velocity, 50));
        return entity;
    }

}
