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
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.network.NetworkClient;
import toniarts.openkeeper.game.network.NetworkServer;
import toniarts.openkeeper.game.network.game.GameClientService;
import toniarts.openkeeper.game.network.game.GameHostedService;
import toniarts.openkeeper.game.network.lobby.LobbyClientService;
import toniarts.openkeeper.game.network.lobby.LobbyHostedService;
import toniarts.openkeeper.game.network.session.AccountClientService;
import toniarts.openkeeper.game.network.session.AccountSessionListener;
import toniarts.openkeeper.game.state.lobby.LobbyService;
import toniarts.openkeeper.game.state.session.GameSessionServerService;
import toniarts.openkeeper.utils.Utils;

/**
 * A state that manages the client server connection. Derived from pspeed's
 * examples.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ConnectionState extends AbstractAppState {
    
    private static final Logger logger = System.getLogger(ConnectionState.class.getName());

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
    private final List<ConnectionErrorListener> connectionErrorListeners = new CopyOnWriteArrayList<>();

    private volatile boolean closing;

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

    /**
     * The client side lobby services, you should always be welcome to use these
     *
     * @return the client side lobby services
     */
    public LobbyClientService getLobbyClientService() {
        return client.getService(LobbyClientService.class);
    }

    /**
     * If you are hosting the game, you have access to the game session services
     *
     * @return the server side game services
     */
    public GameSessionServerService getGameSessionServerService() {
        if (server != null) {
            return server.getService(GameHostedService.class);
        }
        return null;
    }

    /**
     * The client side game session services, you should always be welcome to
     * use these
     *
     * @return the client side game session services
     */
    public GameClientService getGameClientService() {
        return client.getService(GameClientService.class);
    }

    public void disconnect() {
        logger.log(Level.INFO, "disconnect()");
        closing = true;
        logger.log(Level.INFO, "Detaching ConnectionState");
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

    private void showError(final String title, final Throwable e, final boolean fatal) {
        showError(title, null, e, fatal);
    }

    private void showError(final String title, final String message, final Throwable e, final boolean fatal) {
        if (!connectionErrorListeners.isEmpty()) {
            for (ConnectionErrorListener listener : connectionErrorListeners) {
                listener.showError(title, message, e, fatal);
            }
        }
    }

    public void addConnectionErrorListener(ConnectionErrorListener listener) {
        connectionErrorListeners.add(listener);
    }

    public void removeConnectionErrorListener(ConnectionErrorListener listener) {
        connectionErrorListeners.remove(listener);
    }

    protected void onConnected() {
        logger.log(Level.INFO, "onConnected()");

        // Add our client listeners
        client.getService(AccountClientService.class).addAccountSessionListener(new AccountObserver());

        serverInfo = client.getService(AccountClientService.class).getServerInfo();

        logger.log(Level.DEBUG, "Server info:{0}", serverInfo);

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
            logger.log(Level.ERROR, "Connection error", t);
            showError("Connection Error", t, true);
        }
    }

    private class AccountObserver implements AccountSessionListener {

        @Override
        public void notifyLoginStatus(final boolean loggedIn) {
            onLoggedOn(loggedIn);
        }
    }

    public interface ConnectionErrorListener {

        public void showError(final String title, final String message, final Throwable e, final boolean fatal);
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
                    logger.log(Level.INFO, "Server started.");
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

                logger.log(Level.INFO, "Starting client...");
                client.start();
                logger.log(Level.INFO, "Client started.");
            } catch (Exception e) {
                if (closing) {
                    disconnect();
                    return;
                }
                disconnect();
                showError("Error Connecting", e, true);
            }
        }
    }
}
