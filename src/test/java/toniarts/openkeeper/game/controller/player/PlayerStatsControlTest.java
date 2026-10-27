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
package toniarts.openkeeper.game.controller.player;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerStatsControlTest {

    private final Keeper keeper = new Keeper(false, (short) 3);
    private final PlayerStatsControl stats = new PlayerStatsControl(keeper);

    @Test
    void countsHeroesSeparatelyFromOtherEnemies() {
        Creature knight = ShotFixtures.creature((short) 1, EnumSet.of(Creature.CreatureFlag.IS_GOOD), EnumSet.noneOf(Creature.CreatureFlag3.class));
        Creature troll = ShotFixtures.creature((short) 2, EnumSet.of(Creature.CreatureFlag.IS_EVIL), EnumSet.noneOf(Creature.CreatureFlag3.class));

        stats.creatureKilled(knight);
        stats.creatureKilled(troll);
        stats.creatureKilled(troll);

        assertEquals(1, stats.getHeroesDestroyed());
        assertEquals(2, stats.getEnemyCreaturesKilled());
        assertEquals(1, keeper.getHeroesDestroyed());
        assertEquals(2, keeper.getEnemyCreaturesKilled());
    }

    @Test
    void countsSpecialsFound() {
        stats.specialFound();
        stats.specialFound();

        assertEquals(2, stats.getSpecialsFound());
    }
}
