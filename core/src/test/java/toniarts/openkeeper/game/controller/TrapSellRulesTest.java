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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Values from the shipped data: TRAP_SELL_VALUE_PERCENTAGE_OF_COST is 50
 */
class TrapSellRulesTest {

    private static final float SELL_VALUE_PERCENTAGE = 50;

    @Test
    void sentryTrapSellsForHalfItsCost() {
        assertEquals(500, TrapSellRules.getSellValue(1000, SELL_VALUE_PERCENTAGE));
    }

    @Test
    void sellValueRoundsDown() {
        assertEquals(375, TrapSellRules.getSellValue(750, SELL_VALUE_PERCENTAGE)); // Fear Trap
        assertEquals(0, TrapSellRules.getSellValue(1, SELL_VALUE_PERCENTAGE));
    }

    @Test
    void ownTrapIsSellable() {
        assertTrue(TrapSellRules.isSellable(Player.KEEPER1_ID, Player.KEEPER1_ID));
    }

    @Test
    void trapOfAnotherKeeperIsNotSellable() {
        assertFalse(TrapSellRules.isSellable(Player.KEEPER2_ID, Player.KEEPER1_ID));
        assertFalse(TrapSellRules.isSellable(Player.GOOD_PLAYER_ID, Player.KEEPER1_ID));
    }

}
