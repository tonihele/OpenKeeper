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
package toniarts.openkeeper.game.controller;

import com.jme3.math.Vector2f;

/**
 * Holds together all game related player actions
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IPlayerActions {

    /**
     * Build a building to the wanted area
     *
     * @param start start coordinates
     * @param end end coordinates
     * @param playerId the player, the new owner
     * @param roomId room to build
     */
    public void build(Vector2f start, Vector2f end, short playerId, short roomId);

    /**
     * Sell building(s) from the wanted area
     *
     * @param start start coordinates
     * @param end end coordinates
     * @param playerId the player, the seller
     */
    public void sell(Vector2f start, Vector2f end, short playerId);

    /**
     * Set some tiles selected/undelected
     *
     * @param start start coordinates
     * @param end end coordinates
     * @param select select or unselect
     * @param playerId the player who selected the tile
     */
    public void selectTiles(Vector2f start, Vector2f end, boolean select, short playerId);

}
