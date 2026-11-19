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
package toniarts.openkeeper.game.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The Player Information window's Map Area is the share of the map's tiles a
 * keeper owns
 */
class MapOwnedPercentageTest {

    @Test
    void ownedTilesOutOfAllTilesRoundedDown() {
        FakeMapData map = new FakeMapData(10, 6);
        for (int x = 0; x < 10; x++) {
            map.getTile(x, 0).ownerId = 3;
            map.getTile(x, 1).ownerId = 3;
        }
        map.getTile(0, 2).ownerId = 3;
        map.getTile(1, 2).ownerId = 4;

        assertEquals(35, map.getOwnedPercentage((short) 3)); // 21 of 60 tiles
        assertEquals(1, map.getOwnedPercentage((short) 4));
        assertEquals(0, map.getOwnedPercentage((short) 5));
    }
}
