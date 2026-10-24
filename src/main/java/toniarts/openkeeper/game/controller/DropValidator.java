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
import toniarts.openkeeper.game.component.Interaction;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;

/**
 * Shared drop rules for the keeper hand, used by both client and server
 */
public final class DropValidator {

    private DropValidator() {
    }

    /**
     * Whether the keeper can drop a thing from the hand on a tile. Everything
     * can be dropped on the keeper's own land, workers also on unclaimed and
     * neutral land. Never on solid terrain, water or lava.
     *
     * @param interaction the interaction of the dropped thing
     * @param tile the tile to drop on
     * @param terrain the terrain of the tile
     * @param playerId the dropping keeper
     * @return whether the thing can be dropped there
     */
    public static boolean canDrop(Interaction interaction, IMapTileInformation tile, Terrain terrain, short playerId) {
        if (interaction == null || tile == null || terrain == null) {
            return false;
        }
        Set<Terrain.TerrainFlag> flags = terrain.getFlags();
        if (flags.contains(Terrain.TerrainFlag.SOLID)) {
            return false;
        }

        // Normal rule: own claimed land
        if (tile.getOwnerId() == playerId && flags.contains(Terrain.TerrainFlag.OWNABLE)) {
            return true;
        }

        // Workers: also unclaimed floor (Dirt Path) and neutral land, but not water/lava
        if (interaction.canBeDroppedOnAnyLand) {
            boolean unclaimed = !flags.contains(Terrain.TerrainFlag.OWNABLE)
                    || tile.getOwnerId() == Player.NEUTRAL_PLAYER_ID;
            return unclaimed
                    && !flags.contains(Terrain.TerrainFlag.WATER)
                    && !flags.contains(Terrain.TerrainFlag.LAVA);
        }
        return false;
    }

}
