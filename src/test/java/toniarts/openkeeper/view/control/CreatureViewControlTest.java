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
package toniarts.openkeeper.view.control;

import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.view.text.CreatureTextParser;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreatureViewControlTest {

    @Test
    void creatureOfAnotherKeeperHasNoTooltip() {
        DefaultEntityData entityData = new DefaultEntityData();
        EntityId creature = entityData.createEntity();
        entityData.setComponent(creature, new Owner((short) 4, (short) 4));

        CreatureViewControl control = new CreatureViewControl(creature, entityData, new Creature(), null, null, new CreatureTextParser(null));

        assertEquals("", control.getTooltip((short) 3));
    }

}
