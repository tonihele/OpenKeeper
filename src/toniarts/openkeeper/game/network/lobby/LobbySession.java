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
package toniarts.openkeeper.game.network.lobby;

import com.jme3.network.service.rmi.Asynchronous;
import java.util.List;
import toniarts.openkeeper.game.data.Keeper;

/**
 * Clients view on game lobby service
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface LobbySession {

    /**
     * Set us ready, or unready
     *
     * @param ready whether we are ready or not
     */
    @Asynchronous
    public void setReady(boolean ready);

    /**
     * Gets the current list of players
     *
     * @return the players
     */
    public List<Keeper> getPlayers();

    /**
     * Gets the current map selection
     *
     * @return the name of the map the server uses
     */
    public String getMap();

}
