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

import com.jme3.asset.AssetManager;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.world.room.control.RoomLairControl;

/**
 * The lair
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Lair extends Normal {

    public Lair(AssetManager assetManager, RoomInstance roomInstance, Thing.Room.Direction direction) {
        super(assetManager, roomInstance, direction);
        addObjectControl(new RoomLairControl(this) {

            @Override
            protected int getObjectsPerTile() {
                return 1;
            }

            @Override
            protected int getNumberOfAccessibleTiles() {
                return roomInstance.getCoordinates().size();
            }
        });
    }

    @Override
    protected boolean hasPillars() {
        return false;
    }
}
