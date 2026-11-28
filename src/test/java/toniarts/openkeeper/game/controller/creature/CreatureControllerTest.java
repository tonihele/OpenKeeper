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

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.math.Vector2;
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
import toniarts.openkeeper.game.component.CreatureHunger;
import toniarts.openkeeper.game.component.CreatureMeleeAttack;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.CreatureRecuperating;
import toniarts.openkeeper.game.component.CreatureSleep;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Fearless;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.MeleeHit;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.Objective;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Slapped;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.component.Threat;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.ObjectsController;
import toniarts.openkeeper.game.controller.entity.IEntityController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.logic.CreatureAiSystem;
import toniarts.openkeeper.game.logic.CreatureFallSystem;
import toniarts.openkeeper.game.logic.CreatureLandingSystem;
import toniarts.openkeeper.game.logic.CreatureMoodSystem;
import toniarts.openkeeper.game.logic.CreatureSleepSystem;
import toniarts.openkeeper.game.logic.HealthSystem;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.ITaskManager;
import toniarts.openkeeper.game.task.Task;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.game.task.creature.GoToSleep;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
    void eatingAChickenClearsOnlyTheNoFoodAnger() {
        try (Fixture f = new Fixture(9)) {
            f.data.setComponent(f.entity, new CreatureHunger(0, 2));
            f.controller.sate();
            CreatureMood mood = f.data.getComponent(f.entity, CreatureMood.class);
            assertEquals(0, mood.noFood);
            assertEquals(100, mood.general);
            assertEquals(600, mood.other);
            assertEquals(1, f.data.getComponent(f.entity, CreatureHunger.class).amountNeeded);
        }
    }

    @Test
    void fatigueInterruptsRoomWorkAndKeepsTheNewSleepTaskAssigned() {
        // The job groups DK II lets fatigue interrupt (mask 0x800A3C3B, 0x477C20)
        for (TaskType type : List.of(TaskType.RESEARCH, TaskType.MANUFACTURE, TaskType.GUARD, TaskType.PRAY, TaskType.TRAIN,
                TaskType.GAMBLE)) {
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
    void slapInterruptsAWorkersWorkOncePerSlap() {
        try (Fixture f = new Fixture(9)) {
            f.data.setComponent(f.entity, workerComponent());
            f.startWork(TaskType.TRAIN);

            // The slap starts between two ticks, as it does from the keeper's own thread
            f.data.setComponent(f.entity, new Slapped(.1));
            f.tick();
            assertEquals(CreatureState.IDLE, f.state());
            assertTrue(f.data.getComponent(f.entity, Slapped.class).reacted);
            assertEquals(.1, f.data.getComponent(f.entity, Slapped.class).startTime);

            // The same slap doesn't interrupt the next work
            f.startWork(TaskType.TRAIN);
            f.tick();
            assertEquals(CreatureState.WORK, f.state());

            // A new slap does
            f.data.setComponent(f.entity, new Slapped(.3));
            f.tick();
            assertEquals(CreatureState.IDLE, f.state());
        }
    }

    @Test
    void guardWithoutLairLooksForOneNowAndThenButOtherWorkKeepsGoing() {
        try (Fixture f = new Fixture(9)) {
            f.data.setComponent(f.entity, new CreatureSleep(null, 0, 0, 0));
            f.startWork(TaskType.GUARD);
            for (int i = 0; i < 19; i++) {
                f.tick();
            }
            assertEquals(0, f.lairSearches);
            f.tick();
            assertEquals(1, f.lairSearches);
            for (int i = 0; i < 19; i++) {
                f.tick();
            }
            assertEquals(1, f.lairSearches);
            f.tick();
            assertEquals(2, f.lairSearches);
            assertEquals(CreatureState.WORK, f.state());
        }
        try (Fixture f = new Fixture(9)) {
            f.data.setComponent(f.entity, new CreatureSleep(null, 0, 0, 0));
            f.startWork(TaskType.TRAIN);
            for (int i = 0; i < 40; i++) {
                f.tick();
            }
            assertEquals(0, f.lairSearches);
        }
    }

    @Test
    void workerPicksUpTheLooseGoldAtItsFeet() {
        try (Fixture f = new Fixture(9)) {
            f.data.setComponent(f.entity, workerComponent());
            f.startWork(TaskType.TRAIN);
            f.data.setComponent(f.entity, new Gold(1060, 1400));
            EntityId pile = f.goldPile(450);

            // A worker that counts as full (the level's margin short of what it carries) leaves it
            f.tick();
            assertEquals(450, f.data.getComponent(pile, Gold.class).gold);

            // Short of that, up to what it carries, the rest stays in the pile
            f.data.setComponent(f.entity, new Gold(1059, 1400));
            f.tick();
            assertEquals(1400, f.data.getComponent(f.entity, Gold.class).gold);
            assertEquals(109, f.data.getComponent(pile, Gold.class).gold);
        }
    }

    @Test
    void creatureLeavesTheLooseGoldInATreasuryAlone() {
        try (Fixture f = new Fixture(9)) {
            f.startWork(TaskType.TRAIN);
            EntityId pile = f.goldPile(60);
            IRoomController lair = f.roomHere;
            f.roomHere = proxy(IRoomController.class, (method, args) -> switch (method) {
                case "getEntityId" -> f.room;
                case "getOwnerId" -> (short) 3;
                case "isDestroyed", "isRemoved", "isDungeonHeart" -> false;
                case "hasObjectControl" -> args[0] == ObjectType.GOLD;
                default -> throw new UnsupportedOperationException(method);
            });
            f.tick();
            assertEquals(0, f.data.getComponent(f.entity, Gold.class).gold);

            // Anywhere else it takes it
            f.roomHere = lair;
            f.tick();
            assertEquals(60, f.data.getComponent(f.entity, Gold.class).gold);
            assertNull(f.data.getComponent(pile, Gold.class));
        }
    }

    @Test
    void slapDoesNotInterruptANonWorker() {
        try (Fixture f = new Fixture(9)) {
            f.startWork(TaskType.TRAIN);
            f.data.setComponent(f.entity, new Slapped(.1));
            f.tick();
            assertEquals(CreatureState.WORK, f.state());
            assertTrue(f.data.getComponent(f.entity, Slapped.class).reacted);
        }
    }

    @Test
    void creatureTakenToTheHandGivesUpItsTask() {
        try (Fixture f = new Fixture(9)) {
            f.startWork(TaskType.GUARD);
            CreatureAiSystem system = new CreatureAiSystem(f.data, proxy(ICreaturesController.class,
                    (method, args) -> f.controller), proxy(ITaskManager.class, (method, args) -> null));
            system.processTick(.25f);
            f.data.removeComponent(f.entity, CreatureAi.class);
            system.processTick(.25f);
            assertEquals(1, f.oldJobUnassignments);
            assertNull(f.data.getComponent(f.entity, TaskComponent.class));
            system.stop();
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
            f.landing.processTick(0);
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
            f.landing.processTick(.25f);
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
            // The animation loop only brings the creature down, the landing is left to the game logic
            assertNotNull(f.data.getComponent(f.entity, CreatureFall.class));
            assertTrue(f.events.isEmpty());
            assertEquals(0, f.data.getComponent(f.entity, CreatureAi.class).stateStartTime);
            f.landing.processTick(.25f);
            assertNull(f.data.getComponent(f.entity, CreatureFall.class));
            assertEquals(List.of(GameEventType.CREATURE_LAND), f.events);
            assertEquals(50.5, f.data.getComponent(f.entity, CreatureAi.class).stateStartTime);
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
            f.landing.processTick(0);
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
            f.landing.processTick(0);
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
    void sleeperLiesOnItsHiddenBedWithTheBedsFacingAndTheBedShowsAgainWhenItWakes() {
        try (Fixture f = new Fixture(9)) {
            f.data.setComponents(f.bed, new Position(1.5f, new Vector3f(3.25f, WorldUtils.FLOOR_HEIGHT, 4.5f)),
                    new ObjectViewState((short) 1, null, ObjectViewState.GameObjectAnimState.MESH_RESOURCE, true));
            f.data.setComponent(f.entity, new Position(0, new Vector3f(3.6f, WorldUtils.FLOOR_HEIGHT, 4.1f)));
            f.sleepNow();

            Position position = f.data.getComponent(f.entity, Position.class);
            assertEquals(1.5f, position.rotation);
            assertEquals(new Vector3f(3.25f, WorldUtils.FLOOR_HEIGHT, 4.5f), position.position);
            assertFalse(f.data.getComponent(f.bed, ObjectViewState.class).visible);

            for (int i = 0; i < 100 && f.state() == CreatureState.SLEEPING; i++) {
                f.tick();
            }
            assertEquals(CreatureState.IDLE, f.state());
            assertTrue(f.data.getComponent(f.bed, ObjectViewState.class).visible);
        }
    }

    @Test
    void heroBedStaysInSightWhileItsOwnerSleeps() {
        try (Fixture f = new Fixture(9)) {
            f.data.setComponent(f.bed, new ObjectViewState(ObjectsController.OBJECT_HERO_BED_ID, null,
                    ObjectViewState.GameObjectAnimState.MESH_RESOURCE, true));
            f.sleepNow();

            assertEquals(CreatureState.SLEEPING, f.state());
            assertTrue(f.data.getComponent(f.bed, ObjectViewState.class).visible);
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

    @Test
    void meleeHitLandsAfterTheShotDelayEvenIfTheTargetStepsAway() {
        try (Fixture f = new Fixture(9)) {
            CreatureSleepFixtures.setShotDelay(f.definition, .5f);
            EntityId target = f.meleeTarget();
            f.controller.executeAttack(target);
            assertEquals(CreatureState.MELEE_ATTACK, f.state());

            f.data.setComponent(target, new Position(0, new Vector3f(6, WorldUtils.FLOOR_HEIGHT, 4)));
            f.time = .25;
            f.controller.getStateMachine().update();
            assertNull(f.data.getComponent(target, Damage.class));
            assertEquals(CreatureState.MELEE_ATTACK, f.state());

            f.time = .5;
            f.controller.getStateMachine().update();
            assertEquals(30, f.data.getComponent(target, Damage.class).damage);
            assertTrue(f.events.contains(GameEventType.MELEE_HIT));
            assertEquals(CreatureState.FIGHT, f.state());
        }
    }

    @Test
    void fleesInAnEnemyOrRuinedHeartButStandsInItsOwn() {
        assertEquals(CreatureState.FLEE, stateWhenThreatenedInHeart((short) 4, false));
        assertEquals(CreatureState.FLEE, stateWhenThreatenedInHeart((short) 3, true));
        assertNotEquals(CreatureState.FLEE, stateWhenThreatenedInHeart((short) 3, false));
    }

    @Test
    void workerNearItsOwnHeartOnlyFightsAnEnemyInReach() {
        assertNull(workerStateWithHeroAt(6, 3, 1)); // Neither fights nor flees, keeps working
        assertEquals(CreatureState.FIGHT, workerStateWithHeroAt(3.5f, 3, 1));

        // 3 tiles from the heart's centre is still near it, and the reach is a tile whatever the melee range
        assertEquals(CreatureState.FIGHT, workerStateWithHeroAt(4, 6, .3f));

        // 4 tiles away it isn't
        assertEquals(CreatureState.FLEE, workerStateWithHeroAt(3.5f, 7, 1));
    }

    private static CreatureState workerStateWithHeroAt(float x, int heartX, float meleeRange) {
        try (Fixture f = new Fixture(CreatureSleepFixtures.worker())) {
            EntityId hero = f.data.createEntity();
            f.data.setComponents(hero, new Owner((short) 1, (short) 1), new Health(1000, 1000), new Threat(100),
                    new Position(0, new Vector3f(x, WorldUtils.FLOOR_HEIGHT, 4)));
            f.data.setComponent(f.entity, new CreatureMeleeAttack(0, 30, 1.2f, meleeRange));
            f.data.removeComponent(f.entity, Fearless.class);
            f.sensed = Set.of(hero);
            f.keeper.setDungeonHeartLocation(new Point(heartX, 4));
            f.roomHere = proxy(IRoomController.class, (method, args) -> switch (method) {
                case "isDestroyed" -> false;
                default -> throw new UnsupportedOperationException(method);
            });
            f.controller.shouldFleeOrAttack();
            return f.state();
        }
    }

    private static CreatureState stateWhenThreatenedInHeart(short heartOwner, boolean destroyed) {
        try (Fixture f = new Fixture(9)) {
            EntityId hero = f.data.createEntity();
            f.data.setComponents(hero, new Owner((short) 1, (short) 1), new Health(1000, 1000), new Threat(100));
            f.data.removeComponent(f.entity, Fearless.class);
            f.sensed = Set.of(hero);
            f.roomHere = proxy(IRoomController.class, (method, args) -> switch (method) {
                case "isDungeonHeart" -> true;
                case "getOwnerId" -> heartOwner;
                case "isDestroyed" -> destroyed;
                default -> throw new UnsupportedOperationException(method);
            });
            f.controller.shouldFleeOrAttack();
            return f.state();
        }
    }

    @Test
    void hungryIdleCreatureRetriesEatingEveryFourTicks() {
        try (Fixture f = new Fixture(9)) {
            f.reachable.set(false);
            f.data.setComponent(f.entity, new CreatureHunger(0, 2));
            for (int i = 0; i < 9; i++) {
                f.tick();
            }
            assertEquals(List.of(.25, 1.25, 2.25), f.eatAttempts);
            assertEquals(2.25, f.data.getComponent(f.entity, CreatureHunger.class).lastEatAttemptTime);
        }
    }

    @Test
    void meleeHitDelayIsRoundedDownToWholeTurns() {
        try (Fixture f = new Fixture(9)) {

            // A Dwarf's 0.47 s is one turn at 4 turns a second
            CreatureSleepFixtures.setShotDelay(f.definition, .47f);
            EntityId target = f.meleeTarget();
            f.controller.executeAttack(target);
            assertNull(f.data.getComponent(target, Damage.class));

            f.time = .25;
            f.controller.getStateMachine().update();
            assertEquals(30, f.data.getComponent(target, Damage.class).damage);
        }
    }

    @Test
    void meleeHitIsLostWhenTheSwingIsInterruptedOrTheTargetLeavesTheMap() {
        try (Fixture f = new Fixture(9)) {
            CreatureSleepFixtures.setShotDelay(f.definition, .5f);
            EntityId target = f.meleeTarget();
            f.controller.executeAttack(target);
            f.controller.getStateMachine().changeState(CreatureState.FIGHT);
            assertNull(f.data.getComponent(f.entity, MeleeHit.class));

            f.time = 2;
            f.controller.executeAttack(target);
            f.data.removeComponent(target, Position.class);
            f.time = 2.5;
            f.controller.getStateMachine().update();
            assertNull(f.data.getComponent(target, Damage.class));
            assertFalse(f.events.contains(GameEventType.MELEE_HIT));
        }
    }

    @Test
    void meleeAttackRechargesInItsRechargeTimeDividedByTheWorkModifier() {
        try (Fixture f = new Fixture(9)) {
            EntityId target = f.meleeTarget();
            f.controller.executeAttack(target);
            f.controller.getStateMachine().changeState(CreatureState.FIGHT);

            // 1.2 s / 0.8 = 1.5 s, and the attack is ready only after that
            f.time = 1.5;
            f.controller.executeAttack(target);
            assertEquals(CreatureState.FIGHT, f.state());

            f.time = 1.75;
            f.controller.executeAttack(target);
            assertEquals(CreatureState.MELEE_ATTACK, f.state());
        }
    }

    @Test
    void swingPlaysItsAnimationPartsOneAfterAnother() {
        Creature definition = CreatureSleepFixtures.sleeper(9);

        // A Knight's swing: 9 + 30 + 11 frames at 18 fps take 11.1 turns at 4 turns a second, so the last frame comes
        // on turn 12, the creature is back in FIGHT on turn 12 and swings again on turn 13, as a Knight does in DK II.
        // Rounding each part up on its own would take 2 + 7 + 3 turns and a turn after each part, 15 turns.
        CreatureSleepFixtures.setSwing(definition, Creature.AnimationType.MELEE_ATTACK, 30, 18, 9, 11);
        try (Fixture f = new Fixture(definition)) {
            f.controller.executeAttack(f.meleeTarget());

            f.time = 2.75;
            f.controller.getStateMachine().update();
            assertEquals(CreatureState.MELEE_ATTACK, f.state());

            f.time = 3;
            f.controller.getStateMachine().update();
            assertEquals(CreatureState.FIGHT, f.state());
        }
    }

    @Test
    void hungerInterruptsRoomWorkBeforeFatigue() {
        for (TaskType type : List.of(TaskType.TRAIN, TaskType.RESEARCH)) {
            try (Fixture f = new Fixture(9)) {
                f.startWork(type);
                f.data.setComponent(f.entity, new CreatureHunger(0, 2));
                f.data.setComponent(f.entity, new CreatureSleep(f.bed, -20, 0, 0));
                f.tick();
                assertEquals(TaskType.GO_TO_EAT, f.controller.getAssignedTask().getTaskType());
                assertEquals(CreatureState.WORK, f.state());
                assertEquals(1, f.oldJobUnassignments);
                assertEquals(0, f.sleepAssignments);
            }
        }
    }

    @Test
    void halfOfTheSwingsPlayTheSecondMeleeAnimationWhenTheFirstIsFlaggedForIt() {
        Creature definition = CreatureSleepFixtures.sleeper(9);

        // The Knight's first melee animation is flagged, its second one takes 9 + 29 + 12 frames at 30 fps, 6.7 turns,
        // so a swing with it ends on turn 8
        CreatureSleepFixtures.setSwing(definition, Creature.AnimationType.MELEE_ATTACK, 30, 18, 9, 11,
                ArtResource.ArtResourceFlag.RANDOM_SWIPE);
        CreatureSleepFixtures.setSwing(definition, Creature.AnimationType.SWIPE, 29, 30, 9, 12);
        try (Fixture f = new Fixture(definition)) {
            EntityId target = f.meleeTarget();
            int swipes = 0;
            for (int i = 0; i < 40; i++) {
                f.time = i * 10;
                f.controller.getStateMachine().changeState(CreatureState.FIGHT);
                f.controller.executeAttack(target);
                assertEquals(CreatureState.MELEE_ATTACK, f.state());
                f.data.setComponent(f.entity, new CreatureAi(f.time, CreatureState.MELEE_ATTACK, (short) 6)); // As the AI tick publishes it
                boolean swipe = f.data.getComponent(f.entity, CreatureMeleeAttack.class).swipe;
                if (swipe) {
                    swipes++;
                }

                f.time = i * 10 + 1.5;
                f.controller.getStateMachine().update();
                assertEquals(CreatureState.MELEE_ATTACK, f.state());

                f.time = i * 10 + 1.75;
                f.controller.getStateMachine().update();
                assertEquals(swipe ? CreatureState.FIGHT : CreatureState.MELEE_ATTACK, f.state());
            }
            assertTrue(swipes > 0 && swipes < 40);
        }
    }

    @Test
    void chickenGivenToAPrisonerFeedsItWithoutLettingItOut() {
        try (Fixture f = new Fixture(9)) {
            f.data.setComponent(f.entity, new CreatureHunger(0, 1));
            f.controller.getStateMachine().changeState(CreatureState.IMPRISONED);
            EntityId chicken = f.data.createEntity();
            f.data.setComponent(chicken, new Health(1000, 1000));

            f.controller.eat(proxy(IEntityController.class, (method, args) -> switch (method) {
                case "getEntityId" -> chicken;
                case "remove" -> {
                    f.data.removeEntity(chicken);
                    yield null;
                }
                default -> throw new UnsupportedOperationException(method);
            }));

            assertEquals(CreatureState.IMPRISONED, f.state());
            assertNull(f.data.getComponent(chicken, Health.class));
            assertEquals(0, f.data.getComponent(f.entity, CreatureHunger.class).amountNeeded);
            assertEquals(-250, f.data.getComponent(f.entity, Damage.class).damage);
        }
    }

    @Test
    void chickenGivesNothingToACreatureThatIsNotHungry() {
        try (Fixture f = new Fixture(9)) {
            f.data.setComponent(f.entity, new CreatureHunger(0, 0));

            f.controller.sate();

            assertNull(f.data.getComponent(f.entity, Damage.class));
        }
    }

    @Test
    void prisonerKeepsItsDyingTimeAsHealthAndGetsATenthOfItsMaximumOnTop() {
        try (Fixture f = new Fixture(9)) {
            f.data.setComponents(f.entity, new Health(0, 1000), new Unconscious(0));
            f.time = 10;

            f.controller.imprison((short) 4, f.room);

            // 50 seconds of dying time left at 4 ticks a second, and 1000 / 10
            assertEquals(300, f.health());
            assertNull(f.data.getComponent(f.entity, Unconscious.class));
        }
    }

    @Test
    void swingKeepsToTheFirstMeleeAnimationWhenItIsNotFlagged() {
        Creature definition = CreatureSleepFixtures.sleeper(9);
        CreatureSleepFixtures.setSwing(definition, Creature.AnimationType.MELEE_ATTACK, 30, 18, 9, 11);
        CreatureSleepFixtures.setSwing(definition, Creature.AnimationType.SWIPE, 29, 30, 9, 12);
        try (Fixture f = new Fixture(definition)) {
            EntityId target = f.meleeTarget();
            for (int i = 0; i < 20; i++) {
                f.time = i * 10;
                f.controller.getStateMachine().changeState(CreatureState.FIGHT);
                f.controller.executeAttack(target);
                assertFalse(f.data.getComponent(f.entity, CreatureMeleeAttack.class).swipe);
            }
        }
    }

    @Test
    void prisonerThatIsAwakeGetsATenthOfItsMaximumHealth() {
        try (Fixture f = new Fixture(9)) {
            f.controller.imprison((short) 4, f.room);

            assertEquals(350, f.health());
        }
    }

    @Test
    void asksTheTaskForItsTargetOnceAndOnlyWhenStopped() {
        try (Fixture f = new Fixture(9)) {
            f.startWork(TaskType.TRAIN);
            f.targetQueries = 0;

            // On its way, a task may search a path for its target
            f.data.setComponent(f.entity, new Navigation(new Point(3, 4), null, List.of()));
            assertFalse(f.controller.isAtAssignedTaskTarget());
            assertEquals(0, f.targetQueries);

            f.data.removeComponent(f.entity, Navigation.class);
            assertTrue(f.controller.isAtAssignedTaskTarget());
            assertEquals(1, f.targetQueries);
        }
    }

    @Test
    void idleHeroTriesItsObjectiveAgainEveryTwelveTicks() {
        try (Fixture f = new Fixture(9)) {
            f.data.setComponent(f.entity, new Owner(Player.GOOD_PLAYER_ID, Player.GOOD_PLAYER_ID));
            f.data.setComponent(f.entity, new Objective(Thing.HeroParty.Objective.KILL_PLAYER, (short) 3, 0));
            f.data.removeComponent(f.entity, CreatureSleep.class);
            f.controller.getStateMachine().changeState(CreatureState.IDLE);
            for (int i = 0; i < 24; i++) {
                f.tick();
            }

            // DK II's hero idle state does nothing for 12 ticks when the objective can't start (0x477850)
            assertEquals(List.of(0d, 3d, 6d), f.objectiveAttempts);
        }
    }

    @Test
    void wakingAndImprisoningPreserveASeededDyingBudget() {
        try (Fixture f = new Fixture(9)) {
            f.data.setComponents(f.entity, new Health(50, 1000), new Unconscious(-47.5));
            f.controller.wakeUp();
            assertEquals(50, f.health());
            assertNull(f.data.getComponent(f.entity, Unconscious.class));
        }
        try (Fixture f = new Fixture(9)) {
            f.data.setComponents(f.entity, new Health(50, 1000), new Unconscious(-47.5));
            f.controller.imprison((short) 4, f.room);
            assertEquals(150, f.health());
            assertNull(f.data.getComponent(f.entity, Unconscious.class));
        }
    }

    private static CreatureComponent workerComponent() {
        CreatureComponent component = new CreatureComponent();
        component.creatureId = 6;
        component.stunDuration = .1f;
        component.worker = true;
        return component;
    }

    @Test
    void isAtTheTaskWhenStandingOnItsWorkPosition() {
        try (Fixture f = new Fixture(9)) {
            f.startWork(TaskType.TRAIN);

            // Away from the middle of the target tile, but where the task wants us
            f.data.setComponent(f.entity, new Position(0, new Vector3f(3.9f, WorldUtils.FLOOR_HEIGHT, 4.6f)));
            assertFalse(f.controller.isAtAssignedTaskTarget());

            f.workPosition = new Vector2f(3.9f, 4.6f);
            assertTrue(f.controller.isAtAssignedTaskTarget());
        }
    }

    @Test
    void isAtTheTaskOnTheTickItsWalkReachesItsLastPoint() {
        try (Fixture f = new Fixture(9)) {
            f.startWork(TaskType.TRAIN);
            List<Vector2> path = List.of(new Vector2(1, 4), new Vector2(3, 4));
            f.data.setComponent(f.entity, new Navigation(new Point(3, 4), null, path));

            // Next to the target, but still walking
            f.data.setComponent(f.entity, new Position(0, new Vector3f(2.5f, WorldUtils.FLOOR_HEIGHT, 4)));
            assertFalse(f.controller.isAtAssignedTaskTarget());

            // DK II's walk is done when it reaches its last point, the creature doesn't come to rest first (0x470C80)
            f.data.setComponent(f.entity, new Position(0, new Vector3f(2.95f, WorldUtils.FLOOR_HEIGHT, 4)));
            assertTrue(f.controller.isAtAssignedTaskTarget());
        }
    }

    private static final class Fixture implements AutoCloseable {
        final EntityData data = new DefaultEntityData();
        final EntityId entity = data.createEntity();
        final EntityId bed = data.createEntity();
        final EntityId room = data.createEntity();
        final Map<Long, Task> tasks = new HashMap<>();
        final AtomicBoolean reachable = new AtomicBoolean(true);
        final Creature definition;
        final Keeper keeper = new Keeper(false, (short) 3);
        IRoomController roomHere;
        Set<EntityId> sensed = Set.of();
        List<EntityId> onTile = List.of();
        CreatureController controller;
        final java.util.function.Supplier<CreatureController> controllers;
        final CreatureSleepSystem sleepSystem;
        final CreatureMoodSystem moodSystem;
        final HealthSystem healthSystem;
        final CreatureFallSystem fall;
        final CreatureLandingSystem landing;
        final List<GameEventType> events = new java.util.ArrayList<>();
        final GameEventService eventService = proxy(GameEventService.class, (method, args) -> {
            events.add((GameEventType) args[0]);
            return null;
        });
        final List<Double> eatAttempts = new java.util.ArrayList<>();
        final List<Double> objectiveAttempts = new java.util.ArrayList<>();
        int sleepAssignments;
        int lairSearches;
        int oldJobUnassignments;
        int targetQueries;
        Vector2f workPosition;
        double time;

        Fixture(int timeSleep) {
            this(CreatureSleepFixtures.sleeper(timeSleep));
        }

        Fixture(Creature definition) {
            fall = new CreatureFallSystem(data);
            landing = new CreatureLandingSystem(data, null, 0f, () -> time, eventService, null);
            this.definition = definition;
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
                case "getRoomControllerByCoordinates" -> roomHere;
                default -> throw new UnsupportedOperationException(method);
            });
            ITaskManager taskManager = proxy(ITaskManager.class, (method, args) -> switch (method) {
                case "getTaskById" -> tasks.get((Long) args[0]);
                case "addToUnemployedWorkerQueue" -> null;
                case "assignObjectiveTask" -> {
                    objectiveAttempts.add(time);
                    yield false;
                }
                case "isTaskAvailable", "assignGoldToTreasuryTask" -> false;
                case "assignClosestRoomTask" -> {
                    lairSearches++;
                    yield false;
                }
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
                case "assignEatTask" -> {
                    eatAttempts.add(time);
                    if (!reachable.get()) {
                        yield false;
                    }
                    Task task = task(-2, TaskType.GO_TO_EAT);
                    tasks.put(-2L, task);
                    ((ICreatureController) args[0]).setAssignedTask(task);
                    yield true;
                }
                default -> throw new UnsupportedOperationException(method);
            });
            roomHere = lair;
            Map<MiscType, MiscVariable> settings = new EnumMap<>(MiscType.class);
            settings.put(MiscType.CREATURE_CRITICAL_HEALTH_PERCENTAGE_OF_MAX,
                    CreatureSleepFixtures.variable(MiscType.CREATURE_CRITICAL_HEALTH_PERCENTAGE_OF_MAX, 10));
            settings.put(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS,
                    CreatureSleepFixtures.variable(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, 60));
            settings.put(MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND,
                    CreatureSleepFixtures.variable(MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, -3));
            settings.put(MiscType.GAMBLING_MODIFY_CREATURE_MONEY_MONEY_PER_SECOND,
                    CreatureSleepFixtures.variable(MiscType.GAMBLING_MODIFY_CREATURE_MONEY_MONEY_PER_SECOND, -10));
            settings.put(MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND,
                    CreatureSleepFixtures.variable(MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND, 100));
            settings.put(MiscType.MODIFY_ANGER_OF_CREATURE_IN_LAIR_PER_SECOND,
                    CreatureSleepFixtures.variable(MiscType.MODIFY_ANGER_OF_CREATURE_IN_LAIR_PER_SECOND, -550));
            settings.put(MiscType.CANNOT_SLEEP_MODIFY_CREATURE_HEALTH_PER_SECOND,
                    CreatureSleepFixtures.variable(MiscType.CANNOT_SLEEP_MODIFY_CREATURE_HEALTH_PER_SECOND, 0));
            settings.put(MiscType.WORKER_FULL_GOLD_MARGIN, CreatureSleepFixtures.variable(MiscType.WORKER_FULL_GOLD_MARGIN, 340));
            IEntityPositionLookup lookup = proxy(IEntityPositionLookup.class, (method, args) -> switch (method) {
                case "getSensedEntities" -> sensed;
                case "getEntityLocation" -> null; // Not on a Hero Lair
                case "getBlockingDoor" -> null;
                case "getEntitiesInLocation" -> onTile;
                default -> throw new UnsupportedOperationException(method);
            });
            IKwdFile kwd = proxy(IKwdFile.class, (method, args) -> switch (method) {
                case "getCreature" -> definition;
                case "getGameLevel" -> CreatureSleepFixtures.level(4);
                case "getRoomById" -> new Room();
                case "getVariables" -> settings;
                default -> throw new UnsupportedOperationException(method);
            });
            ILevelInfo levelInfo = proxy(ILevelInfo.class, (method, args) -> switch (method) {
                case "getLevelData" -> kwd;
                case "getPlayer" -> keeper;
                default -> throw new UnsupportedOperationException(method);
            });
            ICreaturesController creatures = proxy(ICreaturesController.class, (method, args) -> switch (method) {
                case "isValidEntity" -> data.getComponent((EntityId) args[0], Position.class) != null;
                case "createController" -> proxy(ICreatureController.class, (method2, args2) -> switch (method2) {
                    case "getEntityId" -> args[0];
                    case "isWorker" -> false;
                    default -> throw new UnsupportedOperationException(method2);
                });
                default -> throw new UnsupportedOperationException(method);
            });
            INavigationService navigation = proxy(INavigationService.class, (method, args) -> switch (method) {
                case "findPath" -> new DefaultGraphPath<>();
                case "isReachable" -> true;
                default -> throw new UnsupportedOperationException(method);
            });
            IObjectsController objects = proxy(IObjectsController.class, (method, args) -> switch (method) {
                case "createController" -> proxy(IObjectController.class, (method2, args2) -> switch (method2) {
                    case "getType" -> ObjectType.GOLD;
                    case "getEntityId" -> args[0];
                    case "remove" -> {
                        data.removeEntity((EntityId) args[0]);
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method2);
                });
                default -> throw new UnsupportedOperationException(method);
            });
            controllers = () -> new CreatureController(entity, data, definition, navigation, taskManager, () -> time,
                    settings, creatures, lookup, map, levelInfo, objects, null, eventService);
            controller = controllers.get();
            sleepSystem = new CreatureSleepSystem(data, kwd, () -> time);
            moodSystem = new CreatureMoodSystem(data, kwd, List.of(), () -> time, settings);
            healthSystem = new HealthSystem(data, lookup, settings, null, null, List.of(), map, () -> time, eventService);
        }

        Task task(long id, TaskType type) {
            return proxy(Task.class, (method, args) -> switch (method) {
                case "getId" -> id;
                case "getTaskType" -> type;
                case "isValid" -> true;
                case "moveToOtherSpot" -> false;
                case "getTarget" -> {
                    targetQueries++;
                    yield new Vector2f(3, 4);
                }
                case "getWorkPosition" -> workPosition;
                case "getTaskTarget", "getTaskLocation", "executeTask" -> null;
                case "unassign" -> { oldJobUnassignments++; yield null; }
                default -> throw new UnsupportedOperationException(method);
            });
        }

        void startWork(TaskType type) {
            Task job = task(-1, type);
            tasks.put(-1L, job);
            data.setComponent(entity, new TaskComponent(-1, null, null, type, 0, false));
            data.setComponent(entity, new CreatureAi(0, CreatureState.WORK, (short) 6));
            controller.getStateMachine().changeState(CreatureState.WORK);
        }

        EntityId goldPile(int gold) {
            EntityId pile = data.createEntity();
            data.setComponents(pile, new ObjectComponent((short) 1, ObjectType.GOLD), new Gold(gold, 2000));
            onTile = List.of(pile);
            return pile;
        }

        EntityId meleeTarget() {
            EntityId target = data.createEntity();

            // In front of the creature: DK II only swings at a target it faces (0x45F030)
            data.setComponents(target, new Position(0, new Vector3f(3, WorldUtils.FLOOR_HEIGHT, 4.5f)), new Health(500, 500));
            data.setComponent(entity, new CreatureMeleeAttack(0, 30, 1.2f, 1));
            controller.getStateMachine().changeState(CreatureState.FIGHT);
            return target;
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
            landing.stop();
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
