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
package toniarts.openkeeper.tools.convert.map;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMeleeAttack;
import toniarts.openkeeper.game.component.CreatureSpells;
import toniarts.openkeeper.game.component.Fearless;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Threat;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.CreatureController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.trigger.creature.CreatureTriggerLogicController;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetFightFlagActionTest {
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 255})
    void onlyOneAllowsOrdinaryTargetSelection(int available) {
        Fixture f = new Fixture();
        f.action(available, true);
        assertEquals(available != 1, f.stats().fightDisabled);
        assertSame(available == 1 ? f.enemyController : null, f.actorController.getAttackTarget());
    }

    @Test
    void disablingPreservesCachedTargetAndEnablingAllowsItAgain() {
        Fixture f = new Fixture();
        assertSame(f.enemyController, f.actorController.getAttackTarget());
        AttackTarget target = f.data.getComponent(f.actor, AttackTarget.class);
        f.action(0, true);
        assertNull(f.actorController.getAttackTarget());
        assertSame(target, f.data.getComponent(f.actor, AttackTarget.class));
        f.action(1, true);
        assertSame(f.enemyController, f.actorController.getAttackTarget());
    }

    @Test
    void reEnablingAllowsFreshTargetAcquisition() {
        Fixture f = new Fixture();
        f.action(0, true);
        assertNull(f.actorController.getAttackTarget());
        assertNull(f.data.getComponent(f.actor, AttackTarget.class));
        f.action(1, true);
        assertSame(f.enemyController, f.actorController.getAttackTarget());
    }

    @Test
    void flagDoesNotMakeCreatureUntargetableByItsEnemy() {
        Fixture f = new Fixture();
        f.action(0, true);
        assertSame(f.actorController, f.enemyController.getAttackTarget());
    }

    @Test
    void disabledCreatureStillEvaluatesFearAndKeepsFleeing() {
        Fixture f = new Fixture();
        f.action(0, true);
        f.data.removeComponent(f.actor, Fearless.class);
        f.actorController.getStateMachine().setInitialState(CreatureState.FLEE);
        assertTrue(f.actorController.shouldFleeOrAttack());
        assertSame(CreatureState.FLEE, f.actorController.getStateMachine().getCurrentState());
    }

    @Test
    void disabledCreatureDoesNotChooseToFightButEnabledOneCan() {
        Fixture f = new Fixture();
        f.actorController.getStateMachine().setInitialState(CreatureState.FIGHT);
        f.action(0, true);
        assertFalse(f.actorController.shouldFleeOrAttack());
        f.action(1, true);
        assertTrue(f.actorController.shouldFleeOrAttack());
    }

    @Test
    void copiedAttributesRetainFlagAndOtherStatsArePreserved() {
        Fixture f = new Fixture();
        CreatureComponent before = f.stats();
        f.action(0, true);
        CreatureComponent updated = new CreatureComponent(f.stats());
        assertTrue(updated.fightDisabled);
        assertEquals(before.creatureId, updated.creatureId);
        assertEquals(before.speed, updated.speed);
        assertEquals(before.fear, updated.fear);
        assertEquals(before.worker, updated.worker);
        f.data.setComponent(f.actor, updated);
        assertNull(f.actorController.getAttackTarget());
    }

    @Test
    void unboundScriptAndRemovedCreatureAreSafe() {
        Fixture f = new Fixture();
        assertDoesNotThrow(() -> f.action(0, false));
        assertFalse(f.stats().fightDisabled);
        f.data.removeEntity(f.actor);
        assertDoesNotThrow(() -> f.actorController.setFightEnabled(false));
        assertNull(f.stats());
    }

    private static final class Fixture {
        final DefaultEntityData data = new DefaultEntityData();
        final EntityId actor = entity(Player.KEEPER1_ID);
        final EntityId enemy = entity(Player.GOOD_PLAYER_ID);
        final CreatureController actorController;
        final CreatureController enemyController;
        final ICreaturesController creatures;
        final ILevelInfo info;
        final IKwdFile kwd;
        int available;

        Fixture() {
            Creature definition = new Creature();
            definition.setCreatureId((short) 7);
            definition.setFlags(EnumSet.noneOf(Creature.CreatureFlag.class));
            definition.setFightStyle(Creature.FightStyle.BLITZER);
            kwd = proxy(IKwdFile.class, (method, args) -> switch (method) {
                case "getThings" -> {
                    Thing.KeeperCreature thing = new Thing.KeeperCreature();
                    thing.setTriggerId(1);
                    thing.setPlayerId(Player.KEEPER1_ID);
                    yield args[0] == Thing.KeeperCreature.class ? List.of(thing) : List.of();
                }
                case "getTriggers" -> {
                    TriggerAction action = new TriggerAction(null);
                    action.setId(1);
                    action.setType(TriggerAction.ActionType.SET_FIGHT_FLAG);
                    action.setUserData("available", (short) available);
                    yield Map.of(1, action);
                }
                default -> throw new UnsupportedOperationException(method);
            });
            Keeper keeper = new Keeper(false, Player.KEEPER1_ID);
            Keeper hero = new Keeper(false, Player.GOOD_PLAYER_ID);
            info = proxy(ILevelInfo.class, (method, args) -> switch (method) {
                case "getLevelData" -> kwd;
                case "getPlayer" -> (short) args[0] == Player.KEEPER1_ID ? keeper : hero;
                default -> throw new UnsupportedOperationException(method);
            });
            creatures = proxy(ICreaturesController.class, (method, args) -> switch (method) {
                case "createController" -> args[0].equals(actor) ? actorController() : enemyController();
                case "isValidEntity" -> data.getComponent((EntityId) args[0], CreatureComponent.class) != null;
                default -> throw new UnsupportedOperationException(method);
            });
            IEntityPositionLookup lookup = proxy(IEntityPositionLookup.class, (method, args) -> {
                if (method.equals("getSensedEntities")) return Set.of(args[0].equals(actor) ? enemy : actor);
                throw new UnsupportedOperationException(method);
            });
            IMapController map = proxy(IMapController.class, (method, args) -> {
                if (method.equals("getRoomControllerByCoordinates")) return null;
                throw new UnsupportedOperationException(method);
            });
            Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> settings = Map.of(
                    Variable.MiscVariable.MiscType.CREATURE_CRITICAL_HEALTH_PERCENTAGE_OF_MAX,
                    CreatureSleepFixtures.variable(Variable.MiscVariable.MiscType.CREATURE_CRITICAL_HEALTH_PERCENTAGE_OF_MAX, 20));
            actorController = new CreatureController(actor, data, definition, null, null, () -> 0,
                    settings, creatures, lookup, map, info, null, null, null);
            enemyController = new CreatureController(enemy, data, definition, null, null, () -> 0,
                    settings, creatures, lookup, map, info, null, null, null);
        }

        private CreatureController actorController() { return actorController; }
        private CreatureController enemyController() { return enemyController; }

        private EntityId entity(short owner) {
            EntityId id = data.createEntity();
            CreatureComponent stats = new CreatureComponent();
            stats.creatureId = 7;
            stats.speed = 2;
            data.setComponents(id, stats, new Owner(owner, owner), new Health(100, 100),
                    new Position(0, new Vector3f()), new CreatureSpells(List.of()),
                    new CreatureMeleeAttack(0, 10, 1, 1), new Threat(100), new Fearless());
            return id;
        }

        CreatureComponent stats() { return data.getComponent(actor, CreatureComponent.class); }

        void action(int value, boolean bound) {
            available = value;
            if (bound) data.setComponent(actor, new toniarts.openkeeper.game.component.Trigger(1));
            CreatureTriggerLogicController scripts = new CreatureTriggerLogicController(
                    null, info, () -> 0, null, creatures, null, data);
            try {
                scripts.processTick(0.25f);
            } finally {
                scripts.stop();
            }
        }
    }

    private interface Call { Object apply(String method, Object[] args); }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Call call) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (p, method, args) -> call.apply(method.getName(), args));
    }
}
