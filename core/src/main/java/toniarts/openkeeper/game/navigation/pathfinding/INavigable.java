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
package toniarts.openkeeper.game.navigation.pathfinding;

import com.simsilica.es.EntityId;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapTileInformation;

/**
 * A simple interface for an entity that can use path finding
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface INavigable {

    /**
     * Cost for traversing in water
     */
    public static final float WATER_COST = 1.4f;
    /**
     * Default path cost
     */
    public static final float DEFAULT_COST = 1.0f;
    /**
     * What breaking through a door adds to the path cost, so that a short way
     * round is preferred
     */
    public static final float DOOR_COST = 8.0f;

    /**
     * Can the entity travel from A to B?
     *
     * @param from the tile we are traversing from, always the adjacent tile
     * which we know already being accessible
     * @param to the tile we are travelling to
     * @param mapController the map controller
     * @param entityPositionLookup the entity lookup
     * @see #DEFAULT_COST
     * @see #WATER_COST
     * @return {@code null} if the to tile is not accessible
     */
    default public Float getCost(final IMapTileInformation from, final IMapTileInformation to, final IMapController mapController,
            final IEntityPositionLookup entityPositionLookup) {
        return MapIndexedGraph.getCost(this, from, to, mapController, entityPositionLookup);
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
     * Is the entity willing and able to break through the door that blocks the
     * way. Such a door doesn't stop the path finding, it just costs more.
     *
     * @param door the door entity
     * @return true if the entity breaks the door down when it is in the way
     */
    default public boolean canBreakDoor(EntityId door) {
        return false;
    }

    /**
     * Can the entity move diagonally
     *
     * @return true for walking diagonally
     */
    public boolean canMoveDiagonally();

}
