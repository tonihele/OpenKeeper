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
package toniarts.openkeeper.game.state.lobby;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.game.MapSelector;
import toniarts.openkeeper.utils.Utils;

/**
 * A lobby state, abstracts game lobby related actions, acts a lifetime lobby
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class LobbyState extends AbstractAppState {

    private Application app;
    private final boolean online;
    private final LobbyService lobbyService;
    private final LobbyClientService lobbyClientService;
    private final Map<LobbySessionListener, SafeLobbySessionListener> listeners = new HashMap<>();
    private final String gameName;
    private Thread renderThread;
    private final MapSelector mapSelector;

    public LobbyState(boolean online, String gameName, LobbyService lobbyService, LobbyClientService lobbyClientService, MapSelector mapSelector) {
        this.online = online;
        this.lobbyService = lobbyService;
        this.lobbyClientService = lobbyClientService;
        if (online) {
            this.gameName = gameName;
        } else {
            this.gameName = Utils.getMainTextResourceBundle().getString("141");
        }
        this.mapSelector = mapSelector;

        // Set the map
        mapSelector.setSkirmish(!online);

        // We as the host should set the initial map
        if (lobbyService != null) {
            lobbyService.setMap(mapSelector.getMap().getMapName(), mapSelector.getMap().getMap().getGameLevel().getPlayerCount());
        }
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = app;

        renderThread = Thread.currentThread();
    }

    public LobbyService getLobbyService() {
        return lobbyService;
    }

    public LobbySession getLobbySession() {
        return lobbyClientService;
    }

    public boolean isOnline() {
        return online;
    }

    public boolean isHosting() {
        return lobbyService != null;
    }

    public String getGameName() {
        return gameName;
    }

    public void addLobbySessionListener(LobbySessionListener l) {
        SafeLobbySessionListener listener = new SafeLobbySessionListener(this, l);
        listeners.put(l, listener);
        lobbyClientService.addLobbySessionListener(listener);
    }

    public void removeLobbySessionListener(LobbySessionListener l) {
        SafeLobbySessionListener listener = listeners.remove(l);
        lobbyClientService.removeLobbySessionListener(listener);
    }

    private boolean isRenderThread() {
        return Thread.currentThread() == renderThread;
    }

    public void setRandomMap() {
        mapSelector.random();
        lobbyService.setMap(mapSelector.getMap().getMapName(), mapSelector.getMap().getMap().getGameLevel().getPlayerCount());
    }

    public void setMap(int selectedMapIndex) {
        mapSelector.selectMap(selectedMapIndex);
        lobbyService.setMap(mapSelector.getMap().getMapName(), mapSelector.getMap().getMap().getGameLevel().getPlayerCount());
    }

    /**
     * Small class to filter all the notifications to the render thread
     */
    private static class SafeLobbySessionListener implements LobbySessionListener {

        private final LobbyState lobbyState;
        private final LobbySessionListener listener;

        public SafeLobbySessionListener(LobbyState lobbyState, LobbySessionListener listener) {
            this.lobbyState = lobbyState;
            this.listener = listener;
        }

        @Override
        public void onPlayerListChanged(List<ClientInfo> players) {
            if (lobbyState.isRenderThread()) {
                listener.onPlayerListChanged(players);
            } else {
                lobbyState.app.enqueue(() -> {
                    listener.onPlayerListChanged(players);
                });
            }
        }

        @Override
        public void onMapChanged(String mapName) {
            if (lobbyState.isRenderThread()) {
                listener.onMapChanged(mapName);
            } else {
                lobbyState.app.enqueue(() -> {
                    listener.onMapChanged(mapName);
                });
            }
        }
    }

}
