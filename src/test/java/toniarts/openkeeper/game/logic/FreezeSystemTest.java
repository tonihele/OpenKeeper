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
package toniarts.openkeeper.game.logic;

import com.badlogic.gdx.ai.fsm.StateMachine;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Frozen;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FreezeSystemTest {

    private final EntityData entityData = new DefaultEntityData();
    private final List<String> calls = new ArrayList<>();
    private CreatureState state = CreatureState.IDLE;
    private double gameTime = 0;
    private FreezeSystem freezeSystem;

    @Test
    void frozenCreatureStopsWhereItIs() {
        EntityId creature = addCreature();
        entityData.setComponent(creature, new Frozen(0, 20));

        tick();

        assertEquals(List.of("unassingCurrentTask", "stopCreature", "changeState STUNNED"), calls);
    }

    @Test
    void creatureThawsAfterTheFreezeDuration() {
        EntityId creature = addCreature();
        entityData.setComponent(creature, new Frozen(0, 20));
        tick();
        calls.clear();

        gameTime = 19.75;
        tick();
        assertNotNull(entityData.getComponent(creature, Frozen.class));

        gameTime = 20;
        tick();
        tick();
        assertNull(entityData.getComponent(creature, Frozen.class));
        assertEquals(List.of("changeState IDLE"), calls);
    }

    @Test
    void thawingLeavesOtherStatesAlone() {
        EntityId creature = addCreature();
        entityData.setComponent(creature, new Frozen(0, 20));
        tick();
        calls.clear();

        // Knocked out while frozen
        state = CreatureState.UNCONSCIOUS;
        gameTime = 20;
        tick();
        tick();

        assertEquals(List.of(), calls);
    }

    @Test
    void deadCreatureIsNotThawed() {
        EntityId creature = addCreature();
        entityData.setComponent(creature, new Frozen(0, 20));
        tick();
        calls.clear();

        entityData.removeEntity(creature);
        tick();

        assertEquals(List.of(), calls);
    }

    private void tick() {
        if (freezeSystem == null) {
            freezeSystem = new FreezeSystem(entityData, creaturesController(), () -> gameTime);
        }
        freezeSystem.processTick(0.25f);
    }

    private EntityId addCreature() {
        EntityId entityId = entityData.createEntity();
        entityData.setComponent(entityId, new CreatureComponent());
        entityData.setComponent(entityId, new CreatureAi(0, CreatureState.IDLE, (short) 1));
        return entityId;
    }

    @SuppressWarnings("unchecked")
    private ICreaturesController creaturesController() {
        StateMachine<ICreatureController, CreatureState> stateMachine = (StateMachine<ICreatureController, CreatureState>) Proxy.newProxyInstance(
                StateMachine.class.getClassLoader(), new Class<?>[]{StateMachine.class}, (proxy, method, args) -> switch (method.getName()) {
            case "changeState" -> {
                state = (CreatureState) args[0];
                calls.add("changeState " + state);
                yield null;
            }
            case "isInState" ->
                state == args[0];
            case "getCurrentState" ->
                state;
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
        ICreatureController creatureController = (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getStateMachine" ->
                stateMachine;
            case "stopCreature", "unassingCurrentTask" -> {
                calls.add(method.getName());
                yield null;
            }
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
        return (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(),
                new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> {
                    if (method.getName().equals("createController")) {
                        return creatureController;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

}
