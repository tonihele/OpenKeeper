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

import com.jme3.network.service.AbstractClientService;
import com.jme3.network.service.ClientServiceManager;
import com.jme3.network.service.rmi.RmiClientService;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import toniarts.openkeeper.game.network.NetworkServer;

/**
 * Client side service for the game lobby services
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class LobbyClientService extends AbstractClientService
        implements LobbySession {

    private static final Logger logger = Logger.getLogger(LobbyClientService.class.getName());

    private RmiClientService rmiService;
    private LobbySession delegate;

    private final LobbySessionCallback sessionCallback = new LobbySessionCallback();
    private final List<LobbySessionListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void setReady(boolean ready) {
        getDelegate().setReady(ready);
    }

    @Override
    public List<ClientInfo> getPlayers() {
        return getDelegate().getPlayers();
    }

    @Override
    public String getMap() {
        return getDelegate().getMap();
    }

    /**
     * Adds a listener that will be notified about account-related events. Note
     * that these listeners are called on the networking thread and as such are
     * not suitable for modifying the visualization directly.
     */
    public void addLobbySessionListener(LobbySessionListener l) {
        listeners.add(l);
    }

    public void removeLobbySessionListener(LobbySessionListener l) {
        listeners.remove(l);
    }

    @Override
    protected void onInitialize(ClientServiceManager s) {
        logger.log(Level.FINER, "onInitialize({0})", s);
        this.rmiService = getService(RmiClientService.class);
        if (rmiService == null) {
            throw new RuntimeException("LobbyClientService requires RMI service");
        }
        logger.finer("Sharing session callback.");
        rmiService.share(NetworkServer.LOBBY_CHANNEL, sessionCallback, LobbySessionListener.class);
    }

    /**
     * Called during connection setup once the server-side services have been
     * initialized for this connection and any shared objects, etc. should be
     * available.
     */
    @Override
    public void start() {
        logger.finer("start()");
        super.start();
    }

    private LobbySession getDelegate() {
        // We look up the delegate lazily to make the service more
        // flexible.  This way we don't have to know anything about the
        // connection lifecycle and can simply report an error if the
        // game is doing something screwy.
        if (delegate == null) {
            // Look it up
            this.delegate = rmiService.getRemoteObject(LobbySession.class);
            logger.log(Level.FINER, "delegate:{0}", delegate);
            if (delegate == null) {
                throw new RuntimeException("No lobby session found");
            }
        }
        return delegate;
    }

    /**
     * Shared with the server over RMI so that it can notify us about account
     * related stuff.
     */
    private class LobbySessionCallback implements LobbySessionListener {

        @Override
        public void onMapChanged(String mapName) {
            logger.log(Level.FINEST, "mapChanged({0})", new Object[]{mapName});
            for (LobbySessionListener l : listeners) {
                l.onMapChanged(mapName);
            }
        }

        @Override
        public void onPlayerListChanged(List<ClientInfo> players) {
            logger.log(Level.FINEST, "onPlayerListChanged({0})", new Object[]{players.stream().map(Object::toString)
                .collect(Collectors.joining(", "))});
            for (LobbySessionListener l : listeners) {
                l.onPlayerListChanged(players);
            }
        }
    }
}
