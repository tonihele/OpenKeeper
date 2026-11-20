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
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.CreaturePay;
import toniarts.openkeeper.game.component.Gold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The keeper pays a creature by dropping gold on it. The creature keeps half
 * of the gold, is owed nothing more and stops being angry about its pay
 */
class CreatureTakesPayTest {

    private final EntityData entityData = new DefaultEntityData();

    @Test
    void creatureKeepsHalfAndIsOwedNothingMore() {
        EntityId entityId = creature(100, 300);
        entityData.setComponent(entityId, new CreaturePay(500, 2, 0, false));
        entityData.setComponent(entityId, new CreatureMood(7, 0, 0, 0, 900, 0));

        controller(entityId).takePay(250);

        assertEquals(225, entityData.getComponent(entityId, Gold.class).gold);
        assertNull(entityData.getComponent(entityId, CreaturePay.class));
        CreatureMood mood = entityData.getComponent(entityId, CreatureMood.class);
        assertEquals(0, mood.get(CreatureMood.REASON_NO_PAY));
        assertEquals(7, mood.get(CreatureMood.REASON_GENERAL));
    }

    @Test
    void creatureKeepsNoMoreThanItCanCarry() {
        EntityId entityId = creature(100, 300);

        controller(entityId).takePay(2000);

        assertEquals(300, entityData.getComponent(entityId, Gold.class).gold);
    }

    @Test
    void creatureThatSkipsTheNextPayDayStillSkipsIt() {
        EntityId entityId = creature(0, 300);
        entityData.setComponent(entityId, new CreaturePay(0, 0, 0, true));

        controller(entityId).takePay(100);

        assertNotNull(entityData.getComponent(entityId, CreaturePay.class));
    }

    private EntityId creature(int gold, int maxGold) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponent(entityId, new Gold(gold, maxGold));
        return entityId;
    }

    private CreatureController controller(EntityId entityId) {
        return new CreatureController(entityId, entityData, null, null, null, null, Map.of(), null, null, null, null,
                null, null, null);
    }

}
