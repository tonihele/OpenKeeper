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

import toniarts.openkeeper.game.network.message.*;
import com.jme3.network.Network;
import com.jme3.network.Server;
import com.simsilica.es.server.EntityDataHostService;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 *
 * @author ArchDemon
 */
public class NetworkServer {
    public final static String GAME_NAME = "OpenKeeper";
    public final static int PROTOCOL_VERSION = 1;

    private String host;
    private int port;
    private String name;
    private EntityDataHostService edHost;

    private Server server = null;
    private long start = System.nanoTime();

    static {
        ClassSerializer.initialize();
    }

    public NetworkServer(String name, int port) throws UnknownHostException {
        this.host = InetAddress.getLocalHost().getCanonicalHostName();
        this.name = name;
        this.port = port;
    }

    private void initialize() {
        server.addMessageListener(new ServerListener(this),
                MessageChat.class,
                MessageTime.class,
                MessagePlayerInfo.class,
                MessageServerInfo.class);
    }

    public void start() throws IOException {
        if (server == null) {
            server = Network.createServer(port);
            //server = Network.createServer(GAME_NAME, PROTOCOL_VERSION, port, port);
        }
        server.addChannel(port + 1);

        initialize();
        server.addConnectionListener(new ServerConnectionListener(this));
        server.start();

        start = System.nanoTime();
    }

    public void close() {
        if (server != null && server.isRunning()) {
            server.close();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public Server getServer() {
        return server;
    }

    public long getGameTime() {
        return System.nanoTime() - start;
    }
}
