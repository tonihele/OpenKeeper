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

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreatureViewSystemTest {

    @Test
    void bileDemonUsesItsOwnWorkshopAnimation() {
        Creature bileDemon = CreatureFixtures.creatureWithAnimation(Creature.AnimationType.MANUFACTURING,
                ArtResource.ArtResourceType.ANIMATING_MESH);

        assertEquals(Creature.AnimationType.MANUFACTURING, CreatureViewSystem.getManufactureAnimation(bileDemon));
    }

    @Test
    void trollSwingsItsHammer() {

        // The Troll's workshop slot is empty in the data
        Creature troll = CreatureFixtures.creatureWithAnimation(Creature.AnimationType.MANUFACTURING,
                ArtResource.ArtResourceType.NONE);

        assertEquals(Creature.AnimationType.MELEE_ATTACK, CreatureViewSystem.getManufactureAnimation(troll));
    }

    @Test
    void missingSlotFallsBackToTheHammer() {
        Creature creature = CreatureFixtures.creatureWithAnimation(Creature.AnimationType.IDLE_1,
                ArtResource.ArtResourceType.ANIMATING_MESH);

        assertEquals(Creature.AnimationType.MELEE_ATTACK, CreatureViewSystem.getManufactureAnimation(creature));
    }

}
