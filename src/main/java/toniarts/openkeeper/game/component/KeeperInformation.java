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
import com.simsilica.es.EntityId;
import java.util.Set;

/**
 * What a player has learned about another keeper from the victims tortured
 * to death
 */
public final class KeeperInformation implements EntityComponent {

    public short playerId;
    public short keeperId;
    public Set<Integer> told;
    public Set<EntityId> roomsRevealed;

    public KeeperInformation() {
        // For serialization
    }

    public KeeperInformation(short playerId, short keeperId, Set<Integer> told, Set<EntityId> roomsRevealed) {
        this.playerId = playerId;
        this.keeperId = keeperId;
        this.told = told;
        this.roomsRevealed = roomsRevealed;
    }

}
