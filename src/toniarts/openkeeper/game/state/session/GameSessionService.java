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

import toniarts.openkeeper.game.map.MapData;

/**
 * This is server's perspective of game flow things. The services we offer our
 * clients.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface GameSessionService {

    /**
     * Sends the game data to the clients to allow them to load it up visually
     *
     * @param mapData
     */
    public void sendGameData(MapData mapData);

    /**
     * Signals that the game should start
     */
    public void startGame();

}
