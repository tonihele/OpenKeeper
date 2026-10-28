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
import com.simsilica.es.EntityId;
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

class CreatureAiSystemTest {

    private final EntityData entityData = new DefaultEntityData();
    private final List<EntityId> processed = new ArrayList<>();

    @Test
    void failingCreatureDoesNotStopTheOthers() {
        EntityId broken = createCreature();
        EntityId healthy = createCreature();
        EntityId healthyToo = createCreature();

        CreatureAiSystem system = new CreatureAiSystem(entityData, creaturesController(broken), taskManager());
        system.processTick(0.25f);

        assertEquals(List.of(healthy, healthyToo), processed);
    }

    private EntityId createCreature() {
        EntityId id = entityData.createEntity();
        entityData.setComponent(id, new CreatureAi(0, CreatureState.IDLE, (short) 1));
        return id;
    }

    private ICreaturesController creaturesController(EntityId broken) {
        return (ICreaturesController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> {
                    EntityId id = (EntityId) args[0];
                    return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ICreatureController.class},
                            (p, m, a) -> switch (m.getName()) {
                        case "getEntityId" -> id;
                        case "compareTo" -> id.compareTo(((ICreatureController) a[0]).getEntityId());
                        case "processTick" -> {
                            if (id.equals(broken)) {
                                throw new IllegalStateException("Test failure");
                            }
                            processed.add(id);
                            yield null;
                        }
                        default -> null;
                    });
                });
    }

    private ITaskManager taskManager() {
        return (ITaskManager) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{ITaskManager.class}, (proxy, method, args) -> null);
    }

}
