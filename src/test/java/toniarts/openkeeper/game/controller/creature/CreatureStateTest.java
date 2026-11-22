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
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.game.controller.entity.IEntityController;
import toniarts.openkeeper.game.task.Task;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Thing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unchecked")
class CreatureStateTest {

    @Test
    void heroToldToWaitStaysWhereHeIs() {
        assertIdleHeroStaysWhereHeIs(Thing.HeroParty.Objective.WAIT);
    }

    @Test
    void heroPastHisLastWaypointStaysWhereHeIs() {
        assertIdleHeroStaysWhereHeIs(Thing.HeroParty.Objective.SEND_TO_ACTION_POINT);
    }

    /**
     * The hero has the objective but no action point to go to and nothing else
     * to do
     */
    private static void assertIdleHeroStaysWhereHeIs(Thing.HeroParty.Objective objective) {
        AtomicReference<CreatureState> currentState = new AtomicReference<>(CreatureState.IDLE);
        AtomicReference<Integer> walks = new AtomicReference<>(0);
        StateMachine<ICreatureController, CreatureState> stateMachine = stateMachine(currentState);
        ICreatureController hero = (ICreatureController) Proxy.newProxyInstance(
                ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getObjective" -> objective;
                    case "hasObjective", "isTimeToReEvaluate", "isStopped" -> true;
                    case "findWork" -> {
                        ((Consumer<Boolean>) args[0]).accept(false);
                        yield null;
                    }
                    case "navigateToRandomPoint" -> {
                        walks.set(walks.get() + 1);
                        yield null;
                    }
                    case "getStateMachine" -> stateMachine;
                    default -> defaultValue(method.getReturnType());
                });

        CreatureState.IDLE.enter(hero);
        CreatureState.IDLE.update(hero);

        assertEquals(CreatureState.IDLE, currentState.get());
        assertEquals(0, walks.get());
    }

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
    void knockedOutCreatureDropsItsGoldInOneToThreePiles() {
        assertEquals(List.of(800), dropGoldOnKnockout(800));
        assertEquals(List.of(1250, 1250), dropGoldOnKnockout(2500));
        assertEquals(List.of(1666, 1666, 1666), dropGoldOnKnockout(5000));
    }

    private static List<Integer> dropGoldOnKnockout(int gold) {
        EntityData data = new DefaultEntityData();
        EntityId entity = data.createEntity();
        data.setComponents(entity, new Gold(gold, 5000), new Owner((short) 1, (short) 1),
                new Position(0, new Vector3f(4, 0, 11)));
        List<Integer> piles = new ArrayList<>();
        IObjectsController objects = (IObjectsController) Proxy.newProxyInstance(
                IObjectsController.class.getClassLoader(), new Class<?>[]{IObjectsController.class}, (proxy, method, args) -> {
                    if (method.getName().equals("addLooseGold")) {
                        piles.add((Integer) args[2]);
                    }
                    return null;
                });
        EntityController body = new EntityController(entity, data, objects, null);
        ICreatureController creature = (ICreatureController) Proxy.newProxyInstance(
                ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) ->
                method.getDeclaringClass() == IEntityController.class ? method.invoke(body, args) : defaultValue(method.getReturnType()));

        CreatureState.UNCONSCIOUS.enter(creature);

        assertEquals(0, data.getComponent(entity, Gold.class).gold);
        return piles;
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
    void heroWhoseObjectiveCannotStartStaysWhereItIs() {
        AtomicReference<CreatureState> state = new AtomicReference<>(CreatureState.IDLE);
        StateMachine<ICreatureController, CreatureState> stateMachine = stateMachine(state);
        AtomicBoolean triedObjective = new AtomicBoolean();
        AtomicBoolean walked = new AtomicBoolean();
        ICreatureController hero = (ICreatureController) Proxy.newProxyInstance(
                ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getStateMachine" -> stateMachine;
                    case "getOwnerId" -> Player.GOOD_PLAYER_ID;
                    case "hasObjective", "isStopped", "isTimeToReEvaluate" -> true;
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

        CreatureState.IDLE.enter(hero);
        CreatureState.IDLE.update(hero);

        // In DK II it does nothing and tries again later (0x477850)
        assertTrue(triedObjective.get());
        assertFalse(walked.get());
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
