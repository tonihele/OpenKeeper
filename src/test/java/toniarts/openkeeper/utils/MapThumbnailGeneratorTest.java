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
package toniarts.openkeeper.utils;

import java.awt.Color;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.Player;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The keeper colours of flowers, effects and the lobby.
 */
class MapThumbnailGeneratorTest {

    @Test
    void keepersTakeTheColoursOfTheirClaimedTiles() {
        // Claimed paths and walls go red, blue, green, yellow, purple
        assertEquals(new Color(230, 50, 50), MapThumbnailGenerator.getPlayerColor(Player.KEEPER1_ID));
        assertEquals(new Color(50, 78, 230), MapThumbnailGenerator.getPlayerColor(Player.KEEPER2_ID));
        assertEquals(new Color(50, 230, 51), MapThumbnailGenerator.getPlayerColor(Player.KEEPER3_ID));
        assertEquals(new Color(226, 230, 50), MapThumbnailGenerator.getPlayerColor(Player.KEEPER4_ID));
        assertEquals(new Color(230, 50, 212), MapThumbnailGenerator.getPlayerColor(Player.KEEPER5_ID));
    }
}
