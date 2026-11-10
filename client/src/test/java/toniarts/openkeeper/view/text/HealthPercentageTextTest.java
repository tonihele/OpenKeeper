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
package toniarts.openkeeper.view.text;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.utils.TextParameter;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The health percentage of a tooltip, e.g. a door that has been hit
 */
class HealthPercentageTextTest {

    private final EntityData entityData = new DefaultEntityData();

    @Test
    void showsTheShareOfHealthLeftOfADamagedEntity() {
        assertEquals("75", percentage(1500, 2000));
        assertEquals("1", percentage(20, 2000));
    }

    @Test
    void showsHundredAtFullHealthAndZeroAtNone() {
        assertEquals("100", percentage(2000, 2000));
        assertEquals("0", percentage(0, 2000));
    }

    private String percentage(int health, int maxHealth) {
        EntityId id = entityData.createEntity();
        entityData.setComponent(id, new Health(health, maxHealth));
        Entity entity = entityData.getEntity(id, Health.class);
        return new DoorTextParser().getReplacement(TextParameter.HEALTH_PERCENTAGE, entity, null);
    }

}
