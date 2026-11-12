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
import toniarts.openkeeper.tools.convert.map.Player;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlapLimitTest {

    private static final short PLAYER = Player.KEEPER1_ID;
    private static final short ENEMY = Player.KEEPER2_ID;

    @Test
    void slapsAreUnlimitedByDefault() {
        SlapLimit slapLimit = new SlapLimit();

        for (int i = 0; i < 1000; i++) {
            assertTrue(slapLimit.tryUseSlap(PLAYER));
        }
    }

    @Test
    void slapsStopAtTheLimit() {
        SlapLimit slapLimit = new SlapLimit();
        slapLimit.setLimit(2);

        assertTrue(slapLimit.tryUseSlap(PLAYER));
        assertTrue(slapLimit.tryUseSlap(PLAYER));
        assertFalse(slapLimit.tryUseSlap(PLAYER));
    }

    @Test
    void eachKeeperHasItsOwnSlaps() {
        SlapLimit slapLimit = new SlapLimit();
        slapLimit.setLimit(1);

        assertTrue(slapLimit.tryUseSlap(PLAYER));
        assertFalse(slapLimit.tryUseSlap(PLAYER));
        assertTrue(slapLimit.tryUseSlap(ENEMY));
    }

    @Test
    void newLimitStartsTheCountOver() {
        SlapLimit slapLimit = new SlapLimit();
        slapLimit.setLimit(1);
        slapLimit.tryUseSlap(PLAYER);

        slapLimit.setLimit(1);

        assertTrue(slapLimit.tryUseSlap(PLAYER));
    }

    @Test
    void zeroLiftsTheLimit() {
        SlapLimit slapLimit = new SlapLimit();
        slapLimit.setLimit(1);
        slapLimit.tryUseSlap(PLAYER);

        slapLimit.setLimit(SlapLimit.NO_LIMIT);

        assertTrue(slapLimit.tryUseSlap(PLAYER));
    }

    @Test
    void limitBeyondIntRangeIsNoLimit() {
        SlapLimit slapLimit = new SlapLimit();

        // An unsigned limit over Integer.MAX_VALUE is read as negative
        slapLimit.setLimit(-1);

        assertTrue(slapLimit.tryUseSlap(PLAYER));
        assertTrue(slapLimit.tryUseSlap(PLAYER));
    }

}
