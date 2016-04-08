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
import com.jme3.network.Network;
import java.io.IOException;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.logging.Logger;

/**
 *
 * @author ArchDemon
 */
public abstract class ServerQuery extends Thread {
    private LinkedList<ServerInfo> queue;
    private static final Logger logger = Logger.getLogger(ServerQuery.class.getName());

    public ServerQuery(LinkedList queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        ServerInfo server;

        while (true) {
            synchronized(queue) {
                if (queue.isEmpty() || isInterrupted()) {
                    break;
                }
                server = queue.pop();
            }

            try {
                //InetAddress ipAddress = InetAddress.getByName(server.getHost());
                //if (!ipAddress.isReachable(null, 255, 500)) { continue; }
                Client client = Network.connectToServer(server.getHost(), server.getPort());
                client.start();
                server.setName(client.getGameName());
                onFound(server);
                client.close();
            } catch (IOException ex) {
                logger.log(java.util.logging.Level.SEVERE, String.format("No %s. %s", server.getHost(), ex.getLocalizedMessage()));
            }
        }
    }

    public abstract void onFound(ServerInfo server);
}

