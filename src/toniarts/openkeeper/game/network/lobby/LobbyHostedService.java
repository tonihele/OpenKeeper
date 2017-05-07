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

import com.jme3.network.HostedConnection;
import com.jme3.network.service.AbstractHostedConnectionService;
import com.jme3.network.service.HostedServiceManager;
import com.jme3.network.service.rmi.RmiHostedService;
import com.jme3.network.service.rmi.RmiRegistry;
import com.simsilica.ethereal.EtherealHost;
import com.simsilica.ethereal.NetworkStateListener;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.network.NetworkConstants;
import static toniarts.openkeeper.game.network.session.AccountHostedService.ATTRIBUTE_SYSTEM_MEMORY;
import toniarts.openkeeper.game.state.lobby.ClientInfo;
import toniarts.openkeeper.game.state.lobby.LobbyService;
import toniarts.openkeeper.game.state.lobby.LobbySession;
import toniarts.openkeeper.game.state.lobby.LobbySessionListener;
import toniarts.openkeeper.game.state.lobby.LocalLobby;
import toniarts.openkeeper.tools.convert.map.AI;

/**
 * Game server hosts lobby service for the game clients.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class LobbyHostedService extends AbstractHostedConnectionService implements LobbyService {

    private static final Logger logger = Logger.getLogger(LobbyHostedService.class.getName());

    private static final String ATTRIBUTE_SESSION = "lobby.session";
    public static final String ATTRIBUTE_KEEPER_ID = "lobby.keeperID";
    private int maxPlayers = 1;
    private int aiPlayerIdCounter = 0;

    private RmiHostedService rmiService;

    private final Object playerLock = new Object();
    private final Map<ClientInfo, AbstractLobbySessionImpl> players = new ConcurrentHashMap<>(4, 0.75f, 5);
    private String mapName;

    /**
     * Creates a new lobby service that will use the default reliable channel
     * for reliable communication.
     */
    public LobbyHostedService() {
        super(false);
    }

    private LobbySessionImpl getLobbySession(HostedConnection conn) {
        return conn.getAttribute(ATTRIBUTE_SESSION);
    }

    @Override
    protected void onInitialize(HostedServiceManager s) {

        // Grab the RMI service so we can easily use it later
        this.rmiService = getService(RmiHostedService.class);
        if (rmiService == null) {
            throw new RuntimeException(getClass().getName() + " requires an RMI service.");
        }
    }

    /**
     * Starts hosting the chat services on the specified connection using a
     * specified player name. This causes the player to 'enter' the chat room
     * and will then be able to send/receive messages.
     */
    public void startHostingOnConnection(HostedConnection conn, String playerName) {
        logger.log(Level.FINER, "startHostingOnConnection({0})", conn);

        boolean playerAdded = false;
        if (players.size() < maxPlayers) {
            synchronized (playerLock) {
                if (players.size() < maxPlayers) {
                    Keeper keeper = LocalLobby.getNextKeeper(false, players.keySet());
                    ClientInfo clientInfo = new ClientInfo(conn.getAttribute(ATTRIBUTE_SYSTEM_MEMORY), conn.getAddress(), conn.getId());
                    clientInfo.setName(playerName);
                    clientInfo.setKeeper(keeper);

                    LobbySessionImpl session = new LobbySessionImpl(conn, clientInfo);
                    conn.setAttribute(ATTRIBUTE_SESSION, session);
                    conn.setAttribute(ATTRIBUTE_KEEPER_ID, keeper.getId());

                    // Expose the session as an RMI resource to the client
                    RmiRegistry rmi = rmiService.getRmiRegistry(conn);
                    rmi.share(NetworkConstants.LOBBY_CHANNEL, session, LobbySession.class);

                    players.put(clientInfo, session);
                    playerAdded = true;
                }
            }
        }

        // See if added or not
        if (playerAdded) {

            // Notify players
            notifyPlayersChange();
        } else {

            // Oh noes, no room, terminate
            conn.close("Game full!");
        }
    }

    /**
     * Starts hosting the chat services on the specified connection using a
     * generated player name.
     */
    @Override
    public void startHostingOnConnection(HostedConnection conn) {
        startHostingOnConnection(conn, "Client:" + conn.getId());
    }

    @Override
    public void stopHostingOnConnection(HostedConnection conn) {
        logger.log(Level.FINER, "stopHostingOnConnection({0})", conn);
        LobbySessionImpl player = getLobbySession(conn);
        if (player != null) {

            // Then we are still hosting on the connection... it's
            // possible that stopHostingOnConnection() is called more than
            // once for a particular connection since some other game code
            // may call it and it will also be called during connection shutdown.
            conn.setAttribute(ATTRIBUTE_SESSION, null);

            // Remove player session from the active sessions list
            players.remove(player.getClientInfo());

            // Notify players
            notifyPlayersChange();
        }
    }

    @Override
    public void setMap(String mapName, int maxPlayers) {

        // In multiplayer, we only allow to change to maps that are equal or bigger than the current amount of connected human players
        int humanPlayers = 0;
        for (ClientInfo clientInfo : players.keySet()) {
            if (!clientInfo.getKeeper().isAi()) {
                humanPlayers++;
            }
        }

        // Change map only so that we don't kick anyone out
        if (humanPlayers <= maxPlayers) {
            this.mapName = mapName;
            this.maxPlayers = maxPlayers;
            synchronized (playerLock) {
                for (ClientInfo clientInfo : LocalLobby.compactPlayers(players.keySet(), maxPlayers)) {

                    // These are AI players only...
                    players.remove(clientInfo);
                }
                for (AbstractLobbySessionImpl lobby : players.values()) {
                    lobby.onMapChanged(mapName);
                }
            }
            notifyPlayersChange();
        }
    }

    @Override
    public void addPlayer() {

        // Add a computer player, create a NOP LobbySessionImpl for it
        if (players.size() < maxPlayers) {
            synchronized (playerLock) {
                if (players.size() < maxPlayers) {
                    aiPlayerIdCounter--;
                    if (aiPlayerIdCounter > 0) {

                        // LOL, ain't nobody gonna overflow this ever
                        aiPlayerIdCounter = -1;
                    }

                    Keeper keeper = LocalLobby.getNextKeeper(true, players.keySet());
                    final ClientInfo clientInfo = new ClientInfo(0, null, aiPlayerIdCounter);
                    clientInfo.setKeeper(keeper);
                    clientInfo.setReady(true);
                    clientInfo.setName(keeper.getAiType().toString());
                    AbstractLobbySessionImpl session = new AbstractLobbySessionImpl(clientInfo) {

                        @Override
                        protected HostedConnection getHostedConnection() {
                            return null;
                        }

                        @Override
                        public void setReady(boolean ready) {

                        }

                        @Override
                        public List<ClientInfo> getPlayers() {
                            return Collections.emptyList();
                        }

                        @Override
                        public void onPlayerListChanged(List<ClientInfo> players) {

                        }

                        @Override
                        public void onMapChanged(String mapName) {

                        }

                        @Override
                        public String getMap() {
                            return null;
                        }

                        @Override
                        public boolean isReady() {
                            return true;
                        }

                        @Override
                        public int getPlayerId() {
                            return clientInfo.getId();
                        }

                    };
                    players.put(clientInfo, session);
                }
            }
        }

        // Notify players
        notifyPlayersChange();
    }

    @Override
    public void removePlayer(ClientInfo keeper) {
        AbstractLobbySessionImpl lobbySessionImpl;
        synchronized (playerLock) {
            lobbySessionImpl = players.remove(keeper);
        }

        // Disconnect player
        if (lobbySessionImpl.getHostedConnection() != null) {
            lobbySessionImpl.getHostedConnection().close("You have been kicked from the game!");
        }

        // Notify players
        notifyPlayersChange();
    }

    @Override
    public void changeAIType(ClientInfo keeper, AI.AIType type) {
        AbstractLobbySessionImpl lobbySessionImpl = players.get(keeper);
        lobbySessionImpl.getClientInfo().getKeeper().setAiType(type);

        // Notify players
        notifyPlayersChange();
    }

    private void notifyPlayersChange() {
        for (AbstractLobbySessionImpl lobby : players.values()) {
            lobby.onPlayerListChanged(lobby.getPlayers());

        }
    }

    private abstract class AbstractLobbySessionImpl implements LobbySession, LobbySessionListener {

        private final ClientInfo clientInfo;

        public AbstractLobbySessionImpl(ClientInfo clientInfo) {
            this.clientInfo = clientInfo;
        }

        protected ClientInfo getClientInfo() {
            return clientInfo;
        }

        protected abstract HostedConnection getHostedConnection();
    }

    /**
     * The connection-specific 'host' for the LobbySession. For convenience this
     * also implements the LobbySessionListener. Since the methods don't collide
     * at all it's convenient for our other code not to have to worry about the
     * internal delegate.
     */
    private class LobbySessionImpl extends AbstractLobbySessionImpl {

        private final HostedConnection conn;
        private LobbySessionListener callback;

        public LobbySessionImpl(HostedConnection conn, ClientInfo clientInfo) {
            super(clientInfo);
            this.conn = conn;

            // Note: at this point we won't be able to look up the callback
            // because we haven't received the client's RMI shared objects yet.
        }

        protected LobbySessionListener getCallback() {
            if (callback == null) {
                RmiRegistry rmi = rmiService.getRmiRegistry(conn);
                callback = rmi.getRemoteObject(LobbySessionListener.class);
                if (callback == null) {
                    throw new RuntimeException("Unable to locate client callback for LobbySessionListener");
                }
            }
            return callback;
        }

        @Override
        public void setReady(boolean ready) {
            getClientInfo().setReady(ready);
            for (AbstractLobbySessionImpl lobby : players.values()) {
                lobby.onPlayerListChanged(getPlayers());
            }
        }

        @Override
        public void onMapChanged(String mapName) {
            getCallback().onMapChanged(mapName);
        }

        @Override
        protected HostedConnection getHostedConnection() {
            return conn;
        }

        @Override
        public List<ClientInfo> getPlayers() {
            List<ClientInfo> keepers = new ArrayList<>(players.keySet());
            Collections.sort(keepers, (ClientInfo o1, ClientInfo o2) -> Short.compare(o1.getKeeper().getId(), o2.getKeeper().getId()));

            // Update pings
            EtherealHost etherealHost = getService(EtherealHost.class);
            for (ClientInfo clientInfo : keepers) {
                AbstractLobbySessionImpl lobby = players.get(clientInfo);

                if (lobby != null && lobby.getHostedConnection() != null) {
                    NetworkStateListener networkStateListener = etherealHost.getStateListener(lobby.getHostedConnection());
                    if (networkStateListener != null) {
                        clientInfo.setPing(networkStateListener.getConnectionStats().getAveragePingTime());
                    }
                }
            }

            return keepers;
        }

        @Override
        public void onPlayerListChanged(List<ClientInfo> players) {
            getCallback().onPlayerListChanged(players);
        }

        @Override
        public String getMap() {
            return mapName;
        }

        @Override
        public boolean isReady() {
            throw new UnsupportedOperationException("You should cache these locally!");
        }

        @Override
        public int getPlayerId() {
            throw new UnsupportedOperationException("You should cache these locally!");
        }

    }
}
