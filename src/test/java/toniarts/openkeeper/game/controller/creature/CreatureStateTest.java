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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void neutralCreatureWithNothingToDoIdlesThenWanders() {
        AtomicReference<CreatureState> currentState = new AtomicReference<>(CreatureState.IDLE);
        AtomicInteger walks = new AtomicInteger();
        AtomicBoolean stopped = new AtomicBoolean(true);
        ICreatureController creature = idleCreature(stateMachine(currentState), false, walks, stopped);

        CreatureState.IDLE.enter(creature);
        assertEquals(CreatureState.IDLE, currentState.get());
        assertEquals(0, walks.get());

        CreatureState.IDLE.update(creature);
        assertEquals(CreatureState.WANDER, currentState.get());

        stopped.set(false);
        CreatureState.WANDER.enter(creature);
        CreatureState.WANDER.update(creature);
        assertEquals(1, walks.get());
        assertEquals(CreatureState.WANDER, currentState.get());

        // On arriving it picks the next point straight away, as DK II does, instead of standing in IDLE
        stopped.set(true);
        CreatureState.WANDER.update(creature);
        assertEquals(CreatureState.WANDER, currentState.get());
        assertEquals(2, walks.get());
    }

    @Test
    void ownedCreatureWithNothingToDoWalksAroundInIdle() {
        AtomicReference<CreatureState> currentState = new AtomicReference<>(CreatureState.IDLE);
        AtomicInteger walks = new AtomicInteger();
        ICreatureController creature = idleCreature(stateMachine(currentState), true, walks, new AtomicBoolean(true));

        CreatureState.IDLE.enter(creature);
        CreatureState.IDLE.update(creature);

        assertEquals(CreatureState.IDLE, currentState.get());
        assertEquals(2, walks.get());
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

    private static ICreatureController idleCreature(
            StateMachine<ICreatureController, CreatureState> stateMachine,
            boolean claimed, AtomicInteger walks, AtomicBoolean stopped) {
        return (ICreatureController) Proxy.newProxyInstance(
                ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isClaimed" -> claimed;
                    case "findWork" -> {
                        ((Consumer<Boolean>) args[0]).accept(false);
                        yield null;
                    }
                    case "navigateToRandomPoint" -> walks.incrementAndGet();
                    case "isStopped" -> stopped.get();
                    case "isTimeToReEvaluate" -> true;
                    case "getStateMachine" -> stateMachine;
                    default -> defaultValue(method.getReturnType());
                });
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
