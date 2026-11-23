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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HiScoresTest {

    @Test
    void scoreEqualToTheLastGetsIn() {
        HiScores hiScores = fullTable();

        assertFalse(hiScores.isEligibleToHiScores(99));
        assertTrue(hiScores.isEligibleToHiScores(100));
        assertTrue(hiScores.addWithoutSaving(100, "New", "Level"));
        assertEquals("New", hiScores.getEntries().get(9).getName());
    }

    @Test
    void scoreGoesAboveEqualScores() {
        HiScores hiScores = fullTable();

        hiScores.addWithoutSaving(500, "New", "Level");

        assertEquals("New", hiScores.getEntries().get(5).getName());
        assertEquals("Old 500", hiScores.getEntries().get(6).getName());
    }

    /**
     * Scores 1000 to 100, every hundred
     */
    private static HiScores fullTable() {
        HiScores hiScores = new HiScores();
        for (int score = 1000; score >= 100; score -= 100) {
            hiScores.addWithoutSaving(score, "Old " + score, "Level");
        }
        return hiScores;
    }

}
