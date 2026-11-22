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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static toniarts.openkeeper.game.state.CheatState.CheatType.CLAIM_LAND;
import static toniarts.openkeeper.game.state.CheatState.CheatType.GOD_MODE;
import static toniarts.openkeeper.game.state.CheatState.CheatType.INSTANT_DIG;
import static toniarts.openkeeper.game.state.CheatState.CheatType.KILL_CREATURES;
import static toniarts.openkeeper.game.state.CheatState.CheatType.REMOVE_FOW;
import static toniarts.openkeeper.game.state.CheatState.CheatType.SPAWN_CREATURES;
import static toniarts.openkeeper.game.state.CheatState.CheatType.TERRAIN_CHANGE;

class CheatStateTest {

    @Test
    void mapInteractionModesAreExclusive() {
        assertTrue(CLAIM_LAND.isExclusiveMode());
        assertTrue(INSTANT_DIG.isExclusiveMode());
        assertTrue(TERRAIN_CHANGE.isExclusiveMode());
        assertTrue(SPAWN_CREATURES.isExclusiveMode());
    }

    @Test
    void clickCheatsAreNotExclusive() {
        assertFalse(KILL_CREATURES.isExclusiveMode());
        assertFalse(GOD_MODE.isExclusiveMode());
    }

    @Test
    void fogOfWarAndGodModeAreExemptFromExclusiveModes() {
        assertTrue(GOD_MODE.isExemptFromExclusiveMode());
        assertTrue(REMOVE_FOW.isExemptFromExclusiveMode());
        assertFalse(KILL_CREATURES.isExemptFromExclusiveMode());
        assertFalse(CLAIM_LAND.isExemptFromExclusiveMode());
    }

}
