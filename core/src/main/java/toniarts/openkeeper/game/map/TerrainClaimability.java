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

import toniarts.openkeeper.tools.convert.map.Terrain;

/**
 * A terrain's "claimable floor" property: non-ownable terrain that turns
 * into a different terrain once its health reaches maximum (e.g. Dirt Path,
 * Mana Vault). This is distinct from {@link IMapInformation#isClaimable} on
 * this class's own tile/player/adjacency sense
 */
public final class TerrainClaimability {

    private TerrainClaimability() {
    }

    public static boolean isClaimableFloor(Terrain terrain) {
        return !terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)
                && terrain.getStartingHealth() < terrain.getMaxHealth()
                && terrain.getTerrainId() != terrain.getMaxHealthTypeTerrainId();
    }

}
