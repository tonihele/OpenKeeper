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
package toniarts.openkeeper.view;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.Position;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatureLevelUpContainerTest {

    private DefaultEntityData entityData;
    private List<EntityId> levelUps;
    private CreatureLevelUpContainer container;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        levelUps = new ArrayList<>();
        container = new CreatureLevelUpContainer(entityData, e -> levelUps.add(e.getId()));
        container.start();
    }

    @AfterEach
    void tearDown() {
        container.stop();
    }

    private EntityId createCreature(int level) {
        EntityId id = entityData.createEntity();
        entityData.setComponents(id, new Position(0, new Vector3f()), experience(level, 0));
        return id;
    }

    private static CreatureExperience experience(int level, int experience) {
        return new CreatureExperience(level, experience, 100, 1, 2);
    }

    @Test
    void creatureComingOntoTheMapDidNotLevelUp() {
        createCreature(5);
        container.update();

        assertTrue(levelUps.isEmpty());
    }

    @Test
    void gainingALevelIsALevelUp() {
        EntityId id = createCreature(1);
        container.update();

        entityData.setComponent(id, experience(2, 0));
        container.update();

        assertEquals(List.of(id), levelUps);
    }

    @Test
    void gainingExperienceOnlyIsNoLevelUp() {
        EntityId id = createCreature(1);
        container.update();

        entityData.setComponent(id, experience(1, 50));
        entityData.setComponent(id, new Position(1, new Vector3f(1, 0, 1)));
        container.update();

        assertTrue(levelUps.isEmpty());
    }

    @Test
    void gainingSeveralLevelsAtOnceIsOneLevelUp() {
        EntityId id = createCreature(1);
        container.update();

        entityData.setComponent(id, experience(10, 0));
        container.update();
        entityData.setComponent(id, experience(10, 20));
        container.update();

        assertEquals(List.of(id), levelUps);
    }

    @Test
    void droppedCreatureDidNotLevelUp() {
        EntityId id = createCreature(1);
        container.update();

        // Picked up and dropped again with a higher level
        entityData.removeComponent(id, Position.class);
        container.update();
        entityData.setComponents(id, new Position(0, new Vector3f()), experience(3, 0));
        container.update();

        assertTrue(levelUps.isEmpty());
    }
}
