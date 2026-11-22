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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Turncoat;
import toniarts.openkeeper.game.component.Unconscious;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Turncoat wears off when its time is up, or when the creature is knocked out
 */
class TurncoatSystemTest {

    private EntityData entityData;
    private double gameTime;
    private TurncoatSystem turncoatSystem;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        gameTime = 100;
        turncoatSystem = new TurncoatSystem(entityData, () -> gameTime);
    }

    @Test
    void turncoatLastsUntilItsEndTime() {
        EntityId creature = entityData.createEntity();
        entityData.setComponent(creature, new Turncoat((short) 3, 125));

        gameTime = 124.9;
        turncoatSystem.processTick(0.25f);
        assertNotNull(entityData.getComponent(creature, Turncoat.class));

        gameTime = 125;
        turncoatSystem.processTick(0.25f);
        assertNull(entityData.getComponent(creature, Turncoat.class));
    }

    @Test
    void knockedOutCreatureIsNoLongerTurned() {
        EntityId creature = entityData.createEntity();
        entityData.setComponent(creature, new Turncoat((short) 3, 125));
        entityData.setComponent(creature, new Unconscious(gameTime));

        turncoatSystem.processTick(0.25f);

        assertNull(entityData.getComponent(creature, Turncoat.class));
    }

}
