/*
 * Copyright (C) 2014-2019 OpenKeeper
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
import java.util.List;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.data.ResearchableType;
import toniarts.openkeeper.game.listener.PlayerRoomListener;
import toniarts.openkeeper.tools.convert.map.Trap;

/**
 * Holds a list of player traps and functionality related to them
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PlayerTrapControl extends AbstractResearchablePlayerControl<Trap, ResearchableEntity> /*implements RoomListener*/ {

    private int trapCount = 0;
    private List<PlayerRoomListener> roomAvailabilityListeners;

    public PlayerTrapControl(Keeper keeper, List<Trap> traps) {
        super(keeper, keeper.getAvailableTraps(), traps);
    }

//    public void init(List<IRoomController> doors) {
//        for (IRoomController roomController : doors) {
//            onBuild(roomController);
//        }
//    }
    @Override
    protected ResearchableEntity createDataType(Trap type) {
        return new ResearchableEntity(type.getTrapId(), ResearchableType.TRAP);
    }

    @Override
    public boolean setTypeAvailable(Trap type, boolean available, boolean discovered) {
        boolean result = super.setTypeAvailable(type, available, discovered);

        // Notify listeners
//        if (result && roomAvailabilityListeners != null) {
//            for (PlayerRoomListener listener : roomAvailabilityListeners) {
//                listener.onRoomAvailabilityChanged(keeper.getId(), type.getId(), available);
//            }
//        }

        return result;
    }

//    @Override
//    public void onBuild(IRoomController room) {
//
//        // Add to the list
//        Set<IRoomController> roomSet = get(room.getRoom());
//        if (roomSet == null) {
//            roomSet = new LinkedHashSet<>();
//            put(room.getRoom(), roomSet);
//        }
//        roomSet.add(room);
//        roomCount++;
//        if (dungeonHeart == null && room.isDungeonHeart()) {
//            dungeonHeart = room;
//            keeper.setDungeonHeartLocation(room.getRoomInstance().getCenter());
//        }
//    }
//
//    @Override
//    public void onCaptured(IRoomController room) {
//        onBuild(room);
//    }
//
//    @Override
//    public void onCapturedByEnemy(IRoomController room) {
//        onSold(room);
//    }
//
//    @Override
//    public void onSold(IRoomController room) {
//
//        // Delete
//        Set<IRoomController> roomSet = get(room.getRoom());
//        if (roomSet != null) {
//            roomSet.remove(room);
//            roomCount--;
//        }
//    }

    /**
     * Get player door count
     *
     * @return the door count
     */
    @Override
    public int getTypeCount() {
        return trapCount;
    }

    /**
     * Listen to room availability changes
     *
     * @param listener the listener
     */
    public void addListener(PlayerRoomListener listener) {
        if (roomAvailabilityListeners == null) {
            roomAvailabilityListeners = new ArrayList<>();
        }
        roomAvailabilityListeners.add(listener);
    }

    /**
     * Stop listening to room availability changes
     *
     * @param listener the listener
     */
    public void removeListener(PlayerRoomListener listener) {
        if (roomAvailabilityListeners != null) {
            roomAvailabilityListeners.remove(listener);
        }
    }

}
