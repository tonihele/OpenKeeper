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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Threat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreatSourceTest {

    private final EntityData entityData = new DefaultEntityData();
    private EntityId imp;
    private EntityId hero;

    @BeforeEach
    void setUp() {
        imp = entityData.createEntity();
        hero = entityData.createEntity();
        entityData.setComponent(imp, new CreatureComponent());
        entityData.setComponent(hero, new CreatureComponent());
    }

    @Test
    void anEnemyThatHasNotChosenUsDoesNotScareUs() {
        assertFalse(ThreatSource.isThreateningUs(entityData, hero, imp));
    }

    @Test
    void anEnemyFightingSomeoneElseDoesNotScareUs() {
        entityData.setComponent(hero, new AttackTarget(entityData.createEntity()));

        assertFalse(ThreatSource.isThreateningUs(entityData, hero, imp));
    }

    @Test
    void anEnemyThatFightsUsScaresUs() {
        entityData.setComponent(hero, new AttackTarget(imp));

        assertTrue(ThreatSource.isThreateningUs(entityData, hero, imp));
    }

    @Test
    void aTrapWithAThreatScaresWithoutFighting() {
        EntityId trap = entityData.createEntity();
        entityData.setComponent(trap, new Threat(74));

        assertTrue(ThreatSource.isThreateningUs(entityData, trap, imp));
    }

}
