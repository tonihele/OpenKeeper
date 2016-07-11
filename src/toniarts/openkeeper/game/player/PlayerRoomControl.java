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
package toniarts.openkeeper.game.player;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.world.listener.RoomListener;
import toniarts.openkeeper.world.room.GenericRoom;
import toniarts.openkeeper.world.room.RoomInstance;

/**
 * Holds a list of player rooms and functionality related to them
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PlayerRoomControl extends AbstractPlayerControl<Room, GenericRoom> implements RoomListener {

    private int roomCount = 0;

    public void init(List<Map.Entry<RoomInstance, GenericRoom>> rooms) {
        for (Map.Entry<RoomInstance, GenericRoom> entry : rooms) {
            onBuild(entry.getValue());
        }
    }

    @Override
    public void onBuild(GenericRoom room) {

        // Add to the list
        Set<GenericRoom> roomSet = get(room.getRoom());
        if (roomSet == null) {
            roomSet = new LinkedHashSet<>();
            put(room.getRoom(), roomSet);
        }
        roomSet.add(room);
        roomCount++;
    }

    @Override
    public void onCaptured(GenericRoom room) {
        onBuild(room);
    }

    @Override
    public void onCapturedByEnemy(GenericRoom room) {
        onSold(room);
    }

    @Override
    public void onSold(GenericRoom room) {

        // Delete
        Set<GenericRoom> roomSet = get(room.getRoom());
        if (roomSet != null) {
            roomSet.remove(room);
            roomCount--;
        }
    }

    /**
     * Get player room count. Even the non-buildables.
     *
     * @return the room count
     */
    @Override
    public int getTypeCount() {
        return roomCount;
    }

}
