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
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("unchecked")
class CombatPitStateTest {

    @Test
    void idlePitFighterWaitsInThePit() {
        assertEquals(CreatureState.COMBAT_PIT, update(CreatureState.IDLE, true, false));
    }

    @Test
    void pitFighterWithAnOpponentFights() {
        assertEquals(CreatureState.FIGHT, update(CreatureState.COMBAT_PIT, true, true));
    }

    @Test
    void pitFighterWithoutOpponentGoesBackToWaiting() {
        assertEquals(CreatureState.COMBAT_PIT, update(CreatureState.FIGHT, true, false));
    }

    @Test
    void creatureTakenOutOfThePitStopsWaiting() {
        assertEquals(CreatureState.IDLE, update(CreatureState.COMBAT_PIT, false, false));
    }

    private static CreatureState update(CreatureState state, boolean pitFighter, boolean hasOpponent) {
        AtomicReference<CreatureState> currentState = new AtomicReference<>(state);
        StateMachine<ICreatureController, CreatureState> stateMachine = (StateMachine<ICreatureController, CreatureState>) Proxy.newProxyInstance(
                StateMachine.class.getClassLoader(), new Class<?>[]{StateMachine.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "changeState" -> {
                        currentState.set((CreatureState) args[0]);
                        yield null;
                    }
                    case "getCurrentState" -> currentState.get();
                    default -> defaultValue(method.getReturnType());
                });
        ICreatureController opponent = (ICreatureController) Proxy.newProxyInstance(
                ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class},
                (proxy, method, args) -> defaultValue(method.getReturnType()));
        ICreatureController creature = (ICreatureController) Proxy.newProxyInstance(
                ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isCombatPitFighter" -> pitFighter;
                    case "getAttackTarget" -> hasOpponent ? opponent : null;
                    case "getStateMachine" -> stateMachine;
                    default -> defaultValue(method.getReturnType());
                });

        state.update(creature);

        return currentState.get();
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
