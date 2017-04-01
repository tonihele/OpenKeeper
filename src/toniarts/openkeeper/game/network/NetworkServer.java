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

import com.jme3.network.HostedConnection;
import com.jme3.network.Network;
import com.jme3.network.Server;
import com.jme3.network.serializing.Serializer;
import com.jme3.network.serializing.serializers.FieldSerializer;
import com.jme3.network.service.AbstractHostedService;
import com.jme3.network.service.HostedService;
import com.jme3.network.service.HostedServiceManager;
import com.jme3.network.service.rmi.RmiHostedService;
import com.jme3.network.service.rpc.RpcHostedService;
import com.simsilica.es.server.EntityDataHostService;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.network.chat.ChatHostedService;
import toniarts.openkeeper.game.network.lobby.LobbyHostedService;
import toniarts.openkeeper.game.network.session.AccountHostedService;

/**
 *
 * @author ArchDemon
 */
public class NetworkServer {

    public final static int PROTOCOL_VERSION = 1;
    public final static String GAME_NAME = "OpenKeeper";

    public final static byte LOBBY_CHANNEL = 0;
    public final static byte CHAT_CHANNEL = 1;

    private final String host;
    private final int port;
    private String name;
    private EntityDataHostService edHost;

    private Server server = null;
    private long start = System.nanoTime();

    static {
//        ClassSerializer.initialize();

//        Serializer.registerClasses(Keeper.class);
    }

    public NetworkServer(String name, int port) throws UnknownHostException {
        this.host = InetAddress.getLocalHost().getCanonicalHostName();
        this.name = name;
        this.port = port;
    }

    private void initialize() {
        Serializer.registerClass(Keeper.class, new FieldSerializer());
    }

    public <T extends HostedService> T getService(Class<T> type) {
        return server.getServices().getService(type);
    }

    public void start() throws IOException {
        if (server == null) {
            server = Network.createServer(GAME_NAME, PROTOCOL_VERSION, port, port);
        }
        server.addChannel(port + 1); // Lobby
        server.addChannel(port + 2); // Chat

        initialize();
        server.addConnectionListener(new ServerConnectionListener(this));

        // Adding a delay for the connectionAdded right after the serializer registration
        // service gets to run let's the client get a small break in the buffer that should
        // generally prevent the RpcCall messages from coming too quickly and getting processed
        // before the SerializerRegistrationMessage has had a chance to process.
        // This "feature" happens with Linux almost all the time
        server.getServices().addService(new DelayService());

        server.getServices().addServices(new RpcHostedService(),
                new RmiHostedService(),
                new AccountHostedService(name),
                new LobbyHostedService(),
                new ChatHostedService()
        );
        server.start();

        start = System.nanoTime();
    }

    public void close() {
        if (server != null && server.isRunning()) {

            // Close the client connections gracefully
            for (HostedConnection conn : server.getConnections()) {
                conn.close("Server closing!");
            }

            server.close();

            // FIXME: Really, I'm sure this is not meant to be
            // https://hub.jmonkeyengine.org/t/solved-for-now-serializer-locked-error-what-does-it-mean-version-jme-3-1/33671
            Serializer.setReadOnly(false);
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

    private class DelayService extends AbstractHostedService {

        private void safeSleep(long ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                throw new RuntimeException("Checked exceptions are lame", e);
            }
        }

        @Override
        protected void onInitialize(HostedServiceManager serviceManager) {
            System.out.println("DelayService.onInitialize()");
            //safeSleep(2000);
            //System.out.println("DelayService.delay done");
        }

        @Override
        public void start() {
            System.out.println("DelayService.start()");
            //safeSleep(2000);
            //System.out.println("DelayService.delay done");
        }

        @Override
        public void connectionAdded(Server server, HostedConnection hc) {
            // Just in case
            super.connectionAdded(server, hc);
            System.out.println("DelayService.connectionAdded(" + hc + ")");
            safeSleep(500);
            System.out.println("DelayService.delay done");
        }
    }
}
