/*
 * Copyright (C) 2014-2017 OpenKeeper
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
 * An entity class marking ownership
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class Owner implements EntityComponent {

    public short ownerId;

    /**
     * Specifies who really has the control over the entity
     */
    public short controlId;

    public Owner() {
        // For serialization
    }

    public Owner(short ownerId, short controlId) {
        this.ownerId = ownerId;
        this.controlId = controlId;
    }

}
