/*
 * Copyright (C) 2014-2016 OpenKeeper
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
package toniarts.openkeeper.game.task;

import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.room.GenericRoom;
import toniarts.openkeeper.world.room.control.RoomObjectControl;

/**
 * A base of a task that involves a room
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class AbstractRoomTask extends AbstractTileTask {

    private final GenericRoom room;

    public AbstractRoomTask(WorldState worldState, int x, int y, short playerId, GenericRoom room) {
        super(worldState, x, y, playerId);

        this.room = room;
    }

    protected GenericRoom getRoom() {
        return room;
    }

    @Override
    public boolean isValid() {

        // See that the room exists and has capacity etc.
        return room.getRoomInstance().getOwnerId() == playerId && worldState.getMapLoader().getRoomActuals().containsKey(room.getRoomInstance()) && !getRoomObjectControl().isFullCapacity();
    }

    protected abstract GenericRoom.ObjectType getRoomObjectType();

    protected RoomObjectControl getRoomObjectControl() {
        return getRoom().getObjectControl(getRoomObjectType());
    }

}
