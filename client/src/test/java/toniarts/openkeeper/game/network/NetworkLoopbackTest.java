/*
 * Copyright (C) 2014-2026 OpenKeeper
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

import com.jme3.math.Vector3f;
import com.jme3.network.Client;
import com.jme3.network.ClientStateListener;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.client.EntityDataClientService;
import com.simsilica.es.server.EntityDataHostedService;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.network.lobby.LobbyClientService;
import toniarts.openkeeper.game.network.session.AccountClientService;
import toniarts.openkeeper.game.state.lobby.ClientInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A multiplayer game in one process: the server is started from the classes of
 * the game logic alone, a client connects to it over the loopback interface,
 * logs in, joins the lobby and reads the server's entities.
 */
class NetworkLoopbackTest {

    private static final short PLAYER = 3;

    @Test
    @Timeout(120)
    void clientLogsInAndReadsTheServersEntities() throws Exception {
        NetworkServer server = startServer();
        NetworkClient client = null;
        try {
            client = new NetworkClient("localhost", server.getPort());
            CountDownLatch connected = new CountDownLatch(1);
            CountDownLatch loggedIn = new CountDownLatch(1);
            client.getClient().addClientStateListener(new ClientStateListener() {
                @Override
                public void clientConnected(Client c) {
                    connected.countDown();
                }

                @Override
                public void clientDisconnected(Client c, DisconnectInfo info) {
                }
            });
            client.getService(AccountClientService.class).addAccountSessionListener(status -> {
                if (status) {
                    loggedIn.countDown();
                }
            });
            client.start();
            assertTrue(connected.await(30, TimeUnit.SECONDS), "The client connects");

            // The account service lets the player in and hands the connection to the lobby
            assertEquals("loopback", client.getService(AccountClientService.class).getServerInfo());
            client.getService(AccountClientService.class).login("Tester", 8);
            assertTrue(loggedIn.await(30, TimeUnit.SECONDS), "The server confirms the login");

            // The lobby sends the players, a class of the game logic in a class of the lobby
            LobbyClientService lobby = client.getService(LobbyClientService.class);
            List<ClientInfo> players = lobby.getPlayers();
            assertEquals(1, players.size());
            assertEquals("Tester", players.get(0).getName());
            assertNotNull(players.get(0).getKeeper());

            // Being ready starts the game services on the connection, the entities with them
            lobby.setReady(true);
            NetworkClient theClient = client;
            waitFor(() -> theClient.getService(LobbyClientService.class).getPlayers().get(0).isReady());

            EntityData serverData = server.getService(EntityDataHostedService.class).getEntityData();
            EntityId entity = serverData.createEntity();
            serverData.setComponents(entity,
                    new Owner(PLAYER, PLAYER),
                    new Health(70, 100),
                    new Gold(250, 500),
                    new Position(1.5f, new Vector3f(1, 2, 3)));

            EntityData clientData = client.getService(EntityDataClientService.class).getEntityData();
            Owner owner = clientData.getComponent(entity, Owner.class);
            assertNotNull(owner, "The client gets the component from the server");
            assertEquals(PLAYER, owner.ownerId);
            assertEquals(70, clientData.getComponent(entity, Health.class).health);
            assertEquals(500, clientData.getComponent(entity, Gold.class).maxGold);
            assertEquals(new Vector3f(1, 2, 3), clientData.getComponent(entity, Position.class).position);
        } finally {
            if (client != null) {
                client.close();
            }
            server.close();
        }
    }

    /**
     * The server takes the port and the three after it, on TCP and UDP both
     */
    private static NetworkServer startServer() throws IOException {
        IOException failure = null;
        for (int attempt = 0; attempt < 10; attempt++) {
            int port;
            try (ServerSocket socket = new ServerSocket(0)) {
                port = socket.getLocalPort();
            }
            if (port > 65000 || !isFree(port)) {
                continue;
            }
            NetworkServer server = new NetworkServer("loopback", port);
            try {
                server.start();
                return server;
            } catch (IOException | RuntimeException e) {
                failure = e instanceof IOException io ? io : new IOException(e);
                server.close();
            }
        }
        throw failure != null ? failure : new IOException("No free ports for the server");
    }

    private static boolean isFree(int port) {
        for (int p = port; p <= port + 3; p++) {
            try (ServerSocket tcp = new ServerSocket(p); DatagramSocket udp = new DatagramSocket(p)) {
                tcp.setReuseAddress(true);
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    private static void waitFor(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (!condition.getAsBoolean()) {
            assertTrue(System.nanoTime() < deadline, "Timed out waiting for the server");
            Thread.sleep(50);
        }
    }

}
