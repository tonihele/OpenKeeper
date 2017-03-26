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
package toniarts.openkeeper.game.state;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.network.Client;
import com.jme3.network.ClientStateListener;
import com.jme3.network.ClientStateListener.DisconnectInfo;
import com.jme3.network.ErrorListener;
import com.jme3.network.service.ClientService;
import com.simsilica.ethereal.EtherealClient;
import com.simsilica.ethereal.TimeSource;
import java.io.IOException;
import java.util.logging.Level;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.network.NetworkClient;
import toniarts.openkeeper.game.network.NetworkServer;
import toniarts.openkeeper.game.network.lobby.LobbyHostedService;
import toniarts.openkeeper.game.network.lobby.LobbyService;
import toniarts.openkeeper.game.network.session.AccountClientService;
import toniarts.openkeeper.game.network.session.AccountSessionListener;
import toniarts.openkeeper.utils.Utils;

/**
 * A state that manages the client server connection. Derived from pspeed's
 * examples.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ConnectionState extends AbstractAppState {

    private Main app;
    private String serverInfo;
    private AppStateManager stateManager;
    private NetworkServer server;
    private NetworkClient client;
    private final boolean gameHost;
    private final String gameName;
    private final String host;
    private final int port;
    private final String playerName;
    private final ConnectionObserver connectionObserver = new ConnectionObserver();
    private Connector connector;

    private volatile boolean closing;

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ConnectionState.class.getName());

    public ConnectionState(String host, int port, String playerName) {
        this(host, port, playerName, false, null);
    }

    public ConnectionState(String host, int port, String playerName, boolean hostGame, String gameName) {
        this.host = host;
        this.port = port;
        this.playerName = playerName;
        this.gameHost = hostGame;
        this.gameName = gameName;
    }

    public int getClientId() {
        return client.getClient().getId();
    }

    public NetworkClient getClient() {
        return client;
    }

    public TimeSource getRemoteTimeSource() {
        return getService(EtherealClient.class).getTimeSource();
    }

    public boolean isGameHost() {
        return gameHost;
    }

    public <T extends ClientService> T getService(Class<T> type) {
        return client.getService(type);
    }

    /**
     * If you are hosting the game, you have access to the lobby services
     *
     * @return the server side lobby services
     */
    public LobbyService getLobbyService() {
        if (server != null) {
            return server.getService(LobbyHostedService.class);
        }
        return null;
    }

    public void disconnect() {
        logger.info("disconnect()");
        closing = true;
        logger.info("Detaching ConnectionState");
        stateManager.detach(this);
    }

    public String getServerInfo() {
        return serverInfo;
    }

    protected void onLoggedOn(boolean loggedIn) {
        if (!loggedIn) {

            // We'll be nice and terminate ourselves
            disconnect();
        }

    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;
        this.stateManager = stateManager;

        // Start connecting
        connector = new Connector();
        connector.start();
    }

    @Override
    public void cleanup() {
        closing = true;
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.close();
        }
    }

    protected void showError(final String title, final Throwable e, final boolean fatal) {
        showError(title, null, e, fatal);
    }

    protected void showError(final String title, final String message, final Throwable e, final boolean fatal) {

    }

    protected void onConnected() {
        logger.info("onConnected()");

        // Add our client listeners
        client.getService(AccountClientService.class).addAccountSessionListener(new AccountObserver());

        serverInfo = client.getService(AccountClientService.class).getServerInfo();

        logger.log(Level.FINER, "Server info:{0}", serverInfo);

        logger.log(Level.INFO, "join({0})", playerName);

        // So here we'd login and then when we get a response from the
        // server that we are logged in then we'd launch the game state and
        // so on... for now we'll just do it directly.
        client.getService(AccountClientService.class).login(playerName, Utils.getSystemMemory());
    }

    protected void onDisconnected(DisconnectInfo info) {
        logger.log(Level.INFO, "onDisconnected({0})", info);
        if (closing) {
            return;
        }
        if (info != null) {
            showError("Disconnect", info.reason, info.error, true);
        } else {
            showError("Disconnected", "Unknown error", null, true);
        }
    }

    private class ConnectionObserver implements ClientStateListener, ErrorListener<Client> {

        @Override
        public void clientConnected(final Client c) {
            logger.log(Level.INFO, "clientConnected({0})", c);
            onConnected();
        }

        @Override
        public void clientDisconnected(final Client c, final DisconnectInfo info) {
            logger.log(Level.INFO, "clientDisconnected({0}, {1})", new Object[]{c, info});
            onDisconnected(info);
        }

        @Override
        public void handleError(Client source, Throwable t) {
            logger.log(Level.SEVERE, "Connection error", t);
            showError("Connection Error", t, true);
        }
    }

    private class AccountObserver implements AccountSessionListener {

        @Override
        public void notifyLoginStatus(final boolean loggedIn) {
            onLoggedOn(loggedIn);
        }
    }

    private class Connector extends Thread {

        public Connector() {
            super("GameConnector");
        }

        @Override
        public void run() {

            try {

                // If we are the host, create the server also
                if (gameHost) {
                    logger.log(Level.INFO, "Creating game server {0} at {1}", new Object[]{gameName, port});
                    server = new NetworkServer(gameName, port);
                    server.start();
                    logger.info("Server started.");
                }

                logger.log(Level.INFO, "Creating game client for: {0} {1}", new Object[]{gameHost ? "localhost" : host, port});
                client = new NetworkClient(gameHost ? "localhost" : host, port);
                if (closing) {
                    return;
                }
                client.getClient().addClientStateListener(connectionObserver);
                client.getClient().addErrorListener(connectionObserver);
                if (closing) {
                    return;
                }

                logger.info("Starting client...");
                client.start();
                logger.info("Client started.");
            } catch (IOException e) {
                disconnect();
                if (closing) {
                    return;
                }
                showError("Error Connecting", e, true);
            }
        }
    }
}
