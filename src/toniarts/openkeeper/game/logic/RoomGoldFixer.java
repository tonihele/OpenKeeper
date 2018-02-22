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
package toniarts.openkeeper.game.logic;

import com.jme3.app.state.AbstractAppState;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.object.GoldObjectControl;
import toniarts.openkeeper.world.object.ObjectControl;
import toniarts.openkeeper.world.room.GenericRoom;
import toniarts.openkeeper.world.room.RoomInstance;

/**
 * A simple state to scan the loose gold inside treasuries. The loose gold is
 * added to the treasury automatically if there is some room left
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class RoomGoldFixer extends AbstractAppState implements IGameLogicUpdatable {

    private final WorldState worldState;

    public RoomGoldFixer(WorldState worldState) {
        this.worldState = worldState;
    }

    @Override
    public void processTick(float tpf, double gameTime) {

        // FIXME: Not all players can hold gold, like neutral or good players, they can have unclaimed treasuries and the original does not really merge the gold there
        synchronized (worldState.goldLock) {
            for (ObjectControl objectControl : worldState.getThingLoader().getObjects()) {
                if (objectControl instanceof GoldObjectControl && objectControl.getState() == ObjectControl.ObjectState.NORMAL) {

                    // See if there is a room
                    RoomInstance roomInstance = worldState.getMapLoader().getRoomCoordinates().get(objectControl.getTile().getLocation());
                    if (roomInstance != null) {
                        GenericRoom room = worldState.getMapLoader().getRoomActuals().get(roomInstance);
                        if (room.hasObjectControl(GenericRoom.ObjectType.GOLD) && !room.isFullCapacity()) {

//                            app.enqueue(() -> {
//
//                                // Give the gold
//                                GoldObjectControl gold = (GoldObjectControl) objectControl;
//                                int goldLeft = (int) room.getObjectControl(GenericRoom.ObjectType.GOLD).addItem(gold.getGold(), gold.getTile().getLocation(), worldState.getThingLoader(), null);
//                                if (goldLeft == 0) {
//                                    gold.removeObject();
//                                } else {
//                                    gold.setGold(goldLeft);
//                                }
//                            });
                            return; // One at the time, with proper sync etc. we can change this
                        }
                    }
                }
            }
        }
    }

    @Override
    public void start() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void stop() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
