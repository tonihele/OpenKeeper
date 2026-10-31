/*
 * Copyright (C) 2014-2017 OpenKeeper
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

import com.badlogic.gdx.ai.fsm.StateMachine;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Fearless;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Threat;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A creature fighting enemies that threaten it more than its fear backs off
 * for a while, as DK II's level 1 Goblins do against a Giant
 */
class CreatureBackOffTest {

    private static final int GOBLIN_FEAR = 50;
    private static final int GIANT_THREAT = 160;

    private final EntityData data = new DefaultEntityData();
    private final Set<EntityId> creatures = new HashSet<>();
    private final Keeper keeper = new Keeper(false, Player.KEEPER1_ID);
    private final Map<MiscType, MiscVariable> settings = new EnumMap<>(MiscType.class);
    private double time;

    CreatureBackOffTest() {
        settings.put(MiscType.BACK_OFF_MINIMUM_DURATION_SECONDS, CreatureSleepFixtures.variable(MiscType.BACK_OFF_MINIMUM_DURATION_SECONDS, 0));
        settings.put(MiscType.BACK_OFF_MAXIMUM_DURATION_SECONDS, CreatureSleepFixtures.variable(MiscType.BACK_OFF_MAXIMUM_DURATION_SECONDS, 30));
    }

    @AfterEach
    void close() {
        data.close();
    }

    @Test
    void takesThreeFightTurnsOverFearToScare() {
        EntityId goblin = creature(Player.KEEPER1_ID, 10, GOBLIN_FEAR, 37, 1.26f);
        EntityId giant = creature(Player.GOOD_PLAYER_ID, 11, 400, GIANT_THREAT, 0.9f);
        fight(goblin, giant);
        fight(giant, goblin);
        CreatureController controller = controller(goblin);

        assertFalse(controller.isScared());
        assertFalse(controller.isScared());
        assertTrue(controller.isScared());
    }

    @Test
    void threatIsSharedBetweenTheTargetAndTheAttackers() {
        EntityId target = creature(Player.KEEPER1_ID, 10, GOBLIN_FEAR, 37, 1.26f);
        EntityId other = creature(Player.KEEPER1_ID, 12, GOBLIN_FEAR, 37, 1.26f);
        EntityId third = creature(Player.KEEPER1_ID, 13, GOBLIN_FEAR, 37, 1.26f);
        EntityId giant = creature(Player.GOOD_PLAYER_ID, 11, 400, GIANT_THREAT, 0.9f);
        fight(target, giant);
        fight(other, giant);
        fight(third, giant);
        fight(giant, target);

        // Three attackers and a target, a quarter each. The target gets two shares
        CreatureController otherController = controller(other);
        CreatureController targetController = controller(target);
        for (int i = 0; i < 5; i++) {
            assertFalse(otherController.isScared());
            targetController.isScared();
        }
        assertTrue(targetController.isScared());
        assertFalse(controller(giant).isScared());
    }

    @Test
    void ownHeartNearbyLightensTheThreat() {
        EntityId goblin = creature(Player.KEEPER1_ID, 10, GOBLIN_FEAR, 37, 1.26f);
        EntityId giant = creature(Player.GOOD_PLAYER_ID, 11, 400, GIANT_THREAT, 0.9f);
        fight(goblin, giant);
        fight(giant, goblin);
        CreatureController controller = controller(goblin);

        keeper.setDungeonHeartLocation(new Point(10, 7));
        for (int i = 0; i < 5; i++) {
            assertFalse(controller.isScared());
        }
        keeper.setDungeonHeartLocation(new Point(10, 6));
        controller.isScared();
        controller.isScared();
        assertTrue(controller.isScared());
    }

    @Test
    void backOffLastsLongerTheLessScared() {
        EntityId goblin = creature(Player.KEEPER1_ID, 10, GOBLIN_FEAR, 37, 1.26f);
        EntityId giant = creature(Player.GOOD_PLAYER_ID, 11, 400, GIANT_THREAT, 0.9f);
        fight(goblin, giant);
        fight(giant, goblin);
        CreatureController controller = controller(goblin);

        // Threat 160 is three times the fear: 30 turns, at 4 turns a second
        controller.startBackOff();
        time = 7.25;
        assertFalse(controller.isBackOffOver());
        time = 7.5;
        assertTrue(controller.isBackOffOver());

        // Near the heart the threat is 64, just over the fear: 90 turns
        keeper.setDungeonHeartLocation(new Point(10, 6));
        controller.startBackOff();
        time += 22.25;
        assertFalse(controller.isBackOffOver());
        time += 0.25;
        assertTrue(controller.isBackOffOver());
    }

    @Test
    void noBackingOffFromAFasterEnemy() {
        EntityId goblin = creature(Player.KEEPER1_ID, 10, GOBLIN_FEAR, 37, 1.26f);
        EntityId giant = creature(Player.GOOD_PLAYER_ID, 11, 400, GIANT_THREAT, 2f);
        fight(goblin, giant);
        fight(giant, goblin);
        CreatureController controller = controller(goblin);

        controller.startBackOff();

        assertTrue(controller.isBackOffOver());
    }

