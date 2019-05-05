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
package toniarts.openkeeper.game.logic;

import com.simsilica.es.EntityId;
import java.awt.Point;
import java.util.List;
import java.util.Set;
import toniarts.openkeeper.game.map.MapTile;

/**
 * Simple entity directory that offers handy lookup methods
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IEntityPositionLookup {

    /**
     * Get list of entities in specified map point
     *
     * @param p the map point
     * @return list of entities in given location
     */
    List<EntityId> getEntitiesInLocation(Point p);

    /**
     * Get list of entities in specified map point
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @return list of entities in given location
     */
    List<EntityId> getEntitiesInLocation(int x, int y);

    /**
     * Get list of entities in specified map tile
     *
     * @param mapTile the map tile
     * @return list of entities in given location
     */
    List<EntityId> getEntitiesInLocation(MapTile mapTile);

    /**
     * Get the map tile of an entity
     *
     * @param entityId the entity to look for
     * @return the entity location on a map tile basis
     */
    MapTile getEntityLocation(EntityId entityId);

    <T> List<T> getEntityTypesInLocation(Point p, Class<T> clazz);

    <T> List<T> getEntityTypesInLocation(int x, int y, Class<T> clazz);

    <T> List<T> getEntityTypesInLocation(MapTile mapTile, Class<T> clazz);

    /**
     * Checks if a tile in given location is blocked for player's entity (a.k.a.
     * can creature go through / to the tile)
     *
     * @param p the map point
     * @param playerId the player ID of which entity is wanting to know
     * @return true if the tile is blocked
     */
    boolean isTileBlocked(Point p, short playerId);

    /**
     * Checks if a tile in given location is blocked for player's entity (a.k.a.
     * can creature go through / to the tile)
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param playerId the player ID of which entity is wanting to know
     * @return true if the tile is blocked
     */
    boolean isTileBlocked(int x, int y, short playerId);

    /**
     * Checks if a tile in given location is blocked for player's entity (a.k.a.
     * can creature go through / to the tile)
     *
     * @param mapTile the map tile
     * @param playerId the player ID of which entity is wanting to know
     * @return true if the tile is blocked
     */
    boolean isTileBlocked(MapTile mapTile, short playerId);

    /**
     * Gets all the entities around given entity that you see or hear at this
     * given moment
     *
     * @param entityId you, the entitty
     * @return set of entities you see or hear
     */
    Set<EntityId> getSensedEntities(EntityId entityId);

}
