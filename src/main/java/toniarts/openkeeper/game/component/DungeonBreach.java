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
 * A dungeon breach that the player's level script has not heard of yet. A
 * breach is when a claimed wall of a player is brought down by another player:
 * the owner of the wall has had the dungeon breached, the other player has
 * breached an enemy dungeon. There is at most one per player and kind
 */
public final class DungeonBreach implements EntityComponent {

    public short playerId;
    public boolean enemy;

    public DungeonBreach() {
        // For serialization
    }

    public DungeonBreach(short playerId, boolean enemy) {
        this.playerId = playerId;
        this.enemy = enemy;
    }

}
