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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import toniarts.openkeeper.game.event.ServerEventDispatcher;
import toniarts.openkeeper.game.event.ServerEvents;
import toniarts.openkeeper.game.event.ServerEvents.CreatureEvent;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.CreatureSleep;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Slapped;
import toniarts.openkeeper.game.component.Trigger;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.player.PlayerRoomControl;
import toniarts.openkeeper.game.controller.player.PlayerStatsControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.trigger.creature.CreatureTriggerLogicController;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureAngryFixtures;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import static toniarts.openkeeper.tools.convert.map.TriggerGeneric.TargetType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CreatureAngryTriggerTest {

    @ParameterizedTest
    @CsvSource({
        "0,16383,1,0", "16383,32767,1,0", "32767,32768,1,1",
        "0,32768,32768,1", "32768,40000,1,0", "40000,32767,-1,0",
        "40000,0,-1,0", "0,32768,0,0", "0,32768,-1,0"
    })
    void deliversOnlyPositiveTransitionsIntoFullAnger(int before, int after, int change, int events) {
        try (Fixture f = new Fixture(255, false)) {
            CreatureMood previous = new CreatureMood(before, 0, 0, 0, 0, 0);
            CreatureMood updated = new CreatureMood(after, 0, 0, 0, 0, 0);
            var observed = new java.util.concurrent.atomic.AtomicReference<Integer>();
            f.events.subscribe(ServerEvents.CREATURE_ANGRY, (type, event) ->
                    observed.set(f.data.getComponent(event.creature(), CreatureMood.class).general));
            CreatureMoodUpdates.set(f.data, f.entity, f.creature, previous, updated, change, f.events);
            assertEquals(events == 0 ? null : Integer.valueOf(after), observed.get());
            assertEquals(events, f.flags[0]);
        }
    }

    @ParameterizedTest
    @CsvSource({"1,1", "2,2", "255,3"})
    void recoveryAllowsAnotherEventSubjectToRepeatLimits(int repeats, int expected) {
        try (Fixture f = new Fixture(repeats, false)) {
            for (int i = 0; i < 3; i++) {
                f.data.setComponent(f.entity, new CreatureMood(32767, 0, 0, 0, 0, 0));
                f.change(1);
                f.change(1); // Remaining angry must not repeat the event.
                f.scripts.processTick(.25f);
            }
            assertEquals(expected, f.flags[0]);
        }
    }

    @Test
    void startingAngryDoesNotFireAndOrdinaryTicksNeverPollTheEventAsAState() {
        try (Fixture f = new Fixture(255, false)) {
            f.data.setComponent(f.entity, new CreatureMood(40000, 0, 0, 0, 0, 0));
            for (int i = 0; i < 12; i++) {
                f.scripts.processTick(.25f);
            }
            f.change(1);
            assertEquals(0, f.flags[0]);
        }
    }

    @Test
    void nestedConditionsAreCheckedAtEventTimeWithoutLatchingFailedEvents() {
        try (Fixture f = new Fixture(255, false, true)) {
            f.change(1);
            assertEquals(0, f.flags[0]);
            f.flags[1] = 1;
            for (int i = 0; i < 8; i++) {
                f.scripts.processTick(.25f);
            }
            assertEquals(0, f.flags[0]);
            f.change(-1);
            f.change(1);
            assertEquals(1, f.flags[0]);
        }
    }

    @Test
    void dispatchRunsAllMatchingRootBranches() {
        try (Fixture f = new Fixture(255, false)) {
            f.records.put(1, CreatureAngryFixtures.condition(1, CREATURE_ANGRY, -1, 2, 255));
            f.records.put(2, CreatureAngryFixtures.condition(2, CREATURE_ANGRY, -2, 0, 255));
            f.records.put(-2, CreatureAngryFixtures.action(-2, 2));
            f.bindScripts();
            f.change(1);
            assertEquals(1, f.flags[0]);
            assertEquals(1, f.flags[2]);
        }
    }

    @Test
    void eventDoesNotInterruptTheOrdinaryTriggerTraversal() {
        try (Fixture f = new Fixture(255, false)) {
            f.records.put(1, CreatureAngryFixtures.condition(1, FLAG, -2, 2, 255));
            f.records.put(2, CreatureAngryFixtures.condition(2, CREATURE_ANGRY, -1, 0, 255));
            f.records.put(-2, CreatureAngryFixtures.action(-2, 2));
            f.bindScripts();
            f.flags[1] = 1;
            f.scripts.processTick(.25f); // Ordinary condition selected, action still pending.
            f.change(1);
            assertEquals(1, f.flags[0]);
            assertEquals(0, f.flags[2]);
            f.scripts.processTick(.25f);
            assertEquals(1, f.flags[2]);
            assertEquals(1, f.flags[0]);
        }
    }

    @Test
    void eventsDoNotMatchNestedAngerNodesOrOtherCreatureEvents() {
        try (Fixture f = new Fixture(255, false)) {
            f.records.put(1, CreatureAngryFixtures.condition(1, FLAG, 2, 3, 255));
            f.records.put(2, CreatureAngryFixtures.condition(2, CREATURE_ANGRY, -1, 0, 255));
            f.records.put(3, CreatureAngryFixtures.condition(3, CREATURE_AFRAID, -2, 0, 255));
            f.records.put(-2, CreatureAngryFixtures.action(-2, 2));
            f.bindScripts();
            f.flags[1] = 1;
            f.change(1);
            for (int i = 0; i < 8; i++) {
                f.scripts.processTick(.25f);
            }
            assertEquals(0, f.flags[0]);
            assertEquals(0, f.flags[2]);
        }
    }

    @Test
    void aNewlySpawnedCreatureCanFireBeforeTheNextBindingTick() {
        try (Fixture f = new Fixture(255, false)) {
            var health = CreatureAngryFixtures.healthCondition(2, -1, 50);
            f.records.put(1, CreatureAngryFixtures.condition(1, CREATURE_ANGRY, 2, 0, 255));
            f.records.put(2, health);
            f.bindScripts();
            f.change(1); // Existing creature is at 100%, so its nested condition fails.
            assertEquals(0, f.flags[0]);
            EntityId spawned = f.data.createEntity();
            f.data.setComponents(spawned, new CreatureComponent(), new Trigger(1), new Health(50, 100));
            f.events.publishNow(ServerEvents.CREATURE_ANGRY, new CreatureEvent(spawned));
            assertEquals(1, f.flags[0]);
        }
    }

    @Test
    void entitiesWithoutRegisteredScriptsAreIgnored() {
        try (Fixture f = new Fixture(255, false)) {
            EntityId other = f.data.createEntity();
            f.events.publishNow(ServerEvents.CREATURE_ANGRY, new CreatureEvent(other));
            f.data.setComponent(other, new Trigger(999));
            f.events.publishNow(ServerEvents.CREATURE_ANGRY, new CreatureEvent(other));
            f.data.setComponent(other, new CreatureComponent());
            f.events.publishNow(ServerEvents.CREATURE_ANGRY, new CreatureEvent(other));
            assertEquals(0, f.flags[0]);
        }
    }

    @Test
    void emotionlessCreaturesDoNotChangeMoodOrEmitAnEvent() {
        try (Fixture f = new Fixture(255, true)) {
            f.change(1);
            assertEquals(32767, f.data.getComponent(f.entity, CreatureMood.class).general);
            assertEquals(0, f.flags[0]);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void noLairMoodUpdatesDeliverTheEventImmediatelyUnlessEmotionless(boolean emotionless) {
        try (Fixture f = new Fixture(255, emotionless)) {
            f.data.setComponent(f.entity, new CreatureMood(0, 0, 32767, 0, 0, 0));
            CreatureMoodSystem moods = new CreatureMoodSystem(f.data, f.kwd, List.of(f.player),
                    () -> 0, f.settings, f.events);
            moods.processTick(.25f);
            assertEquals(emotionless ? 0 : 1, f.flags[0]);
            moods.processTick(.25f);
            assertEquals(emotionless ? 0 : 1, f.flags[0]);
            moods.stop();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void slapMoodUpdatesDeliverTheEventBeforeAnotherTriggerTick(boolean emotionless) {
        try (Fixture f = new Fixture(255, emotionless)) {
            f.data.setComponent(f.entity, new CreatureMood(0, 0, 0, 0, 0, 32767));
            f.data.setComponent(f.entity, new Slapped(0));
            SlapSystem slaps = new SlapSystem(f.data, f.kwd, List.of(f.player), f.settings,
                    f.events);
            assertEquals(emotionless ? 0 : 1, f.flags[0]);
            slaps.stop();
        }
    }

    private static final class Fixture implements AutoCloseable {
        final EntityData data = new DefaultEntityData();
        final EntityId entity = data.createEntity();
        final int[] flags = new int[3];
        final Creature creature;
        final Map<Integer, toniarts.openkeeper.tools.convert.map.Trigger> records = new HashMap<>();
        final Room lair = RoomFixtures.room((short) 2, 0, Room.RoomFlag.BUILDABLE);
        final Keeper keeper = new Keeper(false, (short) 3);
        final PlayerRoomControl rooms = new PlayerRoomControl(keeper, List.of(lair));
        final PlayerStatsControl stats = new PlayerStatsControl();
        final Map<MiscType, MiscVariable> settings = Map.of(
                MiscType.MODIFY_ANGER_OF_CREATURE_IN_LAIR_PER_SECOND,
                CreatureSleepFixtures.variable(MiscType.MODIFY_ANGER_OF_CREATURE_IN_LAIR_PER_SECOND, -550),
                MiscType.INCREASED_WORK_RATE_DURATION_FROM_SLAPPING_SECONDS,
                CreatureSleepFixtures.variable(MiscType.INCREASED_WORK_RATE_DURATION_FROM_SLAPPING_SECONDS, 1));
        final IKwdFile kwd;
        final ILevelInfo level;
        final IPlayerController player;
        final ICreaturesController creatures;
        final ServerEventDispatcher events = new ServerEventDispatcher();
        CreatureTriggerLogicController scripts;

        Fixture(int repeats, boolean emotionless) {
            this(repeats, emotionless, false);
        }

        Fixture(int repeats, boolean emotionless, boolean nested) {
            creature = CreatureAngryFixtures.creature(emotionless);
            records.put(1, CreatureAngryFixtures.condition(1, CREATURE_ANGRY, nested ? 2 : -1, 0, repeats));
            if (nested) {
                records.put(2, CreatureAngryFixtures.condition(2, FLAG, -1, 0, 255));
            }
            records.put(-1, CreatureAngryFixtures.action(-1, 0));
            CreatureComponent component = new CreatureComponent();
            component.creatureId = 6;
            data.setComponents(entity, component, new Trigger(1), new Health(100, 100),
                    new Owner((short) 3, (short) 3), new CreatureMood(32767, 0, 0, 0, 0, 0),
                    new CreatureSleep());
            rooms.setTypeAvailable(lair, true);
            kwd = proxy(IKwdFile.class, (name, args) -> switch (name) {
                case "getTriggers" -> records;
                case "getCreature" -> creature;
                case "getRoomById" -> lair;
                case "getGameLevel" -> CreatureSleepFixtures.level(4);
                case "getThings" -> args[0] == Thing.GoodCreature.class
                        ? List.of(CreatureAngryFixtures.thing(1)) : List.of();
                default -> throw new UnsupportedOperationException(name);
            });
            level = proxy(ILevelInfo.class, (name, args) -> switch (name) {
                case "getLevelData" -> kwd;
                case "getFlag" -> flags[((Number) args[0]).intValue()];
                case "setFlag" -> { flags[((Number) args[0]).intValue()] = (int) args[1]; yield null; }
                default -> throw new UnsupportedOperationException(name);
            });
            player = proxy(IPlayerController.class, (name, args) -> switch (name) {
                case "getKeeper" -> keeper;
                case "getRoomControl" -> rooms;
                case "getStatsControl" -> stats;
                default -> throw new UnsupportedOperationException(name);
            });
            creatures = proxy(ICreaturesController.class, (name, args) -> switch (name) {
                case "createController" -> proxy(ICreatureController.class, (method, a) -> switch (method) {
                    case "getEntityId" -> args[0];
                    case "getHealth" -> data.getComponent((EntityId) args[0], Health.class).health;
                    case "getMaxHealth" -> data.getComponent((EntityId) args[0], Health.class).maxHealth;
                    default -> throw new UnsupportedOperationException(method);
                });
                default -> throw new UnsupportedOperationException(name);
            });
            bindScripts();
        }

        void bindScripts() {
            if (scripts != null) {
                scripts.stop();
            }
            scripts = new CreatureTriggerLogicController(null, level, null, null, creatures, null, data, events);
            events.start();
        }

        void change(int delta) {
            CreatureMood previous = data.getComponent(entity, CreatureMood.class);
            CreatureMoodUpdates.set(data, entity, creature, previous, previous.add(0, delta), delta,
                    events);
        }

        @Override
        public void close() {
            events.close();
            scripts.stop();
            data.close();
        }
    }

    @FunctionalInterface
    private interface Call {
        Object invoke(String name, Object[] args);
    }

    private static <T> T proxy(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (p, method, args) -> call.invoke(method.getName(), args)));
    }
}
