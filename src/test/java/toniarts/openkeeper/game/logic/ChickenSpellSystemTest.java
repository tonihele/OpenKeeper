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
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Morphed;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The Chicken spell wears off when its time is up
 */
class ChickenSpellSystemTest {

    private double gameTime = 100;

    @Test
    void chickenLastsUntilItsEndTime() {
        EntityData entityData = new DefaultEntityData();
        ChickenSpellSystem chickenSpellSystem = new ChickenSpellSystem(entityData, () -> gameTime);
        EntityId creature = entityData.createEntity();
        entityData.setComponent(creature, new Morphed(160));

        gameTime = 159.9;
        chickenSpellSystem.processTick(0.25f);
        assertNotNull(entityData.getComponent(creature, Morphed.class));

        gameTime = 160;
        chickenSpellSystem.processTick(0.25f);
        assertNull(entityData.getComponent(creature, Morphed.class));
    }

}