    @Test
    void fearlessCreatureIsNeverScared() {
        EntityId goblin = creature(Player.KEEPER1_ID, 10, GOBLIN_FEAR, 37, 1.26f);
        EntityId giant = creature(Player.GOOD_PLAYER_ID, 11, 400, GIANT_THREAT, 0.9f);
        fight(goblin, giant);
        fight(giant, goblin);
        data.setComponent(goblin, new Fearless(null));
        CreatureController controller = controller(goblin);

        for (int i = 0; i < 5; i++) {
            assertFalse(controller.isScared());
        }
    }

    @Test
    void scaredFighterBacksOffThenFightsAgainOrWithdraws() {
        AtomicReference<CreatureState> state = new AtomicReference<>(CreatureState.FIGHT);
        AtomicReference<Boolean> scared = new AtomicReference<>(true);
        AtomicReference<Boolean> over = new AtomicReference<>(false);
        AtomicReference<Boolean> canBackOff = new AtomicReference<>(true);
        ICreatureController creature = creature(state, scared, over, canBackOff);

        CreatureState.FIGHT.update(creature);
        assertEquals(CreatureState.BACK_OFF, state.get());

        CreatureState.BACK_OFF.update(creature);
        assertEquals(CreatureState.BACK_OFF, state.get());

        canBackOff.set(false);
        CreatureState.BACK_OFF.update(creature);
        assertEquals(CreatureState.FLEE, state.get());

        state.set(CreatureState.BACK_OFF);
        canBackOff.set(true);
        over.set(true);
        CreatureState.BACK_OFF.update(creature);
        assertEquals(CreatureState.FLEE, state.get());

        state.set(CreatureState.BACK_OFF);
        over.set(false);
        scared.set(false);
        CreatureState.BACK_OFF.update(creature);
        assertEquals(CreatureState.FIGHT, state.get());
    }

    private EntityId creature(short ownerId, float x, int fear, int threat, float runSpeed) {
        EntityId entity = data.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.fear = fear;
        creatureComponent.runSpeed = runSpeed;
        data.setComponents(entity, creatureComponent, new Threat(threat), new Health(1000, 1000),
                new Owner(ownerId, ownerId), new Position(0, new Vector3f(x, WorldUtils.FLOOR_HEIGHT, 10)));
        creatures.add(entity);
        return entity;
    }

    private void fight(EntityId attacker, EntityId target) {
        data.setComponent(attacker, new AttackTarget(target));
    }

    private CreatureController controller(EntityId entity) {
        IEntityPositionLookup lookup = proxy(IEntityPositionLookup.class, (method, args) -> switch (method) {
            case "getSensedEntities" -> {
                Set<EntityId> sensed = new HashSet<>(creatures);
                sensed.remove(entity);
                yield sensed;
            }
            default -> throw new UnsupportedOperationException(method);
        });
        IKwdFile kwd = proxy(IKwdFile.class, (method, args) -> switch (method) {
            case "getGameLevel" -> CreatureSleepFixtures.level(4);
            default -> throw new UnsupportedOperationException(method);
        });
        Keeper heroes = new Keeper(false, Player.GOOD_PLAYER_ID);
        ILevelInfo levelInfo = proxy(ILevelInfo.class, (method, args) -> switch (method) {
            case "getPlayer" -> (Short) args[0] == Player.KEEPER1_ID ? keeper : heroes;
            case "getLevelData" -> kwd;
            default -> throw new UnsupportedOperationException(method);
        });
        return new CreatureController(entity, data, CreatureSleepFixtures.sleeper(1), null, null, () -> time,
                settings, null, lookup, null, levelInfo, null, null, null);
    }

    private static ICreatureController creature(AtomicReference<CreatureState> state, AtomicReference<Boolean> scared,
            AtomicReference<Boolean> over, AtomicReference<Boolean> canBackOff) {
        StateMachine<?, ?> stateMachine = proxy(StateMachine.class, (method, args) -> switch (method) {
            case "changeState" -> {
                state.set((CreatureState) args[0]);
                yield null;
            }
            case "getCurrentState" -> state.get();
            default -> throw new UnsupportedOperationException(method);
        });
        AtomicReference<ICreatureController> self = new AtomicReference<>();
        self.set(proxy(ICreatureController.class, (method, args) -> switch (method) {
            case "getStateMachine" -> stateMachine;
            case "getAttackTarget" -> self.get();
            case "isScared" -> scared.get();
            case "isBackOffOver" -> over.get();
            case "backOff" -> canBackOff.get();
            default -> throw new UnsupportedOperationException(method);
        }));
        return self.get();
    }

    @FunctionalInterface
    private interface Invocation {

        Object call(String method, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation call) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (object, method, args) -> switch (method.getName()) {
                    case "toString" -> type.getSimpleName();
                    case "hashCode" -> System.identityHashCode(object);
                    case "equals" -> object == args[0];
                    default -> call.call(method.getName(), args);
                });
    }
}
