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
import toniarts.openkeeper.game.data.GameResult;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Horned Reaper is sent for a won campaign level, whichever way the level was started
 */
public class GameControllerReaperDueTest {

    @Test
    void wonCampaignLevelSendsTheReaper() {
        GameResult result = new GameResult();
        result.setData(GameResult.ResultType.LEVEL_WON, true);

        assertTrue(GameController.isReaperDue(true, result));
    }

    @Test
    void otherGamesDoNot() {
        GameResult won = new GameResult();
        won.setData(GameResult.ResultType.LEVEL_WON, true);
        GameResult lost = new GameResult();
        lost.setData(GameResult.ResultType.LEVEL_WON, false);

        assertFalse(GameController.isReaperDue(false, won));
        assertFalse(GameController.isReaperDue(true, lost));
        assertFalse(GameController.isReaperDue(true, null));
    }
}
