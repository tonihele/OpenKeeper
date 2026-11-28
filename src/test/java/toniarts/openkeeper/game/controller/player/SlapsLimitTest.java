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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlapsLimitTest {

    @Test
    void slapsAreUnlimitedByDefault() {
        PlayerStatsControl stats = new PlayerStatsControl();

        for (int i = 0; i < 1000; i++) {
            assertTrue(stats.tryUseSlap());
        }
    }

    @Test
    void slapsStopAtTheLimit() {
        PlayerStatsControl stats = new PlayerStatsControl();
        stats.setSlapsLimit(2);

        assertTrue(stats.tryUseSlap());
        assertTrue(stats.tryUseSlap());
        assertFalse(stats.tryUseSlap());
    }

    @Test
    void newLimitStartsTheCountOver() {
        PlayerStatsControl stats = new PlayerStatsControl();
        stats.setSlapsLimit(1);
        stats.tryUseSlap();

        stats.setSlapsLimit(1);

        assertTrue(stats.tryUseSlap());
    }

    @Test
    void zeroLiftsTheLimit() {
        PlayerStatsControl stats = new PlayerStatsControl();
        stats.setSlapsLimit(1);
        stats.tryUseSlap();

        stats.setSlapsLimit(0);

        assertTrue(stats.tryUseSlap());
    }

    @Test
    void limitBeyondIntRangeIsNoLimit() {
        PlayerStatsControl stats = new PlayerStatsControl();

        // An unsigned limit over Integer.MAX_VALUE is read as negative
        stats.setSlapsLimit(-1);

        assertTrue(stats.tryUseSlap());
        assertTrue(stats.tryUseSlap());
    }

}
