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
 * A creature type a keeper's Portals have been able to attract, or a unique
 * creature his rooms have called in, with the generations it is still
 * favoured for
 */
public final class CreatureTypeAttraction implements EntityComponent {

    public short playerId;
    public short creatureId;
    public int favour;

    public CreatureTypeAttraction() {
        // For serialization
    }

    public CreatureTypeAttraction(short playerId, short creatureId, int favour) {
        this.playerId = playerId;
        this.creatureId = creatureId;
        this.favour = favour;
    }

}
