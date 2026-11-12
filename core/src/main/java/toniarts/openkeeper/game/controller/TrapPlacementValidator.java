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
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.utils.Point;

/**
 * Shared client/server validation for placing a trap blueprint. The client
 * uses it to colour the cursor and to decide whether to send the placement at
 * all, the server checks it again before it charges the gold.
 */
public final class TrapPlacementValidator {

    public enum Failure {
        NONE,
        INVALID_TRAP,
        OUT_OF_BOUNDS,
        INVALID_TERRAIN,
        OCCUPIED,
        NOT_ENOUGH_GOLD
    }

    private TrapPlacementValidator() {
        // Nope
    }

    /**
     * A trap goes on the keeper's own claimed floor, outside of rooms, on a
     * tile that has no trap or door yet. Placing it costs the trap's gold
     * cost.
     *
     * @param mapInformation the map
     * @param occupiedTiles tiles that already have a trap or a door
     * @param tile the tile to place the trap on
     * @param playerId the placing keeper
     * @param trap the trap, {@code null} if the id was not valid
     * @param availableGold the keeper's gold
     * @return why the trap can't be placed, {@link Failure#NONE} if it can
     */
    public static Failure validate(IMapInformation<?> mapInformation, Set<Point> occupiedTiles, Point tile,
            short playerId, Trap trap, int availableGold) {
        if (trap == null) {
            return Failure.INVALID_TRAP;
        }

        IMapTileInformation mapTile = tile != null ? mapInformation.getMapData().getTile(tile) : null;
        if (mapTile == null) {
            return Failure.OUT_OF_BOUNDS;
        }

        if (!isOwnFloor(mapInformation.getTerrain(mapTile), mapTile, playerId)) {
            return Failure.INVALID_TERRAIN;
        }

        if (occupiedTiles.contains(tile)) {
            return Failure.OCCUPIED;
        }

        if (trap.getGoldCost() > availableGold) {
            return Failure.NOT_ENOUGH_GOLD;
        }

        return Failure.NONE;
    }

    private static boolean isOwnFloor(Terrain terrain, IMapTileInformation mapTile, short playerId) {
        return terrain != null
                && mapTile.getOwnerId() == playerId
                && terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)
                && !terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)
                && !terrain.getFlags().contains(Terrain.TerrainFlag.ROOM);
    }

}
