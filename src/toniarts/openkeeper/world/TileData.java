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

import toniarts.openkeeper.tools.convert.map.Tile;

/**
 * Wrapper for a map tile
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class TileData extends Tile {

    private boolean selected = false;
    private Integer randomTextureIndex;

    protected TileData(Tile tile) {
        this.setFlag(tile.getFlag());
        this.setPlayerId(tile.getPlayerId());
        this.setTerrainId(tile.getTerrainId());
        this.setUnknown(tile.getUnknown());
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
}
