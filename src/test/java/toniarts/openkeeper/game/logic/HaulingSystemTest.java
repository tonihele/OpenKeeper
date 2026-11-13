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

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.HauledBy;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.task.TaskType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * An Imp carries a knocked out creature only while it is on the job of
 * carrying it
 */
class HaulingSystemTest {

    private final EntityData entityData = new DefaultEntityData();
    private final HaulingSystem haulingSystem = new HaulingSystem(entityData);
    private final EntityId imp = entityData.createEntity();
    private final EntityId creature = entityData.createEntity();

    HaulingSystemTest() {
        entityData.setComponent(imp, new Position(0, new Vector3f(5, 1, 5)));
        entityData.setComponents(creature, new Position(0, new Vector3f(2, 1, 2)), new Unconscious(0),
                new HauledBy(imp));
    }

    @Test
    void impOnTheJobCarriesTheCreature() {
        entityData.setComponent(imp, new TaskComponent(1, creature, null, TaskType.CARRY_CREATURE_TO_JAIL, 0, true));

        haulingSystem.processTick(0.25f);

        assertNotNull(entityData.getComponent(creature, HauledBy.class));
        assertEquals(5, entityData.getComponent(creature, Position.class).position.x);
    }

    @Test
    void impSwitchingJobsKeepsTheCreature() {
        entityData.setComponent(imp, new TaskComponent(1, creature, null, TaskType.CAPTURE_ENEMY_CREATURE, 0, true));

        // Like CreatureController.setAssignedTask, the old task goes and the new one comes on the same logic tick
        entityData.removeComponent(imp, TaskComponent.class);
        entityData.setComponent(imp, new TaskComponent(2, creature, null, TaskType.CARRY_CREATURE_TO_JAIL, 0, false));
        haulingSystem.processTick(0.25f);

        assertNotNull(entityData.getComponent(creature, HauledBy.class));
        assertEquals(5, entityData.getComponent(creature, Position.class).position.x);
    }

    @Test
    void impThatLeftTheJobPutsTheCreatureDown() {

        // An Imp that has dropped the job, like one that runs away, can't keep holding the creature. It would
        // never bleed out, and no Imp would take it to a Prison or a lair
        haulingSystem.processTick(0.25f);

        assertNull(entityData.getComponent(creature, HauledBy.class));
        assertEquals(2, entityData.getComponent(creature, Position.class).position.x);
    }
}
