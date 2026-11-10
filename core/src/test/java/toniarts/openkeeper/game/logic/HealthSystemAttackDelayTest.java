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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMeleeAttack;
import toniarts.openkeeper.game.component.CreatureSpell;
import toniarts.openkeeper.game.component.CreatureSpells;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A creature other than a worker below a fifth of its health attacks and casts
 * at half speed: DK II pushes its last melee and spell times on by a tick every
 * other tick (DKII.EXE 0x488930)
 */
class HealthSystemAttackDelayTest {

    private final EntityData entityData = new DefaultEntityData();

    @Test
    void badlyHurtCreatureAttacksAtHalfSpeed() {
        EntityId hurt = creature(19, false);
        EntityId healthy = creature(20, false);
        EntityId worker = creature(19, true);
        EntityId spell = entityData.createEntity();
        entityData.setComponent(spell, new CreatureSpell(new CreatureSpell((short) 1, hurt, 2, 3), 0));
        entityData.setComponent(hurt, new CreatureSpells(List.of(spell)));

        // Two seconds at 4 ticks a second
        HealthSystem healthSystem = new HealthSystem(entityData, null, settings(), null, null, List.of(), null, () -> 0, null);
        for (int tick = 0; tick < 8; tick++) {
            healthSystem.processTick(0.25f);
        }

        assertEquals(1.0, entityData.getComponent(hurt, CreatureMeleeAttack.class).attactStartTime, 0.0001);
        assertEquals(1.0, entityData.getComponent(spell, CreatureSpell.class).attactStartTime, 0.0001);
        assertEquals(0.0, entityData.getComponent(healthy, CreatureMeleeAttack.class).attactStartTime, 0.0001);
        assertEquals(0.0, entityData.getComponent(worker, CreatureMeleeAttack.class).attactStartTime, 0.0001);
    }

    private EntityId creature(int health, boolean worker) {
        EntityId entity = entityData.createEntity();
        CreatureComponent creature = new CreatureComponent();
        creature.worker = worker;
        entityData.setComponents(entity, creature, new Health(health, 100),
                new CreatureMeleeAttack(new CreatureMeleeAttack(1, 10, 1, 1), 0));
        return entity;
    }

    private static Map<MiscType, MiscVariable> settings() {
        Map<MiscType, MiscVariable> settings = new EnumMap<>(MiscType.class);
        for (MiscType type : List.of(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS,
                MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND)) {
            settings.put(type, CreatureSleepFixtures.variable(type, 0));
        }
        return settings;
    }
}
