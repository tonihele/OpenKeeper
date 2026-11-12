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
package toniarts.openkeeper.game.controller;

import java.util.Set;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.Point;

/**
 * Shared client/server validation for placing a door blueprint. The client
 * uses it to colour the cursor and to decide whether to send the placement at
 * all, the server checks it again before it charges the gold.
 */
public final class DoorPlacementValidator {

    public enum Failure {
        NONE,
        INVALID_DOOR,
        OUT_OF_BOUNDS,
        INVALID_TERRAIN,
        OCCUPIED,
        NOT_BETWEEN_WALLS,
        NOT_ENOUGH_GOLD
    }

    private DoorPlacementValidator() {
        // Nope
    }

    /**
     * A door goes on the keeper's own claimed floor, outside of rooms, on a
     * tile that has no trap or door yet, like a trap. It also needs a wall on
     * two opposite sides, north and south or east and west. Any solid tile is
     * a wall, whoever owns it. A barricade stands on its own and needs no
     * walls. Placing the door costs the door's gold cost.
     *
     * @param mapInformation the map
     * @param occupiedTiles tiles that already have a trap or a door
     * @param tile the tile to place the door on
     * @param playerId the placing keeper
     * @param door the door, {@code null} if the id was not valid
     * @param availableGold the keeper's gold
     * @return why the door can't be placed, {@link Failure#NONE} if it can
     */
    public static Failure validate(IMapInformation<?> mapInformation, Set<Point> occupiedTiles, Point tile,
            short playerId, Door door, int availableGold) {
        if (door == null) {
            return Failure.INVALID_DOOR;
        }

        IMapTileInformation mapTile = tile != null ? mapInformation.getMapData().getTile(tile) : null;
        if (mapTile == null) {
            return Failure.OUT_OF_BOUNDS;
        }

        if (!TrapPlacementValidator.isOwnFloor(mapInformation.getTerrain(mapTile), mapTile, playerId)) {
            return Failure.INVALID_TERRAIN;
        }

        if (occupiedTiles.contains(tile)) {
            return Failure.OCCUPIED;
        }

        if (!door.getFlags().contains(Door.DoorFlag.IS_BARRICADE)
                && !isBetweenWallsEastAndWest(mapInformation, tile.x, tile.y)
                && !isBetweenWallsNorthAndSouth(mapInformation, tile.x, tile.y)) {
            return Failure.NOT_BETWEEN_WALLS;
        }

        if (door.getGoldCost() > availableGold) {
            return Failure.NOT_ENOUGH_GOLD;
        }

        return Failure.NONE;
    }

    /**
     * Whether the tile has a wall on its east and on its west side
     *
     * @param mapInformation the map
     * @param x the tile x
     * @param y the tile y
     * @return true if both sides are solid
     */
    public static boolean isBetweenWallsEastAndWest(IMapInformation<?> mapInformation, int x, int y) {
        return isWall(mapInformation, x - 1, y) && isWall(mapInformation, x + 1, y);
    }

    /**
     * Whether the tile has a wall on its north and on its south side
     *
     * @param mapInformation the map
     * @param x the tile x
     * @param y the tile y
     * @return true if both sides are solid
     */
    public static boolean isBetweenWallsNorthAndSouth(IMapInformation<?> mapInformation, int x, int y) {
        return isWall(mapInformation, x, y - 1) && isWall(mapInformation, x, y + 1);
    }

    private static boolean isWall(IMapInformation<?> mapInformation, int x, int y) {
        IMapTileInformation mapTile = mapInformation.getMapData().getTile(x, y);
        if (mapTile == null) {
            return false;
        }
        Terrain terrain = mapInformation.getTerrain(mapTile);
        return terrain != null && terrain.getFlags().contains(Terrain.TerrainFlag.SOLID);
    }

}
