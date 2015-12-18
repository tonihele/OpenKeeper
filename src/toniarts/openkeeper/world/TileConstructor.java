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
package toniarts.openkeeper.world;

import com.jme3.asset.AssetManager;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Terrain;

/**
 *
 * @author ArchDemon
 */

abstract class TileConstructor {
    protected final KwdFile kwdFile;

    public TileConstructor(KwdFile kwdFile) {
        this.kwdFile = kwdFile;
    }

    /**
     * Compares the given terrain tile to terrain tile at the given coordinates
     *
     * @param tiles the tiles
     * @param x the x
     * @param y the y
     * @param terrain terrain tile to compare with
     * @return are the tiles same
     */
    protected boolean hasSameTile(TileData[][] tiles, int x, int y, Terrain terrain) {

        // Check for out of bounds
        if (x < 0 || x >= tiles.length || y < 0 || y >= tiles[x].length) {
            return false;
        }
        Terrain bridgeTerrain = kwdFile.getTerrainBridge(tiles[x][y].getFlag(), kwdFile.getTerrain(tiles[x][y].getTerrainId()));
        return (tiles[x][y].getTerrainId() == terrain.getTerrainId() || (bridgeTerrain != null && bridgeTerrain.getTerrainId() == terrain.getTerrainId()));
    }

    protected boolean isSolidTile(TileData[][] tiles, int x, int y) {
        // Check for out of bounds
        if (x < 0 || x >= tiles.length || y < 0 || y >= tiles[x].length) {
            return false;
        }
        return kwdFile.getTerrain(tiles[x][y].getTerrainId()).getFlags().contains(Terrain.TerrainFlag.SOLID);
    }

    abstract public Spatial construct(TileData[][] tiles, int x, int y, final Terrain terrain, final AssetManager assetManager, String model);
}
