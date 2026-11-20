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
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.AttackTarget;
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
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Objective;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Slapped;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.component.Threat;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.component.WithdrawnFrom;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.entity.IEntityController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.game.logic.CreatureFallSystem;
import toniarts.openkeeper.game.logic.CreatureMoodSystem;
import toniarts.openkeeper.game.logic.CreatureSleepSystem;
import toniarts.openkeeper.game.logic.HealthSystem;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.ITaskManager;
import toniarts.openkeeper.game.task.Task;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.game.task.creature.GoToSleep;
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

    @Test
    void fleesInAnEnemyOrRuinedHeartButStandsInItsOwn() {
        assertEquals(CreatureState.FLEE, stateWhenThreatenedInHeart((short) 4, false));
        assertEquals(CreatureState.FLEE, stateWhenThreatenedInHeart((short) 3, true));
        assertNotEquals(CreatureState.FLEE, stateWhenThreatenedInHeart((short) 3, false));
    }

    @Test
    void workerWithdrawsFromAnEnemyAfterItAndOnlyHitsBackNearItsHeart() {

        // An enemy after it sends it away at once, wherever it is
        assertEquals(CreatureState.FLEE, workerStateWithHeroAt(6, 20, true));
        assertEquals(CreatureState.FLEE, workerStateWithHeroAt(3.5f, 3, true));

        // Near its own heart it hits back at an enemy in reach that is after nobody, 3 tiles from the heart's
        // centre is still near it
        assertNull(workerStateWithHeroAt(6, 3, null)); // Neither fights nor flees, keeps working
        assertEquals(CreatureState.FIGHT, workerStateWithHeroAt(3.5f, 3, null));
        assertEquals(CreatureState.FIGHT, workerStateWithHeroAt(3.5f, 6, null));

        // Elsewhere it keeps working next to an enemy that isn't after it
        assertNull(workerStateWithHeroAt(3.5f, 7, null));
        assertNull(workerStateWithHeroAt(3.5f, 20, null));
        assertNull(workerStateWithHeroAt(3.5f, 20, false));
    }

    @Test
    void workerWithdrawsWhenTheEnemyTurnsOnItInTheMiddleOfAFight() {
        try (Fixture f = new Fixture(CreatureSleepFixtures.worker())) {
            EntityId hero = heroNextToWorker(f, 3.5f, 3);
            f.controller.shouldFleeOrAttack();
            assertEquals(CreatureState.FIGHT, f.state());

            f.data.setComponent(hero, new AttackTarget(f.entity));
            f.controller.getStateMachine().update();

            // It backs off (DK II's 0xCB), which for a worker lasts no time, then withdraws
            assertEquals(CreatureState.BACK_OFF, f.state());
            f.controller.getStateMachine().update();
            assertEquals(CreatureState.FLEE, f.state());
        }
    }

    @Test
    void workerWithdrawsWhenTheEnemyTurnsOnItDuringItsOwnSwing() {
        try (Fixture f = new Fixture(CreatureSleepFixtures.worker())) {
            EntityId hero = heroNextToWorker(f, 3.5f, 3);
            f.controller.getStateMachine().changeState(CreatureState.MELEE_ATTACK);

            f.data.setComponent(hero, new AttackTarget(f.entity));
            f.controller.getStateMachine().update();

            // It backs off (DK II's 0xCB), which for a worker lasts no time, then withdraws
            assertEquals(CreatureState.BACK_OFF, f.state());
            f.controller.getStateMachine().update();
            assertEquals(CreatureState.FLEE, f.state());
        }
    }

    @Test
    void workerHitsBackAtAnEnemyATileAwayNearItsHeart() {
        try (Fixture f = new Fixture(CreatureSleepFixtures.worker())) {
            heroNextToWorker(f, 4, 3);
            f.data.setComponent(f.entity, new CreatureMeleeAttack(0, 30, 1.2f, .3f));
            f.controller.shouldFleeOrAttack();
            assertEquals(CreatureState.FIGHT, f.state());
        }
    }

    @Test
    void workerWithdrawsFromAnEnemyAfterItThatItCantSense() {
        try (Fixture f = new Fixture(CreatureSleepFixtures.worker())) {
            EntityId archer = heroNextToWorker(f, 8, 20);
            f.data.setComponent(archer, new AttackTarget(f.entity));

            // An archer shoots from farther than a worker senses
            f.sensed = Set.of();
            f.controller.shouldFleeOrAttack();

            assertEquals(CreatureState.FLEE, f.state());
        }
    }

    @Test
    void workerWithdrawsNextToItsHeartAndGoesBackToWorkOnArrival() {
        try (Fixture f = new Fixture(CreatureSleepFixtures.worker())) {
            EntityId hero = heroNextToWorker(f, 3.5f, 3);
            f.data.setComponent(hero, new AttackTarget(f.entity));
            f.controller.shouldFleeOrAttack();
            assertEquals(CreatureState.FLEE, f.state());
            // 2 tiles either way of (4, 5), a tile down and right of the heart's centre
            assertTrue(Set.of(new Point(6, 7), new Point(2, 7), new Point(6, 3), new Point(2, 3))
                    .contains(f.data.getComponent(f.entity, Navigation.class).target));

            // Still wanted when it gets there, so off it goes again
            f.data.removeComponent(f.entity, Navigation.class);
            f.controller.getStateMachine().update();
            assertEquals(CreatureState.FLEE, f.state());
            assertNotNull(f.data.getComponent(f.entity, Navigation.class));

            // The enemy has turned to someone else
            f.data.setComponent(hero, new AttackTarget(f.data.createEntity()));
            f.data.setComponent(hero, new Position(0, new Vector3f(8, WorldUtils.FLOOR_HEIGHT, 4)));
            f.data.removeComponent(f.entity, Navigation.class);
            f.controller.getStateMachine().update();
            assertEquals(CreatureState.IDLE, f.state());
        }
    }

    @Test
    void workerThatCantGetRoundItsHeartWithdrawsToTheRoomPositionOfOneOfItsRooms() {
        try (Fixture f = new Fixture(CreatureSleepFixtures.worker())) {
            EntityId hero = heroNextToWorker(f, 3.5f, 3);
            f.data.setComponent(hero, new AttackTarget(f.entity));
            f.rooms = List.of(room((short) 3, new Point(3, 4), true), room((short) 3, new Point(10, 4), false),
                    room((short) 1, new Point(12, 4), false));
            // Only the keeper's Treasury and an enemy's room can be reached
            f.walkable = tile -> tile.equals(new Point(10, 4)) || tile.equals(new Point(12, 4));
            boolean withdrew = false;
            for (int i = 0; i < 20; i++) {
                f.data.removeComponent(f.entity, Navigation.class);
                boolean fled = f.controller.flee();
                Navigation navigation = f.data.getComponent(f.entity, Navigation.class);

                // Picking the heart 3 times running, it finds nowhere to go and doesn't withdraw
                assertEquals(navigation != null, fled);
                if (navigation != null) {
                    assertEquals(new Point(10, 4), navigation.target);
                    withdrew = true;
                }
            }
            assertTrue(withdrew);

            // Only the heart's corners can be walked on, so the room position is the heart's, a tile down and right of
            // its centre, unless the random point 2 tiles off it is the reachable corner
            f.rooms = List.of(room((short) 3, new Point(3, 4), true));
            f.walkable = tile -> Math.abs(tile.x - 3) <= 2 && Math.abs(tile.y - 4) <= 2
                    && Math.abs(tile.x - 3) + Math.abs(tile.y - 4) > 1;
            for (int i = 0; i < 20; i++) {
                f.data.removeComponent(f.entity, Navigation.class);
                f.controller.flee();
                assertTrue(Set.of(new Point(4, 5), new Point(2, 3))
                        .contains(f.data.getComponent(f.entity, Navigation.class).target));
            }
        }
    }

    @Test
    void workerThatCantGetToAnyRoomWithdrawsAwayFromTheEnemyItFights() {
        try (Fixture f = new Fixture(CreatureSleepFixtures.worker())) {
            EntityId hero = heroNextToWorker(f, 5.5f, 10);
            f.data.setComponent(hero, new AttackTarget(f.entity));
            f.data.setComponent(f.entity, new AttackTarget(hero));
            f.rooms = List.of(room((short) 3, new Point(10, 4), true));
            // Only the tiles west of the worker, away from the hero, can be reached
            f.walkable = tile -> tile.x < 3;
            boolean withdrew = false;
            for (int i = 0; i < 20; i++) {
                f.data.removeComponent(f.entity, Navigation.class);
                f.controller.flee();
                Navigation navigation = f.data.getComponent(f.entity, Navigation.class);
                if (navigation != null) {

                    // Less than 4 times the 2 tiles to the hero, and up to 3 tiles either way as it stands level with it
                    assertTrue(navigation.target.x >= 0 && navigation.target.x < 3, navigation.target.toString());
                    assertTrue(Math.abs(navigation.target.y - 4) <= 3, navigation.target.toString());
                    withdrew = true;
                }
            }
            assertTrue(withdrew);
        }
    }

    @Test
    void workerWithNowhereToWithdrawToKeepsWorking() {
        try (Fixture f = new Fixture(CreatureSleepFixtures.worker())) {
            EntityId hero = heroNextToWorker(f, 6, 20);
            f.data.setComponent(hero, new AttackTarget(f.entity));
            f.rooms = List.of(room((short) 3, new Point(20, 4), true));
            f.walkable = tile -> false;

            // DK II's withdraw finds nowhere to go (0x4611D0), so the worker isn't sent anywhere (0x45BDF0)
            assertFalse(f.controller.shouldFleeOrAttack());
            assertNull(f.state());
            assertNull(f.data.getComponent(f.entity, Navigation.class));

            // Already withdrawing, it goes back to work once it stops
            f.walkable = tile -> true;
            f.controller.shouldFleeOrAttack();
            assertEquals(CreatureState.FLEE, f.state());
            f.walkable = tile -> false;
            f.data.removeComponent(f.entity, Navigation.class);
            f.controller.getStateMachine().update();
            assertEquals(CreatureState.IDLE, f.state());
        }
    }

    @Test
    void creatureGoesBackToWhereItWithdrewFromOnceItIsWellAgain() {
        try (Fixture f = new Fixture(9)) {
            f.keeper.setDungeonHeartLocation(new Point(20, 4));
            assertTrue(f.controller.flee());

            // DK II stores the tile as (x, x) (0x4611D0), we keep the intended (x, y)
            assertEquals(new Point(3, 4), f.data.getComponent(f.entity, WithdrawnFrom.class).tile);

            // By its heart with three quarters of its health, it stays (0x4773D0)
            f.data.setComponents(f.entity, new Position(0, new Vector3f(21, WorldUtils.FLOOR_HEIGHT, 5)), new Health(750, 1000));
            f.data.removeComponent(f.entity, Navigation.class);
            assertFalse(f.controller.goBackToWhereWeFought());
            assertNull(f.data.getComponent(f.entity, Navigation.class));

            // With more, the idle creature walks back, once
            f.data.setComponent(f.entity, new Health(751, 1000));
            f.controller.getStateMachine().changeState(CreatureState.IDLE);
            assertEquals(new Point(3, 4), f.data.getComponent(f.entity, Navigation.class).target);
            assertFalse(f.controller.goBackToWhereWeFought());

            // Up to 500 turns after the withdraw, 4 turns a second here
            f.controller.flee();
            f.time = 124.75;
            assertTrue(f.controller.goBackToWhereWeFought());
            f.controller.flee();
            f.time += 125;
            assertFalse(f.controller.goBackToWhereWeFought());
            assertNull(f.data.getComponent(f.entity, WithdrawnFrom.class));
        }

        // An Imp doesn't remember where it withdrew from
        try (Fixture f = new Fixture(CreatureSleepFixtures.worker())) {
            f.keeper.setDungeonHeartLocation(new Point(20, 4));
            assertTrue(f.controller.flee());
            assertNull(f.data.getComponent(f.entity, WithdrawnFrom.class));
        }
    }

    private static IRoomController room(short owner, Point centre, boolean heart) {
        RoomInstance instance = new RoomInstance(new Room());
        instance.addCoordinate(centre);
        return proxy(IRoomController.class, (method, args) -> switch (method) {
            case "getOwnerId" -> owner;
            case "getRoomInstance" -> instance;
            case "isDungeonHeart" -> heart;
            default -> throw new UnsupportedOperationException(method);
        });
    }

    private static CreatureState workerStateWithHeroAt(float x, int heartX, Boolean heroAfterUs) {
        try (Fixture f = new Fixture(CreatureSleepFixtures.worker())) {
            EntityId hero = heroNextToWorker(f, x, heartX);
            if (heroAfterUs != null) {
                f.data.setComponent(hero, new AttackTarget(heroAfterUs ? f.entity : f.data.createEntity()));
            }
            f.controller.shouldFleeOrAttack();
            return f.state();
        }
    }

    private static EntityId heroNextToWorker(Fixture f, float x, int heartX) {
        EntityId hero = f.data.createEntity();
        f.data.setComponents(hero, new Owner((short) 1, (short) 1), new Health(1000, 1000), new Threat(100),
                new Position(0, new Vector3f(x, WorldUtils.FLOOR_HEIGHT, 4)));
        f.data.setComponent(f.entity, new CreatureMeleeAttack(0, 30, 1.2f, 1));
        f.data.removeComponent(f.entity, Fearless.class);
        f.sensed = Set.of(hero);
        f.keeper.setDungeonHeartLocation(new Point(heartX, 4));
        f.roomHere = proxy(IRoomController.class, (method, args) -> switch (method) {
            case "isDestroyed" -> false;
            default -> throw new UnsupportedOperationException(method);
        });
        return hero;
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

    private static CreatureComponent workerComponent() {
        CreatureComponent component = new CreatureComponent();
        component.creatureId = 6;
        component.stunDuration = .1f;
        component.worker = true;
        return component;
    }

    private static final class Fixture implements AutoCloseable {
        final EntityData data = new DefaultEntityData();
        final EntityId entity = data.createEntity();
        final EntityId bed = data.createEntity();
        final EntityId room = data.createEntity();
        final Map<Long, Task> tasks = new HashMap<>();
        final AtomicBoolean reachable = new AtomicBoolean(true);
        java.util.function.Predicate<Point> walkable = tile -> true;
        List<IRoomController> rooms = List.of();
        final Keeper keeper = new Keeper(false, (short) 3);
        IRoomController roomHere;
        Set<EntityId> sensed = Set.of();
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
        final List<Double> eatAttempts = new java.util.ArrayList<>();
        final List<Double> objectiveAttempts = new java.util.ArrayList<>();
        int sleepAssignments;
        int oldJobUnassignments;
        int targetQueries;
        double time;

        Fixture(int timeSleep) {
            this(CreatureSleepFixtures.sleeper(timeSleep));
        }

        Fixture(Creature definition) {
            fall = new CreatureFallSystem(data, () -> time, eventService);
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
            IMapDataInformation<?> mapData = proxy(IMapDataInformation.class, (method, args) -> switch (method) {
                case "getWidth", "getHeight" -> 32;
                default -> throw new UnsupportedOperationException(method);
            });
            IMapController map = proxy(IMapController.class, (method, args) -> switch (method) {
                case "getRoomControllerByCoordinates" -> roomHere;
                case "getRoomControllers" -> rooms;
                case "getMapData" -> mapData;
                default -> throw new UnsupportedOperationException(method);
            });
            ITaskManager taskManager = proxy(ITaskManager.class, (method, args) -> switch (method) {
                case "getTaskById" -> tasks.get((Long) args[0]);
                case "addToUnemployedWorkerQueue" -> null;
                case "assignObjectiveTask" -> {
                    objectiveAttempts.add(time);
                    yield false;
                }
                case "isTaskAvailable" -> false;
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
            IEntityPositionLookup lookup = proxy(IEntityPositionLookup.class, (method, args) -> switch (method) {
                case "getSensedEntities" -> sensed;
                case "getEntityLocation" -> null; // Not on a Hero Lair
                case "getBlockingDoor" -> null;
                case "getEntitiesInLocation" -> sensed.stream()
                        .filter(other -> WorldUtils.vectorToPoint(data.getComponent(other, Position.class).position).equals(args[0])).toList();
                default -> throw new UnsupportedOperationException(method);
            });
            IKwdFile kwd = proxy(IKwdFile.class, (method, args) -> switch (method) {
                case "getCreature" -> definition;
                case "getImp" -> CreatureSleepFixtures.worker();
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
                case "findPath" -> walkable.test((Point) args[1]) ? new DefaultGraphPath<>() : null;
                case "findRandomAccessibleTile" -> null;
                default -> throw new UnsupportedOperationException(method);
            });
            controllers = () -> new CreatureController(entity, data, definition, navigation, taskManager, () -> time,
                    settings, creatures, lookup, map, levelInfo, null, null, eventService);
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
                case "getTarget" -> { targetQueries++; yield new Vector2f(3, 4); }
                case "getWorkPosition" -> null;
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
