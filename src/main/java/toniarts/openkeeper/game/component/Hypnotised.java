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

/**
 * A creature a possessed Vampire has hypnotised. It is on the Vampire's side
 * and follows it until it is let go.
 */
public final class Hypnotised implements EntityComponent {

    /**
     * The possessed creature that hypnotised us
     */
    public EntityId entity;

    /**
     * Who owned us before, we go back to them when we are let go
     */
    public short ownerId;

    public Hypnotised() {
        // For serialization
    }

    public Hypnotised(EntityId entity, short ownerId) {
        this.entity = entity;
        this.ownerId = ownerId;
    }

}
