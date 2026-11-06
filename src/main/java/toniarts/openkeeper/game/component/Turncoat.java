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
 * A creature under the Turncoat spell. Until the end time it fights for the
 * given player, but it stays owned by its keeper (pay, lair, jobs)
 */
public final class Turncoat implements EntityComponent {

    /**
     * The player the creature fights for
     */
    public short playerId;

    /**
     * Game time when the spell wears off
     */
    public double endTime;

    public Turncoat() {
        // For serialization
    }

    public Turncoat(short playerId, double endTime) {
        this.playerId = playerId;
        this.endTime = endTime;
    }

}
