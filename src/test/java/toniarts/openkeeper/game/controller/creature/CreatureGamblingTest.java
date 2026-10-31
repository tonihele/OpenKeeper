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
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.task.ITaskManager;
import toniarts.openkeeper.tools.convert.map.CasinoVariableFixtures;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An idle creature wants to gamble when it holds more gold than the stake of
 * the Casino's money mode, as in the original
 */
@SuppressWarnings("unchecked")
class CreatureGamblingTest {

    private final EntityData entityData = new DefaultEntityData();
    private final Map<MiscType, MiscVariable> gameSettings = new EnumMap<>(MiscType.class);
    private int drinkAssignments = 0;

    @Test
    void creatureWithMoreGoldThanTheStakeGoesGambling() {
        assertTrue(creature(Player.KEEPER1_ID, false, 9).goGambling());
        assertEquals(1, drinkAssignments);
    }

    @Test
    void creatureWithTheStakeOrLessStaysAway() {
        assertFalse(creature(Player.KEEPER1_ID, false, 8).goGambling());
        assertEquals(0, drinkAssignments);
    }

    @Test
    void workersAndHeroesDoNotGamble() {
        assertFalse(creature(Player.KEEPER1_ID, true, 500).goGambling());
        assertFalse(creature(Player.GOOD_PLAYER_ID, false, 500).goGambling());
        assertEquals(0, drinkAssignments);
    }

    @Test
    void idleCreatureThatWantsToGambleStartsWorking() {
        AtomicReference<CreatureState> currentState = new AtomicReference<>(CreatureState.IDLE);
        StateMachine<ICreatureController, CreatureState> stateMachine = (StateMachine<ICreatureController, CreatureState>) Proxy.newProxyInstance(
                StateMachine.class.getClassLoader(), new Class<?>[]{StateMachine.class}, (proxy, method, args) -> {
                    if (method.getName().equals("changeState")) {
                        currentState.set((CreatureState) args[0]);
                    }
                    return method.getReturnType() == boolean.class ? Boolean.TRUE : null;
                });
        ICreatureController creature = (ICreatureController) Proxy.newProxyInstance(
                ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "goGambling" -> true;
                    case "getStateMachine" -> stateMachine;
                    default -> method.getReturnType() == boolean.class ? Boolean.FALSE : null;
                });

        CreatureState.IDLE.enter(creature);

        assertEquals(CreatureState.WORK, currentState.get());
    }

    private ICreatureController creature(short owner, boolean worker, int gold) {
        gameSettings.put(MiscType.GAMBLING_MODIFY_CREATURE_MONEY_MONEY_PER_SECOND,
                CasinoVariableFixtures.miscVariable(MiscType.GAMBLING_MODIFY_CREATURE_MONEY_MONEY_PER_SECOND, -8));
        EntityId id = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.worker = worker;
        entityData.setComponents(id, creatureComponent, new Owner(owner, owner), new Gold(gold, 1000));
        ITaskManager taskManager = (ITaskManager) Proxy.newProxyInstance(
                ITaskManager.class.getClassLoader(), new Class<?>[]{ITaskManager.class}, (proxy, method, args) -> {
                    if (method.getName().equals("assignTask") && args[1] == Creature.JobType.DRINK) {
                        drinkAssignments++;
                        return true;
                    }
                    return method.getReturnType() == boolean.class ? Boolean.FALSE : null;
                });
        return new CreatureController(id, entityData, null, null, taskManager, null, gameSettings, null, null, null, null, null, null, null);
    }
}
