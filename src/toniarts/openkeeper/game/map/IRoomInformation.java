/*
 * Copyright (C) 2014-2024 OpenKeeper
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
package toniarts.openkeeper.game.map;

import com.simsilica.es.EntityId;

/**
 * A kind of a room container with no editing functionalities
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IRoomInformation {

    /**
     * Get the entity ID of this room
     *
     * @return
     */
    EntityId getEntityId();

    int getHealth();

    int getMaxHealth();

    Integer getHealthPercent();

    /**
     * Is room at full health
     *
     * @return true if full health
     */
    boolean isAtFullHealth();

    short getOwnerId();

    short getRoomId();

    /**
     * Is this room instance destroyed?
     *
     * @see #remove()
     * @return is the room destroyed
     */
    boolean isDestroyed();

}
