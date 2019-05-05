/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.common;

import java.util.List;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.view.map.WallSection;

/**
 * Holds a room instance, series of coordinates that together form a room
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class RoomInstance extends EntityInstance<Room> {

    private List<WallSection> wallSections;
    private final Thing.Room.Direction direction;
    private short ownerId;
    private boolean destroyed = false;

    public RoomInstance(Room room) {
        this(room, null);
    }

    public RoomInstance(Room room, Thing.Room thing) {
        super(room);
        if (thing != null) {
            this.direction = thing.getDirection();
        } else {
            this.direction = null;
        }
    }

    public Room getRoom() {
        return super.getEntity();
    }

    public void setWallSections(List<WallSection> wallSections) {
        this.wallSections = wallSections;
    }

    public List<WallSection> getWallSections() {
        return wallSections;
    }

    /**
     * Get the room instance direction. Some fixed rooms may have this.
     *
     * @return room direction
     */
    public Thing.Room.Direction getDirection() {
        return direction;
    }

    public void setOwnerId(short ownerId) {
        this.ownerId = ownerId;
    }

    public short getOwnerId() {
        return ownerId;
    }

    public void setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

}
