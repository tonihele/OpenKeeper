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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

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
    void workerWithNothingToDoKeepsItsGold() {
        Worker worker = new Worker(CreatureState.IDLE, false, false);

        CreatureState.IDLE.enter(worker.creature);

        // As in the original, an imp with some gold and nothing to do doesn't bring it home
        assertEquals(CreatureState.IDLE, worker.state.get());
        assertFalse(worker.wentToTreasury.get());
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
        return 0;
    }
}
