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
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Morphed;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.SpellTargetFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A creature under the Chicken spell does nothing but wander about until the
 * spell wears off
 */
@SuppressWarnings("unchecked")
class CreatureChickenTest {

    private static final double NOW = 100;

    private EntityData entityData;
    private EntityId entity;
    private CreatureController controller;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        entity = entityData.createEntity();
        entityData.setComponent(entity, new Owner((short) 3, (short) 3));
        controller = controller(SpellTargetFixtures.creature((short) 1));
    }

    @Test
    void workingCreatureTurnsIntoChickenForTheDuration() {
        controller.getStateMachine().setInitialState(CreatureState.WORK);

        controller.turnIntoChicken(60);

        Morphed morphed = entityData.getComponent(entity, Morphed.class);
        assertNotNull(morphed);
        assertEquals(NOW + 60, morphed.endTime, 0.0001);
        assertTrue(controller.isChicken());
        assertEquals(CreatureState.CHICKEN, controller.getStateMachine().getCurrentState());
    }

    @Test
    void immuneCreatureStaysAsItIs() {
        CreatureController immuneController = controller(SpellTargetFixtures.creature((short) 2, Creature.CreatureFlag.IS_IMMUNE_TO_CHICKEN));
        immuneController.getStateMachine().setInitialState(CreatureState.WORK);

        immuneController.turnIntoChicken(60);

        assertFalse(immuneController.isChicken());
        assertEquals(CreatureState.WORK, immuneController.getStateMachine().getCurrentState());
    }

    @Test
    void prisonerStaysInPrison() {
        controller.getStateMachine().setInitialState(CreatureState.IMPRISONED);

        controller.turnIntoChicken(60);

        assertTrue(controller.isChicken());
        assertEquals(CreatureState.IMPRISONED, controller.getStateMachine().getCurrentState());
    }

    @Test
    void possessedCreatureStaysPossessed() {
        controller.getStateMachine().setInitialState(CreatureState.IDLE);
        entityData.setComponent(entity, new Possessed(0, NOW));

        controller.turnIntoChicken(60);

        assertEquals(CreatureState.IDLE, controller.getStateMachine().getCurrentState());
    }

    @Test
    void chickenWandersUntilTheSpellWearsOff() {
        AtomicBoolean chicken = new AtomicBoolean(true);
        AtomicBoolean wandered = new AtomicBoolean();
        AtomicReference<CreatureState> state = new AtomicReference<>(CreatureState.CHICKEN);
        ICreatureController creature = creature(chicken, wandered, state);

        CreatureState.CHICKEN.update(creature);
        assertTrue(wandered.get());
        assertEquals(CreatureState.CHICKEN, state.get());

        chicken.set(false);
        CreatureState.CHICKEN.update(creature);
        assertEquals(CreatureState.IDLE, state.get());
    }

    @Test
    void idleChickenGoesBackToBeingAChicken() {
        AtomicReference<CreatureState> state = new AtomicReference<>(CreatureState.IDLE);
        ICreatureController creature = creature(new AtomicBoolean(true), new AtomicBoolean(), state);

        CreatureState.IDLE.enter(creature);

        assertEquals(CreatureState.CHICKEN, state.get());
    }

    @Test
    void notAChickenWithoutTheSpell() {
        assertFalse(controller.isChicken());
    }

    private CreatureController controller(Creature creature) {
        ILevelInfo levelInfo = (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(),
                new Class<?>[]{ILevelInfo.class}, (proxy, method, args) -> null);
        return new CreatureController(entity, entityData, creature, null, null, () -> NOW, null, null, null, null, levelInfo, null, null);
    }

    private static ICreatureController creature(AtomicBoolean chicken, AtomicBoolean wandered, AtomicReference<CreatureState> state) {
        StateMachine<ICreatureController, CreatureState> stateMachine = (StateMachine<ICreatureController, CreatureState>) Proxy.newProxyInstance(
                StateMachine.class.getClassLoader(), new Class<?>[]{StateMachine.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "changeState" -> {
                        state.set((CreatureState) args[0]);
                        yield null;
                    }
                    case "getCurrentState" -> state.get();
                    default -> defaultValue(method.getReturnType());
                });
        return (ICreatureController) Proxy.newProxyInstance(
                ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isChicken" -> chicken.get();
                    case "isStopped" -> true;
                    case "navigateToRandomPoint" -> {
                        wandered.set(true);
                        yield null;
                    }
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
