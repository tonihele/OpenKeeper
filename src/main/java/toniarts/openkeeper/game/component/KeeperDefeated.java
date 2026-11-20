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
package toniarts.openkeeper.game.component;

import com.simsilica.es.EntityComponent;

/**
 * A keeper whose Dungeon Heart was destroyed, and when. Like DK II's player
 * (+0xEE2), the time of the defeat is kept, the keeper's creatures are dealt
 * with from a few turns after it
 */
public final class KeeperDefeated implements EntityComponent {

    public short playerId;
    /**
     * The game time of the defeat
     */
    public double time;

    public KeeperDefeated() {
        // For serialization
    }

    public KeeperDefeated(short playerId, double time) {
        this.playerId = playerId;
        this.time = time;
    }

}
