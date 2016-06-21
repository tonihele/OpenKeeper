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
package toniarts.openkeeper.world.room;

import com.jme3.asset.AssetManager;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.world.room.control.RoomGoldControl;

/**
 * The Treasury
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class Treasury extends Normal {

    public Treasury(AssetManager assetManager, RoomInstance roomInstance, Thing.Room.Direction direction) {
        super(assetManager, roomInstance, direction);

        addObjectControl(new RoomGoldControl(this) {

            @Override
            protected int getObjectsPerTile() {
                return Treasury.this.getGoldPerTile();
            }

            @Override
            protected int getNumberOfAccessibleTiles() {
                return roomInstance.getCoordinates().size();
            }
        });
    }

    protected abstract int getGoldPerTile();

}
