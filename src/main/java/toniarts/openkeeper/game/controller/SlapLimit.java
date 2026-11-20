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

import java.util.HashMap;
import java.util.Map;

/**
 * The slap limit set by the SET_SLAPS_LIMIT script action. Each keeper may slap
 * as many times as the limit allows, counted from the moment the limit is set.
 * Setting a new limit starts the count over. A limit of 0 means no limit, which
 * is also how a game starts. The level stores the limit as an unsigned integer,
 * a value too big for a Java int comes in negative: such a limit can never be
 * reached either, so it is taken as no limit too.<br>
 * Slaps arrive from the network while the level script runs in the game loop,
 * hence the synchronization.
 */
final class SlapLimit {

    static final int NO_LIMIT = 0;

    private int limit = NO_LIMIT;
    private final Map<Short, Integer> slapsUsedByPlayerId = new HashMap<>();

    synchronized void setLimit(int limit) {
        this.limit = Math.max(NO_LIMIT, limit);
        slapsUsedByPlayerId.clear();
    }

    /**
     * Uses one slap of the keeper, if any is left
     *
     * @param playerId the slapping keeper
     * @return whether the keeper may slap, {@code false} when the limit is
     * reached, in which case nothing is used
     */
    synchronized boolean tryUseSlap(short playerId) {
        if (limit == NO_LIMIT) {
            return true;
        }
        int slapsUsed = slapsUsedByPlayerId.getOrDefault(playerId, 0);
        if (slapsUsed >= limit) {
            return false;
        }
        slapsUsedByPlayerId.put(playerId, slapsUsed + 1);
        return true;
    }

}
