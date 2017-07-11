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

import com.jme3.math.Vector2f;
import com.jme3.network.HostedConnection;
import com.jme3.network.service.AbstractHostedConnectionService;
import com.jme3.network.service.HostedServiceManager;
import com.jme3.network.service.rmi.RmiHostedService;
import com.jme3.network.service.rmi.RmiRegistry;
import com.jme3.util.SafeArrayList;
import com.simsilica.es.EntityData;
import com.simsilica.es.server.EntityDataHostedService;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.game.controller.player.PlayerSpell;
import toniarts.openkeeper.game.map.MapData;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.game.network.NetworkConstants;
import toniarts.openkeeper.game.network.streaming.StreamingHostedService;
import toniarts.openkeeper.game.state.lobby.ClientInfo;
import toniarts.openkeeper.game.state.session.GameSession;
import toniarts.openkeeper.game.state.session.GameSessionListener;
import toniarts.openkeeper.game.state.session.GameSessionServerService;
import toniarts.openkeeper.game.state.session.GameSessionServiceListener;

/**
 * Game server hosts lobby service for the game clients.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class GameHostedService extends AbstractHostedConnectionService implements GameSessionServerService {

    /**
     * Someone is listening on the other end, for that we need a message type
     */
    public enum MessageType {
        MAP_DATA;
    }

    private static final Logger logger = Logger.getLogger(GameHostedService.class.getName());

    private boolean readyToLoad = false;
    private final Object loadLock = new Object();
    private static final String ATTRIBUTE_SESSION = "game.session";
    private final Map<ClientInfo, GameSessionImpl> players = new ConcurrentHashMap<>(4, 0.75f, 5);
    private final List<GameSessionServiceListener> serverListeners = new SafeArrayList<>(GameSessionServiceListener.class);
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
    public void startHostingOnConnection(HostedConnection conn, ClientInfo clientInfo) {
        logger.log(Level.FINER, "startHostingOnConnection({0})", conn);

        GameSessionImpl session = new GameSessionImpl(conn, clientInfo);
        players.put(clientInfo, session);

        // Expose the session as an RMI resource to the client
        RmiRegistry rmi = rmiService.getRmiRegistry(conn);
        rmi.share(NetworkConstants.GAME_CHANNEL, session, GameSession.class);
    }

    @Override
    public void startHostingOnConnection(HostedConnection conn) {
        throw new UnsupportedOperationException("You need to supply the clientInfo!");
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
            players.remove(player.clientInfo);
        }
    }

    @Override
    public void addGameSessionServiceListener(GameSessionServiceListener l) {
        serverListeners.add(l);
    }

    @Override
    public void removeGameSessionServiceListener(GameSessionServiceListener l) {
        serverListeners.remove(l);
    }

    @Override
    public EntityData getEntityData() {
        return getServiceManager().getService(EntityDataHostedService.class).getEntityData();
    }

    @Override
    public void sendGameData(MapData mapData) {
        Thread thread = new Thread(() -> {

            if (!readyToLoad) {
                synchronized (loadLock) {
                    if (!readyToLoad) {
                        try {
                            loadLock.wait();
                        } catch (InterruptedException ex) {
                            logger.log(Level.SEVERE, "Game data sender interrupted!", ex);
                            return;
                        }
                    }
                }
            }

            // We must block this until all clients are ready to receive
            try {

                // Data is too big, stream the data
                getServiceManager().getService(StreamingHostedService.class).sendData(MessageType.MAP_DATA.ordinal(), mapData, null);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }

        }, "GameDataSender");
        thread.start();
    }

    @Override
    public void startGame() {
        for (GameSessionImpl gameSession : players.values()) {
            gameSession.onGameStarted();
        }
    }

    @Override
    public void updateTiles(List<MapTile> updatedTiles) {
        for (GameSessionImpl gameSession : players.values()) {
            gameSession.onTilesChange(updatedTiles);
        }
    }

    @Override
    public void onAdded(PlayerSpell spell) {
        for (GameSessionImpl gameSession : players.values()) {
            gameSession.onAdded(spell);
        }
    }

    @Override
    public void onRemoved(PlayerSpell spell) {
        for (GameSessionImpl gameSession : players.values()) {
            gameSession.onRemoved(spell);
        }
    }

    @Override
    public void onResearchStatusChanged(PlayerSpell spell) {
        for (GameSessionImpl gameSession : players.values()) {
            gameSession.onResearchStatusChanged(spell);
        }
    }

    @Override
    public void onGoldChange(short keeperId, int gold) {
        for (GameSessionImpl gameSession : players.values()) {
            gameSession.onGoldChange(keeperId, gold);
        }
    }

    @Override
    public void onManaChange(short keeperId, int mana, int manaLoose, int manaGain) {
        for (GameSessionImpl gameSession : players.values()) {
            gameSession.onManaChange(keeperId, mana, manaLoose, manaGain);
        }
    }

    /**
     * The connection-specific 'host' for the GameSession. For convenience this
     * also implements the GameSessionListener. Since the methods don't collide
     * at all it's convenient for our other code not to have to worry about the
     * internal delegate.
     */
    private class GameSessionImpl implements GameSession, GameSessionListener {

        private final HostedConnection conn;
        private final ClientInfo clientInfo;
        private GameSessionListener callback;

        public GameSessionImpl(HostedConnection conn, ClientInfo clientInfo) {
            this.conn = conn;
            this.clientInfo = clientInfo;

            // Note: at this point we won't be able to look up the callback
            // because we haven't received the client's RMI shared objects yet.
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
            clientInfo.setLoaded(true);
            boolean allLoaded = true;
            for (GameSessionImpl gameSession : players.values()) {
                if (!gameSession.clientInfo.isLoaded()) {
                    allLoaded = false;
                }
                gameSession.onLoadComplete(clientInfo.getKeeper().getId());
            }

            // Start the game if everyone has loaded
            if (allLoaded) {
                startGame();
            }
        }

        @Override
        public void loadStatus(float progress) {
            clientInfo.setLoadingProgress(progress);

            // FIXME: we need to change this to messages or something, it really saturates the connection/channel/etc
            // and nothing works then
//            for (GameSessionImpl gameSession : players.values()) {
//                gameSession.onLoadStatusUpdate(progress, clientInfo.getKeeper().getId());
//            }
        }

        @Override
        public void onGameDataLoaded(MapData mapData) {

            // We send this as a streamed message, to all, super big
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

        @Override
        public void onTilesChange(List<MapTile> updatedTiles) {
            getCallback().onTilesChange(updatedTiles);
        }

        @Override
        public void markReady() {
            clientInfo.setReadyToLoad(true);

            for (GameSessionImpl gameSession : players.values()) {
                if (!gameSession.clientInfo.isReadyToLoad()) {
                    return;
                }
            }

            // Let the game loader continue
            readyToLoad = true;
            synchronized (loadLock) {
                loadLock.notifyAll();
            }
        }

        @Override
        public void selectTiles(Vector2f start, Vector2f end, boolean select) {
            for (GameSessionServiceListener listener : serverListeners) {
                listener.onSelectTiles(start, end, select, clientInfo.getKeeper().getId());
            }
        }

        @Override
        public EntityData getEntityData() {
            return null; // Cached on client...
        }

        @Override
        public void onAdded(PlayerSpell spell) {
            getCallback().onAdded(spell);
        }

        @Override
        public void onRemoved(PlayerSpell spell) {
            getCallback().onRemoved(spell);
        }

        @Override
        public void onResearchStatusChanged(PlayerSpell spell) {
            getCallback().onResearchStatusChanged(spell);
        }

        @Override
        public void onGoldChange(short keeperId, int gold) {
            getCallback().onGoldChange(keeperId, gold);
        }

        @Override
        public void onManaChange(short keeperId, int mana, int manaLoose, int manaGain) {
            getCallback().onManaChange(keeperId, mana, manaLoose, manaGain);
        }

    }
}
