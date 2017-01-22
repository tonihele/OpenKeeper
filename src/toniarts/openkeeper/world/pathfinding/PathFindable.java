/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.world.pathfinding;

import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.door.DoorControl;
import toniarts.openkeeper.world.room.GenericRoom;
import toniarts.openkeeper.world.room.RoomInstance;

/**
 * A simple interface for an entity that can use path finding
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface PathFindable {

    /**
     * Cost for traversing in water
     */
    public static final float WATER_COST = 1.4f;
    /**
     * Default path cost
     */
    public static final float DEFAULT_COST = 1.0f;

    /**
     * Can the entity travel from A to B?
     *
     * @param from the tile we are traversing from, always the adjacent tile
     * which we know already being accessible
     * @param to the tile we are travelling to
     * @param worldState world state for your convenience
     * @see #DEFAULT_COST
     * @see #WATER_COST
     * @return {@code null} if the to tile is not accessible
     */
    default public Float getCost(final TileData from, final TileData to, final WorldState worldState) {
        Terrain terrain = to.getTerrain();
        if (!terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {

            // Check for doors etc.
            DoorControl doorControl = worldState.getThingLoader().getDoor(to.getLocation());
            if (doorControl != null && !doorControl.isPassable(getOwnerId())) {
                return null;
            }

            // Check terrain
            if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {

                // Get room obstacles
                RoomInstance roomInstance = worldState.getMapLoader().getRoomCoordinates().get(to.getLocation());
                GenericRoom room = worldState.getMapLoader().getRoomActuals().get(roomInstance);
                return room.isTileAccessible(to.getLocation()) ? DEFAULT_COST : null;
            } else if (canFly()) {
                return DEFAULT_COST;
            } else if (terrain.getFlags().contains(Terrain.TerrainFlag.LAVA) && canWalkOnLava()) {
                return DEFAULT_COST;
            } else if (terrain.getFlags().contains(Terrain.TerrainFlag.WATER) && canWalkOnWater()) {
                return WATER_COST;
            }
            return DEFAULT_COST;
        }
        return null;
    }

    /**
     * The owner of the entity, useful for checking can we pass the doors etc.
     *
     * @return the owner ID
     */
    public short getOwnerId();

    /**
     * Can the entity fly
     *
     * @return true for flying
     */
    public boolean canFly();

    /**
     * Can the entity walk on water
     *
     * @return true for walking on water
     */
    public boolean canWalkOnWater();

    /**
     * Can the entity walk on lava
     *
     * @return true for walking on lava
     */
    public boolean canWalkOnLava();

    /**
     * Can the entity move diagonally
     *
     * @return true for walking diagonally
     */
    public boolean canMoveDiagonally();

}
