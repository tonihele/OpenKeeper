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
package toniarts.openkeeper.view.map.construction.room;

import com.jme3.asset.AssetManager;
import toniarts.openkeeper.view.map.construction.NormalConstructor;

/**
 * The workshop
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class WorkshopConstructor extends NormalConstructor {

    private final boolean[][] bigTiles;

    public WorkshopConstructor(AssetManager assetManager, toniarts.openkeeper.common.RoomInstance roomInstance) {
        super(assetManager, roomInstance);
        bigTiles = new boolean[map.length][map[0].length];
    }

    @Override
    public boolean useBigFloorTile(int x, int y) {

        // In workshop a big tile can't be in touch with other big tiles (except diagonally)
        // There is also catch, not always the big tile (a grill), sometimes normal + saw table
        // The logic is unknown to me, something maybe to do with the coordinates, not totally random it seems
        boolean N = hasSameTile(bigTiles, x, y - 1);
        boolean E = hasSameTile(bigTiles, x + 1, y);
        boolean S = hasSameTile(bigTiles, x, y + 1);
        boolean W = hasSameTile(bigTiles, x - 1, y);
        if (!N && !E && !S && !W) {
            bigTiles[x][y] = true;
            return true;
        }
        return false;
    }

}
