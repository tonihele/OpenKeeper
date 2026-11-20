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

import com.badlogic.gdx.ai.fsm.StateMachine;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.task.Task;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unchecked")
class CreatureStateTest {

    @Test
    void sleepingCreatureWakesWhenItsBedIsGone() {
        assertMissingLairChangesStateToIdle(CreatureState.SLEEPING);
    }

    @Test
    void recuperatingCreatureWakesWhenItsBedIsGone() {
        assertMissingLairChangesStateToIdle(CreatureState.RECUPERATING);
    }

    @Test
    void sleepingCreatureStaysInBedUntilDebtAndHealthRecoveryAreComplete() {
        AtomicReference<CreatureState> currentState = new AtomicReference<>(CreatureState.SLEEPING);
        AtomicReference<Boolean> recovered = new AtomicReference<>(false);
        StateMachine<ICreatureController, CreatureState> stateMachine = stateMachine(currentState);
        ICreatureController creature = creature(stateMachine, true, recovered);

        CreatureState.SLEEPING.update(creature);
        assertEquals(CreatureState.SLEEPING, currentState.get());

        recovered.set(true);
        CreatureState.SLEEPING.update(creature);
        assertEquals(CreatureState.IDLE, currentState.get());
    }

    @Test
    void castReleasesItsShotBeforeItEnds() {
        List<String> calls = new ArrayList<>();
        ICreatureController creature = recordingCreature(calls);

        CreatureState.CAST_SPELL.update(creature);

        assertEquals(List.of("isScared", "releaseCastShot", "isStateTimeExceeded", "changeState FIGHT"), calls);
    }

    @Test
    void interruptedCastDropsItsShot() {
        List<String> calls = new ArrayList<>();

        CreatureState.CAST_SPELL.exit(recordingCreature(calls));

        assertEquals(List.of("cancelCastShot"), calls);
    }

