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
package toniarts.openkeeper.world.room;

import java.util.List;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.world.EntityInstance;

/**
 * Holds a room instance, series of coordinates that together form a room
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class RoomInstance extends EntityInstance<Room> {

    private List<WallSection> wallPoints;
    private GenericRoom room;

    public RoomInstance(Room room) {
        super(room);
    }

    public Room getRoom() {
        return super.getEntity();
    }

    public void setWallPoints(List<WallSection> wallPoints) {
        this.wallPoints = wallPoints;
    }

    public List<WallSection> getWallPoints() {
        return wallPoints;
    }

    protected void setRoomConstructor(GenericRoom room) {
        this.room = room;
    }

    public GenericRoom getRoomConstructor() {
        return room;
    }
}
