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
import toniarts.openkeeper.game.data.GameResult.ResultType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The debriefing tells a won level from a lost one by the game result, and the Evil Rating of a
 * level is calculated from its statistics
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

    @Test
    void evilRatingIsZeroWithoutStatistics() {
        assertEquals(0, new GameResult().calculateEvilRating());
    }

    @Test
    void evilRatingWeighsEachStatistic() {
        assertEquals(40, rating(ResultType.ENEMY_CREATURES_KILLED, 1));
        assertEquals(50, rating(ResultType.HEROES_DESTROYED, 1));
        assertEquals(5, rating(ResultType.ROOMS_CAPTURED, 1));
        assertEquals(5, rating(ResultType.LAND_OWNED, 1));
        assertEquals(1, rating(ResultType.GOLD_MINED, 1));
        assertEquals(2, rating(ResultType.MANA_STORED, 1));
        assertEquals(5, rating(ResultType.ITEMS_MANUFACTURED, 1));
        assertEquals(20, rating(ResultType.CREATURES_COMMANDED, 1));
        assertEquals(40, rating(ResultType.CREATURES_CONVERTED, 1));
    }

    @Test
    void evilRatingCountsTrainingWithTwoDecimals() {
        assertEquals(100, rating(ResultType.CREATURE_TRAINING, 2f));
        assertEquals(125, rating(ResultType.CREATURE_TRAINING, 2.5f));

        // 3.333... levels are 3 levels and 33 hundredths
        assertEquals(166, rating(ResultType.CREATURE_TRAINING, 10f / 3));
    }

    @Test
    void evilRatingIncludesItemsAndConvertedCreatures() {
        GameResult result = new GameResult();
        result.setData(ResultType.ITEMS_MANUFACTURED, 3);
        result.setData(ResultType.CREATURES_CONVERTED, 2);

        assertEquals(5 * 3 + 40 * 2, result.calculateEvilRating());
    }

    @Test
    void evilRatingIgnoresWinAndTime() {
        GameResult result = new GameResult();
        result.setData(ResultType.LEVEL_WON, true);
        result.setData(ResultType.TIME_TAKEN, 1234.5);
        result.setData(ResultType.GOLD_MINED, 3000);
        result.setData(ResultType.LAND_OWNED, 200);
        result.setData(ResultType.CREATURES_COMMANDED, 12);

        assertEquals(3000 + 5 * 200 + 20 * 12, result.calculateEvilRating());
    }

    private static int rating(ResultType type, Number value) {
        GameResult result = new GameResult();
        result.setData(type, value);
        return result.calculateEvilRating();
    }
}
