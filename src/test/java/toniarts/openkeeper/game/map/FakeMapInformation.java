/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.game.map;

import java.util.List;
import java.util.Map;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.Point;

/**
 * An {@link IMapInformation} test double. Only implements what
 * {@link MapColourClassifier} actually calls ({@code getMapData},
 * {@code getTerrain}, {@code isSolid}) with real behaviour; everything else
 * is a harmless stub. Public: also reused by {@code view.minimap}'s
 * rendering tests.
 */
public final class FakeMapInformation implements IMapInformation<FakeMapTile> {

    private final FakeMapData mapData;
    private final Map<Short, Terrain> terrainsById;

    public FakeMapInformation(FakeMapData mapData, Map<Short, Terrain> terrainsById) {
        this.mapData = mapData;
        this.terrainsById = Map.copyOf(terrainsById);
    }

    @Override
    public IMapDataInformation<FakeMapTile> getMapData() {
        return mapData;
    }

    @Override
    public void setTiles(List<FakeMapTile> tiles) {
    }

    @Override
    public boolean isBuildable(Point p, short playerId, short roomId) {
        return false;
    }

    @Override
    public boolean isClaimable(Point p, short playerId) {
        return false;
    }

    @Override
    public boolean isSelected(Point p, short playerId) {
        return false;
    }

    @Override
    public boolean isSelected(FakeMapTile tile, short playerId) {
        return false;
    }

    @Override
    public boolean isTaggable(Point p) {
        return false;
    }

    @Override
    public boolean isSellable(Point p, short playerId) {
        return false;
    }

    @Override
    public Terrain getTerrain(toniarts.openkeeper.game.map.IMapTileInformation tile) {
        return terrainsById.get(tile.getTerrainId());
    }

    @Override
    public boolean isClaimableWall(Point p, short playerId) {
        return false;
    }

    @Override
    public boolean isClaimableWall(FakeMapTile tile, Terrain terrain, short playerId) {
        return false;
    }

    @Override
    public boolean isClaimableTile(Point p, short playerId) {
        return false;
    }

    @Override
    public boolean isClaimableTile(FakeMapTile tile, Terrain terrain, short playerId) {
        return false;
    }

    @Override
    public boolean isRepairableWall(Point p, short playerId) {
        return false;
    }

    @Override
    public boolean isRepairableWall(FakeMapTile tile, Terrain terrain, short playerId) {
        return false;
    }

    @Override
    public boolean isClaimableRoom(Point p, short playerId) {
        return false;
    }

    @Override
    public boolean isClaimableRoom(FakeMapTile tile, Terrain terrain, short playerId) {
        return false;
    }

    @Override
    public boolean isWater(Point p) {
        return false;
    }

    @Override
    public boolean isLava(Point p) {
        return false;
    }

    @Override
    public boolean isSolid(Point p) {
        FakeMapTile tile = mapData.getTile(p.x, p.y);
        if (tile == null) {
            return false;
        }
        Terrain terrain = terrainsById.get(tile.getTerrainId());
        return terrain != null && terrain.getFlags().contains(Terrain.TerrainFlag.SOLID);
    }

}
