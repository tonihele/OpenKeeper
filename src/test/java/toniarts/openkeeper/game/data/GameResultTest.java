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
package toniarts.openkeeper.game.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The debriefing tells a won level from a lost one by the game result
 */
class GameResultTest {

    @Test
    void wonLevel() {
        GameResult result = new GameResult();
        result.setData(GameResult.ResultType.LEVEL_WON, true);

        assertTrue(result.isLevelWon());
    }

    @Test
    void lostLevel() {
        GameResult result = new GameResult();
        result.setData(GameResult.ResultType.LEVEL_WON, false);

        assertFalse(result.isLevelWon());
    }

    @Test
    void resultWithoutOutcomeIsNoWin() {
        assertFalse(new GameResult().isLevelWon());
    }
}
