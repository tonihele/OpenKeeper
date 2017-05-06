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
package toniarts.openkeeper.game.state.lobby;

import toniarts.openkeeper.tools.convert.map.AI;

/**
 * This is server's perspective of lobby things. The services we offer our
 * clients. You can implement this and make a local lobby etc.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface LobbyService {

    /**
     * Set the map
     *
     * @param mapName the name of the map
     */
    public void setMap(String mapName);

    /**
     * Add a computer player to the game
     */
    public void addPlayer();

    /**
     * Remove an AI player from the game, or kick an human player out
     *
     * @param keeper the player to remove
     */
    public void removePlayer(ClientInfo keeper);

    /**
     * Change an AI player type
     *
     * @param keeper the player to change
     * @param type the new AI type
     */
    public void changeAIType(ClientInfo keeper, AI.AIType type);

}
