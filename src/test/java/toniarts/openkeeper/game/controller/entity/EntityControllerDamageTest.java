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
package toniarts.openkeeper.game.controller.entity;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.controller.creature.CreatureState;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The damage a creature takes depends on what it is doing, as in DK II
 */
class EntityControllerDamageTest {

    private final EntityData entityData = new DefaultEntityData();

    @Test
    void creatureOnItsFeetTakesTheDamageDealt() {
        assertEquals(10, damageTaken(creature(CreatureState.IDLE, false), 10));
    }

    @Test
    void possessedCreatureTakesHalf() {

        // Possession takes the creature's AI away
        EntityId entityId = entityData.createEntity();
        entityData.setComponent(entityId, new Possessed(0, 0));
        assertEquals(5, damageTaken(entityId, 10));
    }

    @Test
    void creatureLyingStunnedTakesDouble() {
        assertEquals(20, damageTaken(creature(CreatureState.FALLEN, false), 10));
        assertEquals(20, damageTaken(creature(CreatureState.GETTING_UP, false), 10));
    }

    @Test
    void possessedAndStunnedAddUp() {
        assertEquals(15, damageTaken(creature(CreatureState.FALLEN, true), 10));
    }

    @Test
    void healingIsNotScaled() {
        assertEquals(-10, damageTaken(creature(CreatureState.FALLEN, true), -10));
    }

    @Test
    void killingBlowDoesNotOverflow() {
        assertEquals(Integer.MAX_VALUE, damageTaken(creature(CreatureState.FALLEN, false), Integer.MAX_VALUE));
    }

    @Test
    void otherThingsTakeTheDamageDealt() {
        assertEquals(10, damageTaken(entityData.createEntity(), 10));
    }

    private EntityId creature(CreatureState state, boolean possessed) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponent(entityId, new CreatureAi(0, state, (short) 1));
        if (possessed) {
            entityData.setComponent(entityId, new Possessed(0, 0));
        }
        return entityId;
    }

    private int damageTaken(EntityId entityId, int damage) {
        EntityController.setDamage(entityData, entityId, damage);
        return entityData.getComponent(entityId, Damage.class).damage;
    }
}
