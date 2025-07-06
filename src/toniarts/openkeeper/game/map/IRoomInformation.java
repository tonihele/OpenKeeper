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
import toniarts.openkeeper.game.controller.room.AbstractRoomController;

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

    /**
     * Get maximum capacity by default storage type
     *
     * @return maximum capacity
     */
    int getMaxCapacity();

    /**
     * Get used capacity by default storage type
     *
     * @return current used capacity
     */
    int getUsedCapacity();

    /**
     * Get maximum capacity by the object type
     *
     * @param objectType the storage type
     * @return maximum capacity
     */
    int getMaxCapacity(AbstractRoomController.ObjectType objectType);

    /**
     * Get used capacity by the object type
     *
     * @param objectType the storage type
     * @return current used capacity
     */
    int getUsedCapacity(AbstractRoomController.ObjectType objectType);

    AbstractRoomController.ObjectType getDefaultStorageType();

}
