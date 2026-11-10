/*
 * Copyright (C) 2014-2025 OpenKeeper
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
import toniarts.openkeeper.game.controller.room.AbstractRoomController;

/**
 * Acts as kind of summary for items stored in a room
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class Storage implements EntityComponent {

    public EntityId room;
    public AbstractRoomController.ObjectType objectType;
    public int currentCapacity;
    public int maxCapacity;

    public Storage() {
        // For serialization
    }

    public Storage(EntityId room, AbstractRoomController.ObjectType objectType, int currentCapacity, int maxCapacity) {
        this.room = room;
        this.objectType = objectType;
        this.currentCapacity = currentCapacity;
        this.maxCapacity = maxCapacity;
    }
}
