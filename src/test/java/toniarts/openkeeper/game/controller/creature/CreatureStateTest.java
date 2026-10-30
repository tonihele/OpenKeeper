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
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.game.controller.entity.IEntityController;

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
                        piles.add((Integer) args[3]);
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
