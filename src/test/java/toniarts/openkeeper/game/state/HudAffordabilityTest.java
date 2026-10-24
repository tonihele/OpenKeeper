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
package toniarts.openkeeper.game.state;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.HudIconFixtures;
import toniarts.openkeeper.tools.convert.map.Room;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A room icon is darkened while the keeper's gold doesn't cover one tile of
 * the room
 */
class HudAffordabilityTest {

    private final Room temple = HudIconFixtures.room("Temple", 3000);

    @Test
    void roomIsLitWithGoldForOneTile() {
        assertTrue(HudAffordability.canAffordRoom(temple, 3000));
    }

    @Test
    void roomIsDarkWithoutGoldForOneTile() {
        assertFalse(HudAffordability.canAffordRoom(temple, 2999));
    }

}
