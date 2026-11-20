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
 * Who owned an object when its trigger tree last looked at it, so a neutral
 * object that gets an owner can be told from one that changes hands
 */
public final class ObjectLastOwner implements EntityComponent {

    public short ownerId;

    public ObjectLastOwner() {
        // For serialization
    }

    public ObjectLastOwner(short ownerId) {
        this.ownerId = ownerId;
    }

}
