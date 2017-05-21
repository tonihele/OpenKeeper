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
package toniarts.openkeeper.game.network.game;

import com.jme3.network.HostedConnection;
import com.jme3.network.service.AbstractHostedConnectionService;
import com.jme3.network.service.HostedServiceManager;
import com.jme3.network.service.rmi.RmiHostedService;
import com.jme3.network.service.rmi.RmiRegistry;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.game.map.MapData;
import toniarts.openkeeper.game.network.NetworkConstants;
import static toniarts.openkeeper.game.network.lobby.LobbyHostedService.ATTRIBUTE_KEEPER_ID;
import toniarts.openkeeper.game.state.session.GameSession;
import toniarts.openkeeper.game.state.session.GameSessionListener;
import toniarts.openkeeper.game.state.session.GameSessionService;

/**
 * Game server hosts lobby service for the game clients.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class GameHostedService extends AbstractHostedConnectionService implements GameSessionService {

    private static final Logger logger = Logger.getLogger(GameHostedService.class.getName());

    private static final String ATTRIBUTE_SESSION = "game.session";
    private final Map<Short, GameSessionImpl> players = new ConcurrentHashMap<>(4, 0.75f, 5);
    private RmiHostedService rmiService;

    /**
     * Creates a new lobby service that will use the default reliable channel
     * for reliable communication.
     */
    public GameHostedService() {
        super(false);
    }

    private GameSessionImpl getGameSession(HostedConnection conn) {
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
     * generated player name.
     */
    @Override
    public void startHostingOnConnection(HostedConnection conn) {
        logger.log(Level.FINER, "startHostingOnConnection({0})", conn);

        short keeperId = conn.getAttribute(ATTRIBUTE_KEEPER_ID);
        GameSessionImpl session = new GameSessionImpl(conn, keeperId);
        players.put(keeperId, session);

        // Expose the session as an RMI resource to the client
        RmiRegistry rmi = rmiService.getRmiRegistry(conn);
        rmi.share(NetworkConstants.GAME_CHANNEL, session, GameSession.class);
    }

    @Override
    public void stopHostingOnConnection(HostedConnection conn) {
        logger.log(Level.FINER, "stopHostingOnConnection({0})", conn);
        GameSessionImpl player = getGameSession(conn);
        if (player != null) {

            // Then we are still hosting on the connection... it's
            // possible that stopHostingOnConnection() is called more than
            // once for a particular connection since some other game code
            // may call it and it will also be called during connection shutdown.
            conn.setAttribute(ATTRIBUTE_SESSION, null);

            // Remove player session from the active sessions list
            players.remove(player.getKeeperId());
        }
    }

    @Override
    public void sendGameData(MapData mapData) {
        for (GameSessionImpl gameSession : players.values()) {
            gameSession.onGameDataLoaded(mapData);
        }
    }

    @Override
    public void startGame() {
        for (GameSessionImpl gameSession : players.values()) {
            gameSession.onGameStarted();
        }
    }

    /**
     * The connection-specific 'host' for the LobbySession. For convenience this
     * also implements the LobbySessionListener. Since the methods don't collide
     * at all it's convenient for our other code not to have to worry about the
     * internal delegate.
     */
    private class GameSessionImpl implements GameSession, GameSessionListener {

        private final HostedConnection conn;
        private final short keeperId;
        private GameSessionListener callback;

        public GameSessionImpl(HostedConnection conn, short keeperId) {
            this.conn = conn;
            this.keeperId = keeperId;

            // Note: at this point we won't be able to look up the callback
            // because we haven't received the client's RMI shared objects yet.
        }

        protected short getKeeperId() {
            return keeperId;
        }

        protected GameSessionListener getCallback() {
            if (callback == null) {
                RmiRegistry rmi = rmiService.getRmiRegistry(conn);
                callback = rmi.getRemoteObject(GameSessionListener.class);
                if (callback == null) {
                    throw new RuntimeException("Unable to locate client callback for GameSessionListener");
                }
            }
            return callback;
        }

        @Override
        public void loadComplete() {
            for (GameSessionImpl gameSession : players.values()) {
                gameSession.onLoadComplete(keeperId);
            }
        }

        @Override
        public void loadStatus(float progress) {
            for (GameSessionImpl gameSession : players.values()) {
                gameSession.onLoadStatusUpdate(progress, keeperId);
            }
        }

        @Override
        public void onGameDataLoaded(MapData mapData) {
            getCallback().onGameDataLoaded(mapData);
        }

        @Override
        public void onGameStarted() {
            getCallback().onGameStarted();
        }

        @Override
        public void onLoadComplete(short keeperId) {
            getCallback().onLoadComplete(keeperId);
        }

        @Override
        public void onLoadStatusUpdate(float progress, short keeperId) {
            getCallback().onLoadStatusUpdate(progress, keeperId);
        }

    }
}
