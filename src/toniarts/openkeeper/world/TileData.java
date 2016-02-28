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

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.indexed.IndexedNode;
import com.badlogic.gdx.utils.Array;
import java.awt.Point;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Tile;

/**
 * Wrapper for a map tile
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class TileData extends Tile implements IndexedNode<TileData> {

    private boolean selected = false;
    private Integer randomTextureIndex;
    private final int x;
    private final int y;
    private final int index;
    private final KwdFile kwdFile;

    protected TileData(KwdFile kwdFile, Tile tile, Terrain terrain, int x, int y, int index) {
        this.setFlag(tile.getFlag());
        this.setPlayerId(tile.getPlayerId());
        this.setTerrainId(tile.getTerrainId());
        this.setUnknown(tile.getUnknown());
        this.x = x;
        this.y = y;
        this.index = index;
        this.kwdFile = kwdFile;

        // The water/lava under the bridge is set only when there is an actual bridge, but we might as well set it here, it doesn't change
        if (terrain.getFlags().contains(Terrain.TerrainFlag.LAVA)) {
            this.setFlag(BridgeTerrainType.LAVA);
        } else if (terrain.getFlags().contains(Terrain.TerrainFlag.WATER)) {
            this.setFlag(BridgeTerrainType.WATER);
        }
    }

    public boolean isSelected() {
        return selected;
    }

    protected void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected void setPlayerId(short playerId) {
        super.setPlayerId(playerId);
    }

    @Override
    protected void setTerrainId(short terrainId) {
        super.setTerrainId(terrainId);
    }

    public Integer getRandomTextureIndex() {
        return randomTextureIndex;
    }

    protected void setRandomTextureIndex(Integer randomTextureIndex) {
        this.randomTextureIndex = randomTextureIndex;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Point getLocation() {
        return new Point(x, y);
    }

    @Override
    public int getIndex() {
        return index;
    }

    /**
     * Get the terrain type of this terrain
     *
     * @return the terrain
     */
    public Terrain getTerrain() {
        return kwdFile.getTerrain(getTerrainId());
    }

    @Override
    public Array<Connection<TileData>> getConnections() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
