/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.game.controller.creature;

import com.badlogic.gdx.ai.fsm.StateMachine;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
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

    private static void assertMissingLairChangesStateToIdle(CreatureState state) {
        AtomicReference<CreatureState> currentState = new AtomicReference<>(state);
        StateMachine<ICreatureController, CreatureState> stateMachine = (StateMachine<ICreatureController, CreatureState>) Proxy.newProxyInstance(
                StateMachine.class.getClassLoader(), new Class<?>[]{StateMachine.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "changeState" -> {
                        currentState.set((CreatureState) args[0]);
                        yield true;
                    }
                    case "getCurrentState" -> currentState.get();
                    default -> defaultValue(method.getReturnType());
                });
        ICreatureController creature = (ICreatureController) Proxy.newProxyInstance(
                ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "hasLair" -> false;
                    case "getStateMachine" -> stateMachine;
                    default -> defaultValue(method.getReturnType());
                });

        state.update(creature);

        assertEquals(CreatureState.IDLE, currentState.get());
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
