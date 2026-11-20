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

import com.simsilica.es.EntityData;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.task.ITaskManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A worker that finishes its job looks for the next one on the tick after, as
 * DK II's imps do in their idle state (0x463DF0).
 */
class CreatureAiSystemTest {

    @Test
    void workerThatWentIdleLooksForWorkOnTheNextTick() {
        EntityData entityData = new DefaultEntityData();
        entityData.setComponent(entityData.createEntity(), new CreatureAi(0, CreatureState.IDLE, (short) 1));
        int[] tick = {0};
        List<Integer> queued = new ArrayList<>();
        List<Integer> lookedForWork = new ArrayList<>();
        ITaskManager taskManager = (ITaskManager) Proxy.newProxyInstance(ITaskManager.class.getClassLoader(),
                new Class<?>[]{ITaskManager.class}, (proxy, method, args) -> {
                    if (method.getName().equals("processUnemployedWorkerQueue") && !queued.isEmpty()) {
                        queued.clear();
                        lookedForWork.add(tick[0]);
                    }
                    return null;
                });
        ICreatureController worker = (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> {

                    // The worker finishes its job on the first tick
                    if (method.getName().equals("processTick") && tick[0] == 1) {
                        queued.add(tick[0]);
                    }
                    return null;
                });
        ICreaturesController creaturesController = (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(),
                new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> method.getName().equals("createController") ? worker : null);
        CreatureAiSystem creatureAiSystem = new CreatureAiSystem(entityData, creaturesController, taskManager);

        for (tick[0] = 1; tick[0] <= 3; tick[0]++) {
            creatureAiSystem.processTick(0.25f);
        }

        assertEquals(List.of(2), lookedForWork);
    }

}
