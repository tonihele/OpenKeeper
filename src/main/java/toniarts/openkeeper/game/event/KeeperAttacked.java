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
package toniarts.openkeeper.game.event;

import toniarts.openkeeper.utils.Point;

/**
 * Something of a keeper was attacked, like DK II's attack messages, which
 * carry the tile (0x4C4600)
 *
 * @param playerId the keeper
 * @param target what was attacked
 * @param tile where
 */
public record KeeperAttacked(short playerId, Target target, Point tile) {

    /**
     * What was attacked
     */
    public enum Target {

        /**
         * The Dungeon Heart was hit, DK II's message 1 (0x4DA200)
         */
        HEART,
        /**
         * A creature started a fight, message 6 (0x45EB20)
         */
        CREATURE,
        /**
         * An enemy claims land, message 0x0C (0x498BB0)
         */
        LAND,
        /**
         * An enemy claims a room, message 0x22 (0x497DF0)
         */
        ROOM,
        /**
         * An Alarm Trap went off, message 0x2F (0x4AE120)
         */
        ALARM
    }

}
