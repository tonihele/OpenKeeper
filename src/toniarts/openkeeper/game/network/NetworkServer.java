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

import com.jme3.network.Network;
import com.jme3.network.Server;
import com.jme3.network.serializing.Serializer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 *
 * @author ArchDemon
 */
public class NetworkServer {
    private String host;
    private int port;
    private String name;
    private final int VERSION = 1;
    
    private Server server = null;

    public NetworkServer(String name, int port) throws UnknownHostException {
        this.host = InetAddress.getLocalHost().getCanonicalHostName();
        this.name = name;
        this.port = port;
    }
    
    public void start() throws IOException {
        if (server == null) {
            server = Network.createServer(port);
            //server = Network.createServer(name, VERSION, port, port);
        }
                
        register();
        
        server.addMessageListener(new ServerListener(), MessageChat.class);
        server.addConnectionListener(new ServerConnectionListener());
        server.start();
    }
  
    public void close() {
        if (server != null && server.isRunning()) {
            server.close();
        }        
    }
    
    private void register() {
        Serializer.registerClass(MessageChat.class);
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
}
