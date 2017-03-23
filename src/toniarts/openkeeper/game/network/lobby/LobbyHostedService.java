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
import com.jme3.network.MessageConnection;
import com.jme3.network.service.AbstractHostedConnectionService;
import com.jme3.network.service.HostedServiceManager;
import com.jme3.network.service.rmi.RmiHostedService;
import com.jme3.network.service.rmi.RmiRegistry;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.stream.Collectors.toList;
import toniarts.openkeeper.game.data.Keeper;

/**
 * HostedService providing a chat server for connected players. Some time during
 * player connection setup, the game must start hosting and provide the player
 * name in order for the client to participate.
 *
 * @author Paul Speed
 */
/**
 * Game server hosts lobby service for the game clients
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class LobbyHostedService extends AbstractHostedConnectionService implements LobbyService {

    private static final Logger logger = Logger.getLogger(LobbyHostedService.class.getName());

    private static final String ATTRIBUTE_SESSION = "lobby.session";
    private static final int MAX_PLAYERS = 4;

    private RmiHostedService rmiService;
    private final int channel;

    private final Object playerLock = new Object();
    private final Map<Keeper, AbstractLobbySessionImpl> players = new ConcurrentHashMap<>(MAX_PLAYERS, 0.75f, 5);

    /**
     * Creates a new lobby service that will use the default reliable channel
     * for reliable communication.
     */
    public LobbyHostedService() {
        super(false);
        this.channel = MessageConnection.CHANNEL_DEFAULT_RELIABLE;
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
        if (players.size() < MAX_PLAYERS) {
            synchronized (playerLock) {
                if (players.size() < MAX_PLAYERS) {
                    Keeper keeper = getNextKeeper(playerName);

                    LobbySessionImpl session = new LobbySessionImpl(conn, keeper);
                    conn.setAttribute(ATTRIBUTE_SESSION, session);

                    // Expose the session as an RMI resource to the client
                    RmiRegistry rmi = rmiService.getRmiRegistry(conn);
                    rmi.share((byte) channel, session, LobbySession.class);

                    players.put(keeper, session);
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
            players.remove(player.getKeeper());

            // Notify players
            notifyPlayersChange();
        }
    }

    @Override
    public void setMap(String mapName) {
        for (AbstractLobbySessionImpl lobby : players.values()) {
            lobby.onMapChanged(mapName);
        }
    }

    @Override
    public void addPlayer() {

        // Add a computer player, create a NOP LobbySessionImpl for it
        if (players.size() < MAX_PLAYERS) {
            synchronized (playerLock) {
                if (players.size() < MAX_PLAYERS) {
                    Keeper keeper = getNextKeeper(null);
                    players.put(keeper, new AbstractLobbySessionImpl(keeper) {

                        @Override
                        protected HostedConnection getHostedConnection() {
                            return null;
                        }

                        @Override
                        public void setReady(boolean ready) {

                        }

                        @Override
                        public List<Keeper> getPlayers() {
                            return Collections.emptyList();
                        }

                        @Override
                        public void onPlayerListChanged(List<Keeper> players) {

                        }

                        @Override
                        public void onMapChanged(String mapName) {

                        }

                    });
                }
            }
        }

        // Notify players
        notifyPlayersChange();
    }

    @Override
    public void removePlayer(Keeper keeper) {
        AbstractLobbySessionImpl lobbySessionImpl = players.remove(keeper);

        // Disconnect player
        if (lobbySessionImpl != null) {
            lobbySessionImpl.getHostedConnection().close("You have been kicked from the game!");
        }

        // Notify players
        notifyPlayersChange();
    }

    private void notifyPlayersChange() {
        for (AbstractLobbySessionImpl lobby : players.values()) {
            lobby.onPlayerListChanged(lobby.getPlayers());
        }
    }

    private Keeper getNextKeeper(String playerName) {
        short id = Keeper.KEEPER1_ID;
        List<Short> keepers = players.keySet().stream().map(Keeper::getId).collect(toList());
        Collections.sort(keepers);
        while (Collections.binarySearch(keepers, id) >= 0) {
            id++;
        }
        return new Keeper(playerName == null, playerName, id, null);
    }

    private abstract class AbstractLobbySessionImpl implements LobbySession, LobbySessionListener {

        private final Keeper keeper;

        public AbstractLobbySessionImpl(Keeper keeper) {
            this.keeper = keeper;
        }

        public Keeper getKeeper() {
            return keeper;
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

        public LobbySessionImpl(HostedConnection conn, Keeper keeper) {
            super(keeper);
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
            getKeeper().setReady(ready);
            getCallback().onPlayerListChanged(getPlayers());
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
        public List<Keeper> getPlayers() {
            List<Keeper> keepers = new ArrayList<>(players.keySet());
            Collections.sort(keepers);
            return keepers;
        }

        @Override
        public void onPlayerListChanged(List<Keeper> players) {
            getCallback().onPlayerListChanged(players);
        }

    }
}
