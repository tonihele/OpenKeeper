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
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureSleep;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Player;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A creature that sleeps but has no lair object in the data, like the princes,
 * doesn't go looking for a lair
 */
class CreatureLairTest {

    @Test
    void keeperCreatureWithoutLairObjectDoesNotNeedALair() {
        EntityData entityData = new DefaultEntityData();
        EntityId entityId = entityData.createEntity();
        entityData.setComponent(entityId, new Owner(Player.KEEPER1_ID, Player.KEEPER1_ID));
        entityData.setComponent(entityId, new CreatureSleep(null, 0, 0, 0));

        // A new Creature has lair object 0, as the princes do in the data
        CreatureController prince = new CreatureController(entityId, entityData, new Creature(), null, null, null, null,
                null, null, null, null, null, null, null);

        assertFalse(prince.needsLair());
        assertFalse(prince.isNeedForSleep());
    }

}
