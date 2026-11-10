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

/**
 * Selling traps. Shared by the client, which colours the sell cursor, and the
 * server, which sells, so that the two agree.
 */
public final class TrapSellRules {

    private TrapSellRules() {
        // Nope
    }

    /**
     * A keeper can sell its own traps, armed or still a blueprint
     *
     * @param trapOwnerId the owner of the trap
     * @param playerId the selling keeper
     * @return whether the keeper may sell the trap
     */
    public static boolean isSellable(short trapOwnerId, short playerId) {
        return trapOwnerId == playerId;
    }

    /**
     * The gold a sold trap returns, a share of its gold cost. A blueprint
     * returns the same as an armed trap.
     *
     * @param goldCost the trap's gold cost
     * @param sellValuePercentage the share, the level variable
     * {@code TRAP_SELL_VALUE_PERCENTAGE_OF_COST}
     * @return the gold returned
     */
    public static int getSellValue(int goldCost, float sellValuePercentage) {
        return (int) (goldCost * (sellValuePercentage / 100));
    }

}
