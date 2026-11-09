/*
 * Copyright (C) 2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper. If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.tools.convert.map;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.controller.creature.CreatureController;
import toniarts.openkeeper.game.task.ITaskManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Like DKII, a creature that looks for a job and finds none gets angry for the
 * lack of work.
 */
class CreatureNoWorkAngerTest {

    @Test
    void creatureWithNoJobToDoGetsAngry() {
        EntityData entityData = new DefaultEntityData();
        EntityId entityId = createCreature(entityData);
        AtomicBoolean foundWork = new AtomicBoolean(true);

        controller(entityData, entityId, false).findWork(foundWork::set);

        assertFalse(foundWork.get());
        assertEquals(50 / 4, entityData.getComponent(entityId, CreatureMood.class).noWork);
    }

    @Test
    void creatureThatFindsAJobStaysCalm() {
        EntityData entityData = new DefaultEntityData();
        EntityId entityId = createCreature(entityData);
        AtomicBoolean foundWork = new AtomicBoolean(false);

        controller(entityData, entityId, true).findWork(foundWork::set);

        assertTrue(foundWork.get());
        assertEquals(0, entityData.getComponent(entityId, CreatureMood.class).noWork);
    }

    private static EntityId createCreature(EntityData entityData) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponent(entityId, new Owner(Player.KEEPER1_ID, Player.KEEPER1_ID));
        entityData.setComponent(entityId, new CreatureComponent());
        entityData.setComponent(entityId, new CreatureMood());
        return entityId;
    }

    private static CreatureController controller(EntityData entityData, EntityId entityId, boolean jobAvailable) {
        Creature.JobPreference research = new Creature.JobPreference();
        research.setJobType(Creature.JobType.RESEARCH);
        research.setChance((short) 100);
        Creature mistress = new Creature();
        mistress.setHappyJobs(new Creature.JobPreference[]{research});
        mistress.getAttributes().setAngerNoWork((short) 50);

        ITaskManager taskManager = (ITaskManager) Proxy.newProxyInstance(ITaskManager.class.getClassLoader(),
                new Class<?>[]{ITaskManager.class}, (proxy, method, args) -> switch (method.getName()) {
            case "isTaskAvailable", "assignTask" -> jobAvailable;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        return new CreatureController(entityId, entityData, mistress, null, taskManager, null, null,
                null, null, null, null, null, null, null);
    }

}
