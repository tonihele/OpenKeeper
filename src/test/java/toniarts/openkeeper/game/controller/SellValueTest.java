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
package toniarts.openkeeper.game.controller;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SellValueTest {

    @Test
    void roomTileGivesBackItsShareOfTheCost() {

        // ROOM_SELL_VALUE_PERCENTAGE_OF_COST is 50 in the shipped data
        assertEquals(375, SellValue.ofRoomTile(RoomFixtures.room((short) 1, 750), 50));
        assertEquals(0, SellValue.ofRoomTile(RoomFixtures.room((short) 1, 750), 0));
    }

    @Test
    void oddCostsRoundDown() {
        assertEquals(37, SellValue.ofRoomTile(RoomFixtures.room((short) 1, 75), 50));
    }

}
