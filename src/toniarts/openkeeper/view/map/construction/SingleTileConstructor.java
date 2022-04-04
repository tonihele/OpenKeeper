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
package toniarts.openkeeper.view.map.construction;

import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.AssetUtils;

/**
 *
 * @author ArchDemon
 */
abstract class SingleTileConstructor {

    protected final KwdFile kwdFile;

    public SingleTileConstructor(KwdFile kwdFile) {
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
    protected boolean hasSameTile(IMapDataInformation mapData, int x, int y, Terrain terrain) {

        // Check for out of bounds
        IMapTileInformation tile = mapData.getTile(x, y);
        if (tile == null) {
            return false;
        }
        Terrain bridgeTerrain = kwdFile.getTerrainBridge(tile.getBridgeTerrainType(), kwdFile.getTerrain(tile.getTerrainId()));
        return (tile.getTerrainId() == terrain.getTerrainId()
                || (bridgeTerrain != null && bridgeTerrain.getTerrainId() == terrain.getTerrainId()));
    }

    protected boolean isSolidTile(IMapDataInformation mapData, int x, int y) {
        IMapTileInformation tile = mapData.getTile(x, y);
        if (tile == null) {
            return false;
        }
        return kwdFile.getTerrain(tile.getTerrainId()).getFlags().contains(Terrain.TerrainFlag.SOLID);
    }

    protected Spatial loadAsset(final AssetManager assetManager, final String asset) {
        return this.loadAsset(assetManager, asset, false);
    }

    protected Spatial loadAsset(final AssetManager assetManager, final String asset,
            final boolean useWeakCache) {

        Spatial spatial = ((Node) AssetUtils.loadModel(assetManager, asset, useWeakCache)).getChild(0);

        return spatial;
    }

    abstract public Spatial construct(IMapDataInformation mapData, int x, int y, final Terrain terrain,
            final AssetManager assetManager, String model);
}