    private static ICreatureController recordingCreature(List<String> calls) {
        StateMachine<ICreatureController, CreatureState> stateMachine = (StateMachine<ICreatureController, CreatureState>) Proxy.newProxyInstance(
                StateMachine.class.getClassLoader(), new Class<?>[]{StateMachine.class}, (proxy, method, args) -> {
                    calls.add(method.getName() + " " + args[0]);
                    return defaultValue(method.getReturnType());
                });
        return (ICreatureController) Proxy.newProxyInstance(
                ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getStateMachine" -> stateMachine;
                    case "isStateTimeExceeded" -> {
                        calls.add(method.getName());
                        yield true;
                    }
                    default -> {
                        calls.add(method.getName());
                        yield defaultValue(method.getReturnType());
                    }
                });
    }

    private static void assertMissingLairChangesStateToIdle(CreatureState state) {
        AtomicReference<CreatureState> currentState = new AtomicReference<>(state);
        StateMachine<ICreatureController, CreatureState> stateMachine = stateMachine(currentState);
        ICreatureController creature = creature(stateMachine, false, new AtomicReference<>(false));

        state.update(creature);

        assertEquals(CreatureState.IDLE, currentState.get());
    }

    private static StateMachine<ICreatureController, CreatureState> stateMachine(
            AtomicReference<CreatureState> currentState) {
        return (StateMachine<ICreatureController, CreatureState>) Proxy.newProxyInstance(
                StateMachine.class.getClassLoader(), new Class<?>[]{StateMachine.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "changeState" -> {
                        currentState.set((CreatureState) args[0]);
                        yield true;
                    }
                    case "getCurrentState" -> currentState.get();
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static ICreatureController creature(
            StateMachine<ICreatureController, CreatureState> stateMachine,
            boolean hasLair, AtomicReference<Boolean> recovered) {
        return (ICreatureController) Proxy.newProxyInstance(
                ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "hasLair" -> hasLair;
                    case "isEnoughSleep" -> recovered.get();
                    case "getStateMachine" -> stateMachine;
                    default -> defaultValue(method.getReturnType());
                });
    }

    @Test
    void workerWithSomeGoldLooksForNewWorkWhenItsTaskIsGone() {
        Worker worker = new Worker(CreatureState.WORK, false, false);

        CreatureState.WORK.update(worker.creature);

        assertEquals(CreatureState.IDLE, worker.state.get());
        assertFalse(worker.wentToTreasury.get());
    }

    @Test
    void fullWorkerUnloadsWhenItsTaskIsGone() {
        Worker worker = new Worker(CreatureState.WORK, true, false);

        CreatureState.WORK.update(worker.creature);

        assertEquals(CreatureState.WORK, worker.state.get());
        assertTrue(worker.wentToTreasury.get());
    }

    @Test
    void fullWorkerUnloadsBeforeTakingNewWork() {
        Worker worker = new Worker(CreatureState.IDLE, true, true);

        CreatureState.IDLE.enter(worker.creature);

        assertEquals(CreatureState.WORK, worker.state.get());
        assertTrue(worker.wentToTreasury.get());
        assertFalse(worker.lookedForWork.get());
    }

    @Test
    void workerWithSomeGoldTakesNewWork() {
        Worker worker = new Worker(CreatureState.IDLE, false, true);

        CreatureState.IDLE.enter(worker.creature);

        assertEquals(CreatureState.WORK, worker.state.get());
        assertTrue(worker.lookedForWork.get());
        assertFalse(worker.wentToTreasury.get());
    }

    @Test
    void creatureOwedPayLeavesItsRoomJob() {
        for (TaskType taskType : List.of(TaskType.TRAIN, TaskType.RESEARCH, TaskType.MANUFACTURE)) {
            AtomicBoolean wentToCollect = new AtomicBoolean();
            Task task = (Task) Proxy.newProxyInstance(Task.class.getClassLoader(), new Class<?>[]{Task.class},
                    (proxy, method, args) -> method.getName().equals("getTaskType") ? taskType : defaultValue(method.getReturnType()));
            ICreatureController creature = (ICreatureController) Proxy.newProxyInstance(
                    ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getAssignedTask" -> task;
                        case "isAssignedTaskValid" -> true;
                        case "goToCollectPay" -> {
                            wentToCollect.set(true);
                            yield true;
                        }
                        default -> defaultValue(method.getReturnType());
                    });

            CreatureState.WORK.update(creature);

            assertTrue(wentToCollect.get(), taskType.toString());
        }
    }

    @Test
    void workerWithNothingToDoKeepsItsGold() {
        Worker worker = new Worker(CreatureState.IDLE, false, false);

        CreatureState.IDLE.enter(worker.creature);

        // As in the original, an imp with some gold and nothing to do doesn't bring it home
        assertEquals(CreatureState.IDLE, worker.state.get());
        assertFalse(worker.wentToTreasury.get());
    }

    @Test
    void idleCreatureThinksAgainAsSoonAsItsWalkEnds() {
        for (boolean worker : new boolean[]{true, false}) {
            for (short ownerId : new short[]{Player.NEUTRAL_PLAYER_ID, Player.GOOD_PLAYER_ID, Player.KEEPER1_ID}) {
                AtomicBoolean lookedForWork = new AtomicBoolean();
                AtomicBoolean walked = new AtomicBoolean();
                StateMachine<ICreatureController, CreatureState> stateMachine = stateMachine(new AtomicReference<>(CreatureState.IDLE));
                ICreatureController creature = (ICreatureController) Proxy.newProxyInstance(
                        ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                            case "getStateMachine" -> stateMachine;
                            case "isWorker" -> worker;
                            case "getOwnerId" -> ownerId;
                            case "isClaimed" -> ownerId != Player.NEUTRAL_PLAYER_ID;
                            case "isStopped" -> true;
                            case "findWork" -> {
                                lookedForWork.set(true);
                                ((Consumer<Boolean>) args[0]).accept(false);
                                yield null;
                            }
                            case "navigateToRandomPoint" -> {
                                walked.set(true);
                                yield null;
                            }
                            default -> defaultValue(method.getReturnType());
                        });

                CreatureState.IDLE.update(creature);

                // In DK II a keeper's creature or a hero that has walked about decides again at once. A neutral one
                // keeps its own pace
                boolean owned = ownerId != Player.NEUTRAL_PLAYER_ID;
                assertEquals(owned, lookedForWork.get());
                assertEquals(owned, walked.get());
            }
        }
    }

    @Test
    void heroWhoseObjectiveCannotStartStandsThroughAnIdleAnimationOrTakesAnIdleWalk() {
        AtomicReference<CreatureState> state = new AtomicReference<>(CreatureState.IDLE);
        StateMachine<ICreatureController, CreatureState> stateMachine = stateMachine(state);
        AtomicBoolean triedObjective = new AtomicBoolean();
        AtomicBoolean walked = new AtomicBoolean();
        AtomicBoolean animationStarts = new AtomicBoolean(true);
        AtomicBoolean animationPlays = new AtomicBoolean();
        ICreatureController hero = (ICreatureController) Proxy.newProxyInstance(
                ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getStateMachine" -> stateMachine;
                    case "getOwnerId" -> Player.GOOD_PLAYER_ID;
                    case "hasObjective", "isStopped" -> true;
                    case "playIdleAnimation" -> animationStarts.get();
                    case "isPlayingIdleAnimation" -> animationPlays.get();
                    case "followObjective" -> {
                        triedObjective.set(true);
                        yield false;
                    }
                    case "findWork" -> {
                        ((Consumer<Boolean>) args[0]).accept(false);
                        yield null;
                    }
                    case "navigateToRandomPoint" -> {
                        walked.set(true);
                        yield null;
                    }
                    default -> defaultValue(method.getReturnType());
                });

        // In DK II it goes to the doing nothing state (0x477850, 0x477680): with an idle animation it stays put
        CreatureState.IDLE.enter(hero);
        assertTrue(triedObjective.get());
        assertFalse(walked.get());

        // While the animation plays it doesn't try its objective
        triedObjective.set(false);
        animationPlays.set(true);
        CreatureState.IDLE.update(hero);
        assertFalse(triedObjective.get());

        // After it the hero tries again, and takes an idle walk when it isn't time for an animation
        animationPlays.set(false);
        animationStarts.set(false);
        CreatureState.IDLE.update(hero);
        assertTrue(triedObjective.get());
        assertTrue(walked.get());
        assertEquals(CreatureState.IDLE, state.get());
    }

    /**
     * An imp carrying some gold, its assigned task is no longer valid
     */
    private static final class Worker {

        private final AtomicReference<CreatureState> state;
        private final AtomicBoolean wentToTreasury = new AtomicBoolean();
        private final AtomicBoolean lookedForWork = new AtomicBoolean();
        private final ICreatureController creature;

        private Worker(CreatureState initialState, boolean full, boolean workAvailable) {
            state = new AtomicReference<>(initialState);
            StateMachine<ICreatureController, CreatureState> stateMachine = (StateMachine<ICreatureController, CreatureState>) Proxy.newProxyInstance(
                    StateMachine.class.getClassLoader(), new Class<?>[]{StateMachine.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "changeState" -> {
                            state.set((CreatureState) args[0]);
                            yield true;
                        }
                        case "getCurrentState" -> state.get();
                        default -> defaultValue(method.getReturnType());
                    });
            creature = (ICreatureController) Proxy.newProxyInstance(
                    ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getStateMachine" -> stateMachine;
                        case "isWorker" -> true;
                        case "getGold" -> 300;
                        case "isTooMuchGold" -> full;
                        case "isAssignedTaskValid" -> false;
                        case "dropGoldToTreasury" -> {
                            wentToTreasury.set(true);
                            yield true;
                        }
                        case "findWork" -> {
                            lookedForWork.set(true);
                            ((Consumer<Boolean>) args[0]).accept(workAvailable);
                            yield null;
                        }
                        default -> defaultValue(method.getReturnType());
                    });
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == short.class) {
            return (short) 0;
        }
        return 0;
    }
}
