/*
 * Copyright (C) 2014-2018 OpenKeeper
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
import toniarts.openkeeper.tools.convert.map.Terrain;

/**
 * A kind of a map container with no editing functionalities
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IMapInformation {

    /**
     * Get the map data
     *
     * @return the map data
     */
    MapData getMapData();

    /**
     * Sets some specified map tiles in place (updates the map data)
     *
     * @param tiles tiles to set
     */
    void setTiles(List<MapTile> tiles);

    /**
     * Determine if a tile at x & y is buildable by the player
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param playerId the player
     * @param roomId the room to be build
     * @return is the tile buildable
     */
    boolean isBuildable(int x, int y, short playerId, short roomId);

    /**
     * Determine if a tile (maybe a room) at x & y is claimable by the player
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param playerId the player
     * @return is the tile claimable by you
     */
    boolean isClaimable(int x, int y, short playerId);

    /**
     * Determine if a tile at x & y is selected or not
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param playerId selected by the player
     * @return is the tile selected
     */
    boolean isSelected(int x, int y, short playerId);

    /**
     * Determine if a tile at x & y is selectable or not
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return is the tile selectable
     */
    boolean isTaggable(int x, int y);

    /**
     * Is the tile (building) sellable by us
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param playerId the player, the seller
     * @return true if we can sell
     */
    public boolean isSellable(int x, int y, short playerId);

    /**
     * Get terrain in given tile. FIXME: I don't think we should use the KWD
     * file stuff in here anymore.
     *
     * @param tile the map tile
     * @return the terrain
     */
    public Terrain getTerrain(MapTile tile);

    /**
     * Is claimable wall at tile point
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param playerId for the player
     * @return is the wall claimable
     */
    public boolean isClaimableWall(int x, int y, short playerId);

    /**
     * Is claimable floor at tile point (not a room)
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param playerId for the player
     * @return is the floor claimable
     */
    public boolean isClaimableTile(int x, int y, short playerId);

    /**
     * Is repairable wall at tile point
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param playerId for the player
     * @return is the wall repairable
     */
    public boolean isRepairableWall(int x, int y, short playerId);

    /**
     * Is claimable room tile at tile point
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param playerId for the player
     * @return is the room claimable
     */
    public boolean isClaimableRoom(int x, int y, short playerId);

    /**
     * Is water tile at the coordinates
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return is the tile water
     */
    public boolean isWater(int x, int y);

    /**
     * Is water tile at the coordinates
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return is the tile lava
     */
    public boolean isLava(int x, int y);

}
