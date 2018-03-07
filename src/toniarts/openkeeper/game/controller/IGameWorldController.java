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
package toniarts.openkeeper.game.controller;

import com.badlogic.gdx.ai.pfa.GraphPath;
import java.awt.Point;
import toniarts.openkeeper.game.listener.PlayerActionListener;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.game.navigation.pathfinding.INavigable;

/**
 * Controls the game world, map and the entities
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IGameWorldController {

    /**
     * Add a lump sum of gold to a player, distributes the gold to the available
     * rooms
     *
     * @param playerId for the player
     * @param sum the gold sum
     * @return returns a sum of gold that could not be added to player's gold
     */
    int addGold(short playerId, int sum);

    /**
     * Add a lump sum of gold to a player, distributes the gold to the available
     * rooms
     *
     * @param playerId for the player
     * @param p a point where to drop the gold, can be {@code  null}
     * @param sum the gold sum
     * @return returns a sum of gold that could not be added to player's gold
     */
    int addGold(short playerId, Point p, int sum);

    //    public Collection<IPlayerController> getPlayerControllers() {
    //        return playerControllers.values();
    //    }
    //
    //    public Collection<Keeper> getPlayers() {
    //        return players.values();
    //    }
    /**
     * If you want to get notified about player actiosns
     *
     * @param listener the listener
     */
    void addListener(PlayerActionListener listener);

    /**
     * Stop listening to player actions
     *
     * @param listener the listener
     */
    void removeListener(PlayerActionListener listener);

    /**
     * Substract gold from player
     *
     * @param amount the amount to try to substract
     * @param playerId the player id
     * @return amount of money that could not be substracted from the player
     */
    int substractGold(int amount, short playerId);

    /**
     * Get a random tile, that is not a starting tile
     *
     * @param start starting coordinates
     * @param radius radius, in tiles
     * @param navigable the navigable entity
     * @return a random tile if one is found
     */
    Point findRandomAccessibleTile(Point start, int radius, INavigable navigable);

    /**
     * FIXME: This can NOT be. Just for quick easy testing. This is not even
     * thread safe
     *
     * @param start start point
     * @param end end point
     * @param navigable the entity to find path for
     * @return output path, null if path not found
     */
    GraphPath<MapTile> findPath(Point start, Point end, INavigable navigable);

    /**
     * Check if given tile is accessible by the given creature
     *
     * @param from from where
     * @param to to where
     * @param navigable the entity to test with
     * @return is accessible
     */
    boolean isAccessible(MapTile from, MapTile to, INavigable navigable);
}
