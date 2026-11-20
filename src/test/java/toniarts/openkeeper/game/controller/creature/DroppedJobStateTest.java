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
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An idle creature that the keeper dropped into a room does the room's job
 * before looking for other work
 */
@SuppressWarnings("unchecked")
class DroppedJobStateTest {

    @Test
    void idleCreatureDoesItsDroppedJob() {
        List<String> calls = new ArrayList<>();
        AtomicReference<CreatureState> currentState = new AtomicReference<>();

        CreatureState.IDLE.enter(creature(true, calls, currentState));

        assertEquals(CreatureState.WORK, currentState.get());
        assertTrue(calls.contains("doDroppedJob"));
        assertFalse(calls.contains("findWork"));
    }

    @Test
    void idleCreatureWithoutDroppedJobLooksForWork() {
        List<String> calls = new ArrayList<>();
        AtomicReference<CreatureState> currentState = new AtomicReference<>();

        CreatureState.IDLE.enter(creature(false, calls, currentState));

        assertTrue(calls.contains("findWork"));
    }

    private static ICreatureController creature(boolean droppedJob, List<String> calls, AtomicReference<CreatureState> currentState) {
        StateMachine<ICreatureController, CreatureState> stateMachine = (StateMachine<ICreatureController, CreatureState>) Proxy.newProxyInstance(
                StateMachine.class.getClassLoader(), new Class<?>[]{StateMachine.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "changeState" -> {
                        currentState.set((CreatureState) args[0]);
                        yield true;
                    }
                    case "getCurrentState" -> currentState.get();
                    default -> defaultValue(method.getReturnType());
                });
        return (ICreatureController) Proxy.newProxyInstance(
                ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> {
                    calls.add(method.getName());
                    return switch (method.getName()) {
                        case "doDroppedJob" -> droppedJob;
                        case "getStateMachine" -> stateMachine;
                        default -> defaultValue(method.getReturnType());
                    };
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
