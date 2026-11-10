/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.game.network;

import com.jme3.network.Client;
import com.jme3.network.ClientStateListener;

/**
 *
 * @author ArchDemon
 */
public final class ClientStateChangeListener implements ClientStateListener {
    private final NetworkClient client;

    public ClientStateChangeListener(NetworkClient client) {
        this.client = client;
    }

    @Override
    public void clientConnected(Client c) {
        client.onConnected();
        //System.out.println(c.getGameName() + " is onConnected");
    }

    @Override
    public void clientDisconnected(Client c, DisconnectInfo info) {
        client.onDisconnected(info);
        //System.out.println(c.getGameName() + " is onDisconnected");
    }

}
