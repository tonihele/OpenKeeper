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

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureFall;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.CreatureRecuperating;
import toniarts.openkeeper.game.component.CreatureSleep;
import toniarts.openkeeper.game.component.Fearless;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.game.logic.CreatureFallSystem;
import toniarts.openkeeper.game.logic.CreatureMoodSystem;
import toniarts.openkeeper.game.logic.CreatureSleepSystem;
import toniarts.openkeeper.game.logic.HealthSystem;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.task.ITaskManager;
import toniarts.openkeeper.game.task.Task;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.game.task.creature.GoToSleep;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatureControllerTest {

    @Test
    void lowHealthWithoutDebtDoesNotAutonomouslyStartSleep() {
        try (Fixture f = new Fixture(9)) {
            f.controller.getStateMachine().changeState(CreatureState.IDLE);
            for (int i = 0; i < 12; i++) {
                f.tick();
            }
            assertEquals(250, f.health());
            assertEquals(0, f.sleep().sleepNeed);
            assertEquals(0, f.sleepAssignments);
            assertNull(f.data.getComponent(f.entity, CreatureRecuperating.class));
        }
    }

    @Test
    void fatigueInterruptsRoomWorkAndKeepsTheNewSleepTaskAssigned() {
        for (TaskType type : List.of(TaskType.TRAIN, TaskType.RESEARCH)) {
            try (Fixture f = new Fixture(9)) {
                f.startWork(type);
                f.data.setComponent(f.entity, new CreatureSleep(f.bed, -20, 0, 0));
                f.tick();
                assertEquals(36, f.sleep().sleepNeed);
                assertEquals(TaskType.GO_TO_SLEEP, f.controller.getAssignedTask().getTaskType());
                assertEquals(CreatureState.WORK, f.state());
                assertEquals(1, f.oldJobUnassignments);
                assertEquals(1, f.controller.getAssignedTask().getAssigneeCount());
                f.tick();
                assertEquals(CreatureState.SLEEPING, f.state());
            }
        }
    }

    @Test
    void fatigueDoesNotInterruptEatingTravelOrLairClaiming() {
        for (TaskType type : List.of(TaskType.GO_TO_EAT, TaskType.CLAIM_LAIR, TaskType.GO_TO_SLEEP)) {
            try (Fixture f = new Fixture(9)) {
                f.startWork(type);
                f.data.setComponent(f.entity, new CreatureSleep(f.bed, 0, 0, 36));
                f.controller.getStateMachine().update();
                assertEquals(type, f.controller.getAssignedTask().getTaskType());
                assertEquals(0, f.sleepAssignments);
            }
        }
    }

    @Test
    void fullHealthStillRequiresDebtCompletionAndNextFatigueCountsFromWake() {
        try (Fixture f = new Fixture(9)) {
            f.data.setComponent(f.entity, new CreatureSleep(f.bed, -20, 0, 36));
            f.sleepNow();
            for (int i = 0; i < 32; i++) {
                f.tick();
            }
            assertEquals(1000, f.health());
            assertEquals(CreatureState.SLEEPING, f.state());
            assertEquals(4, f.sleep().sleepNeed);
            for (int i = 0; i < 4; i++) {
                f.tick();
            }
            assertEquals(0, f.sleep().sleepNeed);
            assertEquals(CreatureState.IDLE, f.state());
            assertEquals(f.time, f.sleep().lastSleepTime);
            double wakeTime = f.time;
            for (int i = 0; i < 79; i++) {
                f.tick();
            }
            assertEquals(0, f.sleep().sleepNeed);
            assertEquals(wakeTime, f.sleep().lastSleepTime);
            f.tick();
            assertEquals(36, f.sleep().sleepNeed);
        }
    }

    @Test
    void manualLairDropWaitsForGetUpThenHealsWithoutDebt() {
        try (Fixture f = new Fixture(9)) {
            f.data.setComponent(f.entity, new CreatureAi(0, CreatureState.FALLEN, (short) 6, CreatureState.GO_TO_SLEEP));
            f.data.setComponent(f.entity, new Position(0, new Vector3f(3, WorldUtils.FLOOR_HEIGHT, 4)));
            f.data.setComponent(f.entity, new CreatureFall());
            f.fall.processTick(0);
            assertNull(f.data.getComponent(f.entity, CreatureFall.class));
            assertEquals(List.of(GameEventType.CREATURE_LAND), f.events);
            assertEquals(CreatureState.GO_TO_SLEEP, f.data.getComponent(f.entity, CreatureAi.class).getTargetState());
            f.tick();
            assertEquals(CreatureState.GETTING_UP, f.state());
            assertEquals(CreatureState.GO_TO_SLEEP, f.data.getComponent(f.entity, CreatureAi.class).getTargetState());
            // Rebuild from entity state while getting up, as after loading a save.
            f.controller = f.controllers.get();
            f.tick();
            assertEquals(CreatureState.WORK, f.state());
            assertEquals(TaskType.GO_TO_SLEEP, f.controller.getAssignedTask().getTaskType());
            assertNull(f.data.getComponent(f.entity, CreatureAi.class).getTargetState());
            f.tick();
            assertEquals(CreatureState.SLEEPING, f.state());
            assertEquals(0, f.sleep().sleepNeed);
            while (f.health() < 1000) {
                assertEquals(CreatureState.SLEEPING, f.state());
                f.tick();
                assertEquals(0, f.sleep().sleepNeed);
            }
            assertEquals(CreatureState.IDLE, f.state());
            assertNull(f.data.getComponent(f.entity, CreatureRecuperating.class));
            CreatureMood mood = f.data.getComponent(f.entity, CreatureMood.class);
            assertEquals(100, mood.general);
            assertEquals(200, mood.noFood);
            assertEquals(0, mood.noLair);
            assertEquals(400, mood.noWork);
            assertEquals(500, mood.noPay);
            assertEquals(0, mood.other);
        }
    }

    @Test
    void airborneTargetWaitsForLandingWithoutRunningAiOrAccumulatingFatigue() {
        try (Fixture f = new Fixture(9)) {
            f.time = 50;
            f.data.setComponents(f.entity,
                    new CreatureAi(0, CreatureState.FALLEN, (short) 6, CreatureState.GO_TO_SLEEP),
                    new Position(0, new Vector3f(3, WorldUtils.FLOOR_HEIGHT + 1, 4)),
                    new CreatureFall());
            f.fall.processTick(.25f);
            f.tick();
            assertNotNull(f.data.getComponent(f.entity, CreatureFall.class));
            assertTrue(f.events.isEmpty());
            assertNull(f.state());
            assertEquals(0, f.sleepAssignments);
            assertEquals(0, f.sleep().sleepNeed);
            assertNull(f.data.getComponent(f.entity, TaskComponent.class));
            assertEquals(250, f.health());
            assertEquals(CreatureState.GO_TO_SLEEP, f.data.getComponent(f.entity, CreatureAi.class).getTargetState());
            // A save/load while airborne must keep the target and delay entry as well.
            f.controller = f.controllers.get();
            f.tick();
            assertNull(f.state());
            assertEquals(0, f.sleep().sleepNeed);
            f.fall.processTick(.25f);
            assertNull(f.data.getComponent(f.entity, CreatureFall.class));
            f.tick();
            assertEquals(CreatureState.GETTING_UP, f.state());
            assertEquals(0, f.sleepAssignments);
            f.tick();
            assertEquals(CreatureState.WORK, f.state());
            assertEquals(1, f.sleepAssignments);
            assertNull(f.data.getComponent(f.entity, CreatureAi.class).getTargetState());
        }
    }

    @Test
    void ordinaryLandingWithoutAiDoesNotRequestManualRest() {
        try (Fixture f = new Fixture(9)) {
            f.data.removeComponent(f.entity, CreatureAi.class);
            f.data.setComponent(f.entity, new CreatureFall());
            f.fall.processTick(0);
            assertEquals(CreatureState.FALLEN, f.data.getComponent(f.entity, CreatureAi.class).getCreatureState());
            assertNull(f.data.getComponent(f.entity, CreatureAi.class).getTargetState());
            f.tick();
            f.tick();
            assertEquals(CreatureState.IDLE, f.state());
            assertEquals(0, f.sleepAssignments);
        }
    }

    @Test
    void landingWithoutStunUsesTargetBeforeEnteringIdle() {
        try (Fixture f = new Fixture(9)) {
            f.data.getComponent(f.entity, CreatureComponent.class).stunDuration = 0;
            f.data.setComponent(f.entity, new CreatureAi(0, CreatureState.FALLEN, (short) 6, CreatureState.GO_TO_SLEEP));
            f.data.setComponent(f.entity, new CreatureFall());
            f.fall.processTick(0);
            assertEquals(CreatureState.IDLE, f.data.getComponent(f.entity, CreatureAi.class).getCreatureState());
            assertEquals(CreatureState.GO_TO_SLEEP, f.data.getComponent(f.entity, CreatureAi.class).getTargetState());
            f.tick();
            assertEquals(CreatureState.SLEEPING, f.state());
            assertEquals(1, f.sleepAssignments);
            assertNull(f.data.getComponent(f.entity, CreatureAi.class).getTargetState());
        }
    }

    @Test
    void rejectedTargetFallsBackToIdleWithoutRetryingRequest() {
        try (Fixture f = new Fixture(9)) {
            f.data.setComponent(f.bed, new Stored(f.data.createEntity(), ObjectType.LAIR));
            f.data.setComponent(f.entity, new CreatureAi(0, CreatureState.IDLE, (short) 6, CreatureState.GO_TO_SLEEP));
            f.tick();
            assertEquals(CreatureState.IDLE, f.state());
            assertNull(f.data.getComponent(f.entity, CreatureAi.class).getTargetState());
            assertEquals(0, f.sleepAssignments);
        }
    }

    @Test
    void manualRequestRejectsAnotherRoomRemovedBedOrNonSleeper() {
        for (int scenario = 0; scenario < 3; scenario++) {
            try (Fixture f = new Fixture(scenario == 2 ? 0 : 9)) {
                if (scenario == 0) {
                    f.data.setComponent(f.bed, new Stored(f.data.createEntity(), ObjectType.LAIR));
                }
                if (scenario == 1) {
                    f.data.removeEntity(f.bed);
                }
                assertFalse(f.controller.goToSleepAfterLairDrop());
                assertEquals(0, f.sleepAssignments);
            }
        }
    }

    @Test
    void recoveryStartsOnTheNextGlobalSecondAndPreservesBothClocks() {
        try (Fixture f = new Fixture(9)) {
            f.tick();
            f.tick();
            f.tick();
            f.sleepNow(); // entry at .75, native first heal at 1.0
            f.tick();
            assertEquals(350, f.health());
            CreatureRecuperating recovery = f.data.getComponent(f.entity, CreatureRecuperating.class);
            assertEquals(1, recovery.healthCheckTime);
            assertEquals(1, recovery.moodCheckTime);
            assertEquals(50, f.data.getComponent(f.entity, CreatureMood.class).other);
            for (int i = 0; i < 4; i++) {
                f.tick();
            }
            assertEquals(450, f.health());
            recovery = f.data.getComponent(f.entity, CreatureRecuperating.class);
            assertEquals(2, recovery.healthCheckTime);
            assertEquals(2, recovery.moodCheckTime);
            assertEquals(0, f.data.getComponent(f.entity, CreatureMood.class).other);
        }
    }

    @Test
    void needsClockPausesDuringEatingCaptivityAndActiveLairClaiming() {
        try (Fixture f = new Fixture(9)) {
            f.time = 50;
            for (CreatureState state : List.of(CreatureState.EATING, CreatureState.IMPRISONED,
                    CreatureState.TORTURED, CreatureState.UNCONSCIOUS)) {
                f.data.setComponent(f.entity, new CreatureAi(0, state, (short) 6));
                f.sleepSystem.processTick(.25f);
                assertEquals(0, f.sleep().sleepNeed);
                assertEquals(0, f.sleep().lastSleepTime);
            }
            f.data.setComponent(f.entity, new CreatureAi(0, CreatureState.WORK, (short) 6));
            f.data.setComponent(f.entity, new TaskComponent(-1, null, null, TaskType.CLAIM_LAIR, 10, true));
            f.sleepSystem.processTick(.25f);
            assertEquals(0, f.sleep().sleepNeed);
            f.data.setComponent(f.entity, new TaskComponent(-1, null, null, TaskType.TRAIN, 10, true));
            f.sleepSystem.processTick(.25f);
            assertEquals(36, f.sleep().sleepNeed);
        }
    }

    @Test
    void healingAndMoodUseTheSameClockWhenHealthSystemRunsAhead() {
        try (Fixture f = new Fixture(9)) {
            f.time = .75;
            f.sleepNow();
            f.healthSystem.processTick(1); // Its internal timer is ahead of the game clock.
            f.moodSystem.processTick(.25f);
            assertEquals(250, f.health());
            assertEquals(600, f.data.getComponent(f.entity, CreatureMood.class).other);
            f.time = 1;
            f.moodSystem.processTick(.25f);
            f.healthSystem.processTick(.25f);
            assertEquals(350, f.health());
            assertEquals(50, f.data.getComponent(f.entity, CreatureMood.class).other);
        }
    }

    @Test
    void unreachableBedRetriesAtMostOncePerSecond() {
        try (Fixture f = new Fixture(9)) {
            f.reachable.set(false);
            assertFalse(f.controller.goToSleep());
            assertEquals(0, f.sleep().lastSleepAttemptTime);
            f.controller.setCreatureLair(f.bed);
            f.time = .75;
            // Recreating the controller must not reset the entity's retry gate.
            assertFalse(f.controllers.get().goToSleep());
            assertEquals(1, f.sleepAssignments);
            f.time = 1;
            assertFalse(f.controller.goToSleep());
            assertEquals(2, f.sleepAssignments);
        }
    }

    private static final class Fixture implements AutoCloseable {
        final EntityData data = new DefaultEntityData();
        final EntityId entity = data.createEntity();
        final EntityId bed = data.createEntity();
        final EntityId room = data.createEntity();
        final Map<Long, Task> tasks = new HashMap<>();
        final AtomicBoolean reachable = new AtomicBoolean(true);
        CreatureController controller;
        final java.util.function.Supplier<CreatureController> controllers;
        final CreatureSleepSystem sleepSystem;
        final CreatureMoodSystem moodSystem;
        final HealthSystem healthSystem;
        final CreatureFallSystem fall;
        final List<GameEventType> events = new java.util.ArrayList<>();
        final GameEventService eventService = proxy(GameEventService.class, (method, args) -> {
            events.add((GameEventType) args[0]);
            return null;
        });
        int sleepAssignments;
        int oldJobUnassignments;
        double time;

        Fixture(int timeSleep) {
            fall = new CreatureFallSystem(data, () -> time, eventService);
            Creature definition = CreatureSleepFixtures.sleeper(timeSleep);
            CreatureComponent component = new CreatureComponent();
            component.creatureId = 6;
            component.stunDuration = .1f;
            data.setComponents(entity, component, new Health(250, 1000), new Owner((short) 3, (short) 3),
                    new Position(0, new Vector3f(3, WorldUtils.FLOOR_HEIGHT, 4)),
                    new CreatureSleep(bed, 0, 0, 0), new CreatureMood(100, 200, 300, 400, 500, 600),
                    new CreatureAi(0, CreatureState.IDLE, (short) 6), new Gold(0, 100), new Fearless());
            data.setComponents(bed, new Position(0, new Vector3f(3, WorldUtils.FLOOR_HEIGHT, 4)),
                    new Stored(room, ObjectType.LAIR));
            IRoomController lair = proxy(IRoomController.class, (method, args) -> switch (method) {
                case "getEntityId" -> room;
                case "getOwnerId" -> (short) 3;
                case "isDestroyed", "isRemoved" -> false;
                case "hasObjectControl" -> args[0] == ObjectType.LAIR;
                default -> throw new UnsupportedOperationException(method);
            });
            IMapController map = proxy(IMapController.class, (method, args) -> switch (method) {
                case "getRoomControllerByCoordinates" -> lair;
                default -> throw new UnsupportedOperationException(method);
            });
            ITaskManager taskManager = proxy(ITaskManager.class, (method, args) -> switch (method) {
                case "getTaskById" -> tasks.get((Long) args[0]);
                case "assignSleepTask" -> {
                    sleepAssignments++;
                    if (!reachable.get()) {
                        yield false;
                    }
                    GoToSleep task = new GoToSleep(null, map, (ICreatureController) args[0]);
                    tasks.put(task.getId(), task);
                    task.assign((ICreatureController) args[0], true);
                    yield true;
                }
                default -> throw new UnsupportedOperationException(method);
            });
            Map<MiscType, MiscVariable> settings = new EnumMap<>(MiscType.class);
            settings.put(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS,
                    CreatureSleepFixtures.variable(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, 60));
            settings.put(MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND,
                    CreatureSleepFixtures.variable(MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, -3));
            settings.put(MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND,
                    CreatureSleepFixtures.variable(MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND, 100));
            settings.put(MiscType.MODIFY_ANGER_OF_CREATURE_IN_LAIR_PER_SECOND,
                    CreatureSleepFixtures.variable(MiscType.MODIFY_ANGER_OF_CREATURE_IN_LAIR_PER_SECOND, -550));
            IEntityPositionLookup lookup = proxy(IEntityPositionLookup.class, (method, args) -> switch (method) {
                case "getSensedEntities" -> Set.of();
                default -> throw new UnsupportedOperationException(method);
            });
            controllers = () -> new CreatureController(entity, data, definition, null, taskManager, () -> time,
                    settings, null, lookup, map, null, null, null, eventService);
            controller = controllers.get();
            IKwdFile kwd = proxy(IKwdFile.class, (method, args) -> switch (method) {
                case "getCreature" -> definition;
                case "getGameLevel" -> CreatureSleepFixtures.level(4);
                case "getRoomById" -> new Room();
                default -> throw new UnsupportedOperationException(method);
            });
            sleepSystem = new CreatureSleepSystem(data, kwd, () -> time);
            moodSystem = new CreatureMoodSystem(data, kwd, List.of(), () -> time, settings);
            healthSystem = new HealthSystem(data, null, settings, null, null, List.of(), map, () -> time, eventService);
        }

        void startWork(TaskType type) {
            Task job = proxy(Task.class, (method, args) -> switch (method) {
                case "getId" -> -1L;
                case "getTaskType" -> type;
                case "isValid" -> true;
                case "getTarget" -> new Vector2f(3, 4);
                case "executeTask" -> null;
                case "unassign" -> { oldJobUnassignments++; yield null; }
                default -> throw new UnsupportedOperationException(method);
            });
            tasks.put(-1L, job);
            data.setComponent(entity, new TaskComponent(-1, null, null, type, 0, false));
            data.setComponent(entity, new CreatureAi(0, CreatureState.WORK, (short) 6));
            controller.getStateMachine().changeState(CreatureState.WORK);
        }

        void sleepNow() {
            controller.sleep();
            controller.processTick(0); // Publish the transition as the normal AI tick does.
        }

        void tick() {
            time += .25;
            sleepSystem.processTick(.25f);
            moodSystem.processTick(.25f);
            healthSystem.processTick(.25f);
            controller.processTick(.25f);
        }

        int health() { return data.getComponent(entity, Health.class).health; }
        CreatureSleep sleep() { return data.getComponent(entity, CreatureSleep.class); }
        CreatureState state() { return controller.getStateMachine().getCurrentState(); }

        @Override public void close() {
            sleepSystem.stop();
            moodSystem.stop();
            healthSystem.stop();
            fall.stop();
            data.close();
        }
    }

    @FunctionalInterface
    private interface Invocation { Object call(String method, Object[] args); }

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
