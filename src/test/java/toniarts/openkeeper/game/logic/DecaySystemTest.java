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
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Decay;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Position;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class DecaySystemTest {

    private static final float TICK = 0.25f;

    private final EntityData entityData = new DefaultEntityData();
    private final DecaySystem system = new DecaySystem(entityData);

    @Test
    void decaysWhenItsTimeIsUp() {
        EntityId entity = decaying(1);

        tick(3);
        assertNotNull(entityData.getComponent(entity, Decay.class));
        tick(1);
        assertNull(entityData.getComponent(entity, Decay.class));
        assertNull(entityData.getComponent(entity, Position.class));
    }

    @Test
    void doesNotDecayInTheKeepersHand() {
        EntityId entity = decaying(1);
        entityData.removeComponent(entity, Position.class);

        tick(8);
        assertNotNull(entityData.getComponent(entity, Decay.class));

        // Dropped again, its time has been up for a while
        entityData.setComponent(entity, new Position(0, new Vector3f(2, 0, 2)));
        tick(1);
        assertNull(entityData.getComponent(entity, Decay.class));
    }

    @Test
    void keepsDecayingWhileItMoves() {
        EntityId entity = decaying(1);
        entityData.setComponent(entity, new Health(10, 10));

        for (int i = 0; i < 4; i++) {
            entityData.setComponent(entity, new Position(0, new Vector3f(i, 0, 1)));
            tick(1);
        }
        assertNull(entityData.getComponent(entity, Decay.class));
        assertEquals(20, entityData.getComponent(entity, Damage.class).damage);
    }

    private EntityId decaying(double duration) {
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity, new Decay(0, duration), new Position(0, new Vector3f(1, 0, 1)));
        return entity;
    }

    private void tick(int ticks) {
        for (int i = 0; i < ticks; i++) {
            system.processTick(TICK);
        }
    }

}
