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

import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.HealthGain;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.tools.convert.map.VariableFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A trap gains health back every second, on a fixed beat (DKII.EXE's trap tick
 * 0x4A84E0)
 */
class TrapHealthGainTest {

    private static final int MAX_HEALTH = 100;
    private static final int HEALTH_GAIN = 5;
    private static final float TICK = 0.25f;

    private final DefaultEntityData entityData = new DefaultEntityData();
    private HealthSystem healthSystem;
    private EntityId trap;

    @BeforeEach
    void setUp() {
        healthSystem = new HealthSystem(entityData, null, Map.of(
                MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, VariableFixtures.miscVariable(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, 10),
                MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, VariableFixtures.miscVariable(MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, 0),
                MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND, VariableFixtures.miscVariable(MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND, 0)),
                null, null, List.of(), null);
        trap = entityData.createEntity();
        entityData.setComponents(trap, new Health(80, MAX_HEALTH), new HealthGain(HEALTH_GAIN, null));
    }

    @AfterEach
    void tearDown() {
        healthSystem.stop();
        entityData.close();
    }

    @Test
    void damagedTrapGainsItsHealthGainEverySecond() {
        runSeconds(1);
        assertEquals(80, healthOf(trap));

        runSeconds(1);
        assertEquals(80 + HEALTH_GAIN, healthOf(trap));

        runSeconds(1);
        assertEquals(80 + 2 * HEALTH_GAIN, healthOf(trap));
    }

    @Test
    void trapNeverGainsMoreThanItsMaxHealth() {
        entityData.setComponent(trap, new Health(MAX_HEALTH - 2, MAX_HEALTH));

        runSeconds(3);

        assertEquals(MAX_HEALTH, healthOf(trap));
    }

    @Test
    void hitDoesNotHoldUpTheNextGain() {
        runSeconds(1);

        // A hit in the very tick the gain is due
        entityData.setComponent(trap, new Damage(10));
        runTicks(1);

        assertEquals(80 - 10 + HEALTH_GAIN, healthOf(trap));
    }

    private void runSeconds(int seconds) {
        runTicks((int) (seconds / TICK));
    }

    private void runTicks(int ticks) {
        for (int i = 0; i < ticks; i++) {
            healthSystem.processTick(TICK);
        }
    }

    private int healthOf(EntityId entityId) {
        return entityData.getComponent(entityId, Health.class).health;
    }

}
