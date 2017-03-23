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

import com.jme3.network.ConnectionListener;
import com.jme3.network.HostedConnection;
import com.jme3.network.Server;

/**
 *
 * @author ArchDemon
 */
public class ServerConnectionListener implements ConnectionListener {

    private final NetworkServer host;

    public ServerConnectionListener(NetworkServer host) {
        this.host = host;
    }

    @Override
    public void connectionAdded(Server server, HostedConnection conn) {
        //server.broadcast(new MessageChat("Server: connected " + conn.getId()));
        //server.broadcast(new MessageServerInfo(host.getName()));
//        getService(ChatHostedService.class).startHostingOnConnection(conn., name);
//host
    }

    @Override
    public void connectionRemoved(Server server, HostedConnection conn) {
        //server.broadcast(new MessageChat("Server: disconnected " + conn.getId()));
    }
}
