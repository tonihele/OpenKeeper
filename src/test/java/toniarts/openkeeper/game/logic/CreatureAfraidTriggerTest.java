/*
 * Copyright (C) 2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper. If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.game.logic;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import toniarts.openkeeper.game.event.ServerEventDispatcher;
import toniarts.openkeeper.game.event.ServerEvents;
import toniarts.openkeeper.game.event.ServerEvents.CreatureEvent;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Fearless;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Threat;
import toniarts.openkeeper.game.component.Trigger;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.CreatureController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.trigger.creature.CreatureTriggerLogicController;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureAfraidFixtures;
import toniarts.openkeeper.tools.convert.map.CreatureAngryFixtures;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import static toniarts.openkeeper.tools.convert.map.TriggerGeneric.TargetType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatureAfraidTriggerTest {

    @ParameterizedTest
    @CsvSource({"0,0", "9,0", "10,0", "11,1", "100,1"})
    void realFearDecisionDeliversOnlyWhenThreatExceedsFear(int enemyThreat, int events) {
        try (Fixture f = new Fixture(255, false)) {
            f.enemy(enemyThreat);
            f.controller.shouldFleeOrAttack();
            assertEquals(events, f.flags[0]);
            if (events != 0) assertEquals(CreatureState.FLEE, f.controller.getStateMachine().getCurrentState());
        }
    }

    @Test
    void callbackRunsBeforeEscapeAndDoesNotRequireSuccessfulNavigation() {
        try (Fixture f = new Fixture(255, false)) {
            f.enemy(20);
            assertNull(f.controller.getStateMachine().getCurrentState());
            assertTrue(f.controller.shouldFleeOrAttack());
            assertEquals(1, f.flags[0]);
            assertNull(f.stateAtEvent);
            assertEquals(CreatureState.FLEE, f.controller.getStateMachine().getCurrentState());
            // No Mobile or navigation service was supplied: fear still delivers the event.
        }
    }

    @Test
    void dungeonHeartEscapeSuppressionDoesNotSuppressTheFearEvent() {
        try (Fixture f = new Fixture(255, false)) {
            f.enemy(20);
            f.inHeart = true;
            assertFalse(f.controller.shouldFleeOrAttack());
            assertEquals(1, f.flags[0]);
            assertNull(f.controller.getStateMachine().getCurrentState());
        }
    }

    @Test
    void fearlessCreaturesDoNotDeliverTheEvent() {
        try (Fixture f = new Fixture(255, false)) {
            f.enemy(100);
            f.data.setComponent(f.entity, new Fearless());
            assertFalse(f.controller.shouldFleeOrAttack());
            assertEquals(0, f.flags[0]);
        }
    }

    @ParameterizedTest
    @CsvSource({"false,0", "true,1"})
    void currentFearRulesAccountForFellowFightersAndAlwaysFlee(boolean alwaysFlee, int events) {
        try (Fixture f = new Fixture(255, alwaysFlee)) {
            f.enemy(100);
            f.fellowFighter();
            f.controller.shouldFleeOrAttack();
            assertEquals(events, f.flags[0]);
        }
    }

    @ParameterizedTest
    @CsvSource({"1,1", "2,2", "255,3"})
    void repeatedQualifyingFearChecksRespectTheScriptRepeatLimit(int repeats, int events) {
        try (Fixture f = new Fixture(repeats, false)) {
            f.enemy(20);
            f.inHeart = true; // Fear qualifies again because no active escape is entered.
            for (int i = 0; i < 3; i++) f.controller.shouldFleeOrAttack();
            assertEquals(events, f.flags[0]);
        }
    }

    @Test
    void activeEscapeSuppressesFurtherFearEventsUntilTheStateChanges() {
        try (Fixture f = new Fixture(255, false)) {
            f.enemy(20);
            f.controller.shouldFleeOrAttack();
            for (int i = 0; i < 3; i++) f.controller.shouldFleeOrAttack();
            assertEquals(1, f.flags[0]);
            f.controller.getStateMachine().changeState(CreatureState.PICKED_UP);
            f.controller.shouldFleeOrAttack();
            assertEquals(2, f.flags[0]);
        }
    }

    @Test
    void ordinaryTicksDoNotPollFleeStateOrRepeatTheEvent() {
        try (Fixture f = new Fixture(255, false)) {
            f.enemy(20);
            f.controller.shouldFleeOrAttack();
            for (int i = 0; i < 12; i++) f.scripts.processTick(.25f);
            assertEquals(1, f.flags[0]);
        }
    }

    @Test
    void nestedConditionsAreEvaluatedAtEventTimeWithoutLatching() {
        try (Fixture f = new Fixture(255, false)) {
            f.records.put(1, CreatureAngryFixtures.condition(1, CREATURE_AFRAID, 2, 0, 255));
            f.records.put(2, CreatureAngryFixtures.condition(2, FLAG, -1, 0, 255));
            f.bindScripts();
            f.events.publishNow(ServerEvents.CREATURE_AFRAID, new CreatureEvent(f.entity));
            assertEquals(0, f.flags[0]);
            f.flags[1] = 1;
            for (int i = 0; i < 8; i++) f.scripts.processTick(.25f);
            assertEquals(0, f.flags[0]);
            f.events.publishNow(ServerEvents.CREATURE_AFRAID, new CreatureEvent(f.entity));
            assertEquals(1, f.flags[0]);
        }
    }

    @Test
    void allMatchingRootBranchesRunImmediately() {
        try (Fixture f = new Fixture(255, false)) {
            f.records.put(1, CreatureAngryFixtures.condition(1, CREATURE_AFRAID, -1, 2, 255));
            f.records.put(2, CreatureAngryFixtures.condition(2, CREATURE_AFRAID, -2, 0, 255));
            f.records.put(-2, CreatureAngryFixtures.action(-2, 2));
            f.bindScripts();
            f.events.publishNow(ServerEvents.CREATURE_AFRAID, new CreatureEvent(f.entity));
            assertEquals(1, f.flags[0]);
            assertEquals(1, f.flags[2]);
        }
    }

    @Test
    void nestedEventsAndOtherEventTypesDoNotMatch() {
        try (Fixture f = new Fixture(255, false)) {
            f.records.put(1, CreatureAngryFixtures.condition(1, FLAG, 2, 3, 255));
            f.records.put(2, CreatureAngryFixtures.condition(2, CREATURE_AFRAID, -1, 0, 255));
            f.records.put(3, CreatureAngryFixtures.condition(3, CREATURE_ANGRY, -2, 0, 255));
            f.records.put(-2, CreatureAngryFixtures.action(-2, 2));
            f.bindScripts();
            f.flags[1] = 1;
            f.events.publishNow(ServerEvents.CREATURE_AFRAID, new CreatureEvent(f.entity));
            for (int i = 0; i < 8; i++) f.scripts.processTick(.25f);
            assertEquals(0, f.flags[0]);
            assertEquals(0, f.flags[2]);
        }
    }

    @Test
    void fearEventDoesNotInterruptOrdinaryTraversal() {
        try (Fixture f = new Fixture(255, false)) {
            f.records.put(1, CreatureAngryFixtures.condition(1, FLAG, -2, 2, 255));
            f.records.put(2, CreatureAngryFixtures.condition(2, CREATURE_AFRAID, -1, 0, 255));
            f.records.put(-2, CreatureAngryFixtures.action(-2, 2));
            f.bindScripts();
            f.flags[1] = 1;
            f.scripts.processTick(.25f);
            f.events.publishNow(ServerEvents.CREATURE_AFRAID, new CreatureEvent(f.entity));
            assertEquals(1, f.flags[0]);
            assertEquals(0, f.flags[2]);
            f.scripts.processTick(.25f);
            assertEquals(1, f.flags[2]);
        }
    }

    @Test
    void currentBindingIsResolvedBeforeTheNextBindingTick() {
        try (Fixture f = new Fixture(255, false)) {
            f.data.removeComponent(f.entity, Trigger.class);
            f.events.publishNow(ServerEvents.CREATURE_AFRAID, new CreatureEvent(f.entity));
            assertEquals(0, f.flags[0]);
            f.data.setComponent(f.entity, new Trigger(99));
            f.events.publishNow(ServerEvents.CREATURE_AFRAID, new CreatureEvent(f.entity));
            assertEquals(0, f.flags[0]);
            f.data.setComponent(f.entity, new Trigger(1));
            f.events.publishNow(ServerEvents.CREATURE_AFRAID, new CreatureEvent(f.entity));
            assertEquals(1, f.flags[0]);
            f.data.removeComponent(f.entity, CreatureComponent.class);
            f.events.publishNow(ServerEvents.CREATURE_AFRAID, new CreatureEvent(f.entity));
            assertEquals(1, f.flags[0]);
        }
    }

    private static final class Fixture implements AutoCloseable {
        final EntityData data = new DefaultEntityData();
        final EntityId entity = data.createEntity();
        final int[] flags = new int[3];
        final Set<EntityId> sensed = new LinkedHashSet<>();
        final Map<Integer, toniarts.openkeeper.tools.convert.map.Trigger> records = new HashMap<>();
        final Keeper keeper = new Keeper(false, (short) 3);
        final Creature creature;
        final Creature fighter = CreatureAfraidFixtures.creature(false, true);
        final ILevelInfo level;
        final ICreaturesController creatures;
        CreatureController controller;
        final ServerEventDispatcher events = new ServerEventDispatcher();
        CreatureTriggerLogicController scripts;
        CreatureState stateAtEvent;
        boolean inHeart;

        Fixture(int repeats, boolean alwaysFlee) {
            creature = CreatureAfraidFixtures.creature(alwaysFlee, false);
            records.put(1, CreatureAngryFixtures.condition(1, CREATURE_AFRAID, -1, 0, repeats));
            records.put(-1, CreatureAngryFixtures.action(-1, 0));
            CreatureComponent component = new CreatureComponent();
            component.creatureId = 6;
            component.fear = 10;
            data.setComponents(entity, component, new Trigger(1), new Health(100,100),
                    new Owner((short) 3, (short) 3), new Threat(0),
                    new Position(0, new Vector3f()), new CreatureAi(0, CreatureState.FLEE, (short) 6));
            IKwdFile kwd = proxy(IKwdFile.class, (name,args) -> switch(name) {
                case "getTriggers" -> records;
                case "getCreature" -> ((Short)args[0]) == 7 ? fighter : creature;
                case "getGameLevel" -> CreatureSleepFixtures.level(4);
                case "getThings" -> args[0] == Thing.GoodCreature.class
                        ? List.of(CreatureAngryFixtures.thing(1)) : List.of();
                default -> throw new UnsupportedOperationException(name);
            });
            level = proxy(ILevelInfo.class, (name,args) -> switch(name) {
                case "getLevelData" -> kwd;
                case "getPlayer" -> keeper;
                case "getFlag" -> flags[((Number)args[0]).intValue()];
                case "setFlag" -> {
                    int id = ((Number)args[0]).intValue();
                    if (id == 0) stateAtEvent = controller.getStateMachine().getCurrentState();
                    flags[id] = (int)args[1];
                    yield null;
                }
                default -> throw new UnsupportedOperationException(name);
            });
            creatures = proxy(ICreaturesController.class, (name,args) -> switch(name) {
                case "createController" -> controller;
                case "isValidEntity" -> false;
                default -> throw new UnsupportedOperationException(name);
            });
            IRoomController heart = proxy(IRoomController.class, (name,args) -> switch(name) {
                case "isDungeonHeart" -> true;
                default -> throw new UnsupportedOperationException(name);
            });
            IMapController map = proxy(IMapController.class, (name,args) -> switch(name) {
                case "getRoomControllerByCoordinates" -> inHeart ? heart : null;
                default -> throw new UnsupportedOperationException(name);
            });
            IEntityPositionLookup lookup = proxy(IEntityPositionLookup.class, (name,args) -> switch(name) {
                case "getSensedEntities" -> sensed;
                default -> throw new UnsupportedOperationException(name);
            });
            var settings = Map.of(MiscType.CREATURE_CRITICAL_HEALTH_PERCENTAGE_OF_MAX,
                    CreatureSleepFixtures.variable(MiscType.CREATURE_CRITICAL_HEALTH_PERCENTAGE_OF_MAX, 10));
            controller = new CreatureController(entity, data, creature, null, null, () -> 0,
                    settings, creatures, lookup, map, level, null, null, null, events);
            bindScripts();
        }

        EntityId enemy(int threat) {
            EntityId enemy = data.createEntity();
            data.setComponents(enemy, new Owner((short) 4, (short) 4), new Health(100,100), new Threat(threat));
            sensed.add(enemy);
            return enemy;
        }

        void fellowFighter() {
            EntityId fellow = data.createEntity();
            CreatureComponent component = new CreatureComponent();
            component.creatureId = 7;
            data.setComponents(fellow, component, new Owner((short) 3, (short) 3), new Health(100,100), new Threat(1));
            sensed.add(fellow);
        }

        void bindScripts() {
            if (scripts != null) scripts.stop();
            scripts = new CreatureTriggerLogicController(null, level, null, null, creatures, null, data, events);
            events.start();
        }

        @Override
        public void close() {
            events.close();
            scripts.stop();
            data.close();
        }
    }

    @FunctionalInterface
    private interface Call { Object invoke(String name, Object[] args); }

    private static <T> T proxy(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (p,m,a) -> call.invoke(m.getName(),a)));
    }
}
