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
package toniarts.openkeeper.game.controller.creature;

import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.pathfinding.INavigable;
import toniarts.openkeeper.tools.convert.map.Terrain;

/**
 * A hero worker on its own, finding its way to the heart. Where there is no
 * way by ordinary means, it can dig through the rock in between, at the
 * same cost as a party with workers.
 */
public final class Tunneller implements INavigable {

    private final ICreatureController creature;

    public Tunneller(ICreatureController creature) {
        this.creature = creature;
    }

    @Override
    public Float getCost(IMapTileInformation from, IMapTileInformation to, IMapController mapController, IEntityPositionLookup entityPositionLookup) {
        Float cost = INavigable.super.getCost(from, to, mapController, entityPositionLookup);
        if (cost == null) {
            Float tunnelCost = getTunnelCost(mapController.getTerrain(to));
            if (tunnelCost != null) {
                return tunnelCost;
            }
        }
        return cost;
    }

    /**
     * The cost of digging or smashing through a tile of the given terrain
     *
     * @param terrain the terrain to go through
     * @return the cost, or {@code null} if the terrain can't be dug or smashed
     */
    public static Float getTunnelCost(Terrain terrain) {
        if (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {
            if (terrain.getFlags().contains(Terrain.TerrainFlag.DWARF_CAN_DIG_THROUGH)) {
                return 5f; // Dig our selves in
            }
            if (terrain.getFlags().contains(Terrain.TerrainFlag.ATTACKABLE)) {
                return 6f; // It seems that everybody can attack reinforced walls i.e. ?
            }
        }
        return null;
    }

    @Override
    public short getOwnerId() {
        return creature.getOwnerId();
    }

    @Override
    public boolean canFly() {
        return creature.canFly();
    }

    @Override
    public boolean canWalkOnWater() {
        return creature.canWalkOnWater();
    }

    @Override
    public boolean canWalkOnLava() {
        return creature.canWalkOnLava();
    }

    @Override
    public boolean canMoveDiagonally() {

        // We don't dig diagonally
        return false;
    }

}
