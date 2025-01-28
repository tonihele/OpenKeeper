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
package toniarts.openkeeper.game.controller.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.data.ResearchableType;
import toniarts.openkeeper.game.listener.PlayerRoomListener;
import toniarts.openkeeper.game.listener.RoomListener;
import toniarts.openkeeper.tools.convert.map.Room;

/**
 * Holds a list of player rooms and functionality related to them
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class PlayerRoomControl extends AbstractResearchablePlayerControl<Room, ResearchableEntity, PlayerRoomListener> implements RoomListener {

    private int roomCount = 0;
    private boolean portalsOpen = true;
    private final Map<Room, Set<IRoomController>> roomControllers = new HashMap<>();
    private IRoomController dungeonHeart;

    public PlayerRoomControl(Keeper keeper, List<Room> rooms) {
        super(keeper, keeper.getAvailableRooms(), rooms);
    }

    public void init(List<IRoomController> rooms) {
        for (IRoomController roomController : rooms) {
            onBuild(roomController);
        }
    }

    @Override
    protected ResearchableEntity createDataType(Room type) {
        return new ResearchableEntity(type.getRoomId(), ResearchableType.ROOM);
    }

    @Override
    public boolean setTypeAvailable(Room type, boolean available, boolean discovered) {

        // Skip non-buildables, I don't know what purpose they serve
        if (!type.getFlags().contains(Room.RoomFlag.BUILDABLE)) {
            return false;
        }

        boolean result = super.setTypeAvailable(type, available, discovered);

        return result;
    }

    @Override
    public void onBuild(IRoomController room) {

        // Add to the list
        Set<IRoomController> roomSet = roomControllers.get(room.getRoom());
        if (roomSet == null) {
            roomSet = new LinkedHashSet<>();
            roomControllers.put(room.getRoom(), roomSet);
        }
        roomSet.add(room);
        roomCount++;
        if (dungeonHeart == null && room.isDungeonHeart()) {
            dungeonHeart = room;
            keeper.setDungeonHeartLocation(room.getRoomInstance().getCenter());
        }
    }

    @Override
    public void onCaptured(IRoomController room) {
        onBuild(room);
    }

    @Override
    public void onCapturedByEnemy(IRoomController room) {
        onSold(room);
    }

    @Override
    public void onSold(IRoomController room) {

        // Delete
        Set<IRoomController> roomSet = roomControllers.get(room.getRoom());
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

    @Override
    public int getTypeCount(Room key) {
        Set<IRoomController> rooms = roomControllers.get(key);
        if (rooms != null) {
            return rooms.size();
        }
        return 0;
    }

    public boolean isPortalsOpen() {
        return portalsOpen;
    }

    public void setPortalsOpen(boolean portalsOpen) {
        this.portalsOpen = portalsOpen;
    }

    public Map<Room, Set<IRoomController>> getRoomControllers() {
        return roomControllers;
    }

    /**
     * Get room slab count, all rooms
     *
     * @return slab count
     */
    public int getRoomSlabsCount() {
        int count = 0;
        if (!types.isEmpty()) {
            for (Room room : new ArrayList<>(types.keySet())) {
                Set<IRoomController> rooms = roomControllers.get(room);
                if (rooms != null && !rooms.isEmpty()) {
                    for (IRoomController genericRoom : new ArrayList<>(rooms)) {
                        count += genericRoom.getRoomInstance().getCoordinates().size();
                    }
                }
            }
        }
        return count;
    }

    /**
     * Get room slab count, certain type of room
     *
     * @param room the room
     * @return slab count
     */
    public int getRoomSlabsCount(Room room) {
        int count = 0;
        Set<IRoomController> rooms = roomControllers.get(room);
        if (rooms != null && !rooms.isEmpty()) {
            for (IRoomController genericRoom : new ArrayList<>(rooms)) {
                count += genericRoom.getRoomInstance().getCoordinates().size();
            }
        }
        return count;
    }

    /**
     * Returns the dungeon heart of the player
     *
     * @return the dungeon heart
     */
    public IRoomController getDungeonHeart() {
        return dungeonHeart;
    }

}
