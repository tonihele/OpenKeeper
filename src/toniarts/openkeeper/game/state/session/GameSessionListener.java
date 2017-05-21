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
package toniarts.openkeeper.game.state.session;

import com.jme3.network.service.rmi.Asynchronous;
import toniarts.openkeeper.game.map.MapData;

/**
 * The gane callbacks the server sends to the client
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface GameSessionListener {

    /**
     * Client should start to load the game data up visually
     *
     * @param mapData the map data
     */
    @Asynchronous
    public void onGameDataLoaded(MapData mapData);

    /**
     * Signal that a player is ready and loaded up
     *
     * @param keeperId the keeper ID of the player
     */
    @Asynchronous
    public void onLoadComplete(short keeperId);

    /**
     * Game loading status update from a client
     *
     * @param progress current progress of a player
     * @param keeperId the keeper ID of the player
     */
    @Asynchronous
    public void onLoadStatusUpdate(float progress, short keeperId);

    /**
     * Client should start the visuals
     */
    @Asynchronous
    public void onGameStarted();

}
