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
import static java.util.stream.Collectors.toList;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.MapSelector;
import toniarts.openkeeper.game.state.ConnectionState;
import toniarts.openkeeper.game.state.GameClientState;
import toniarts.openkeeper.game.state.GameServerState;
import toniarts.openkeeper.game.state.MainMenuState;
import toniarts.openkeeper.game.state.session.GameSessionClientService;
import toniarts.openkeeper.game.state.session.GameSessionServerService;
import toniarts.openkeeper.game.state.session.LocalGameSession;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Utils;

/**
 * A lobby state, abstracts game lobby related actions, acts a lifetime lobby
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class LobbyState extends AbstractAppState {

    private Main app;
    private AppStateManager stateManager;
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
            lobbyService.setMap(mapSelector.getMap().mapName(), mapSelector.getMap().map().getGameLevel().getPlayerCount());
        }
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;
        this.stateManager = stateManager;

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
        lobbyService.setMap(mapSelector.getMap().mapName(), mapSelector.getMap().map().getGameLevel().getPlayerCount());
    }

    public void setMap(int selectedMapIndex) {
        mapSelector.selectMap(selectedMapIndex);
        lobbyService.setMap(mapSelector.getMap().mapName(), mapSelector.getMap().map().getGameLevel().getPlayerCount());
    }

    private void startGame(List<ClientInfo> players) {
        GameSessionServerService gameSessionService;
        GameSessionClientService gameClientService;

        // See if we can fall back to local server
        boolean fallback = false;
        if (isOnline()) {
            fallback = true;
            int humanPlayers = 0;
            for (ClientInfo clientInfo : players) {
                if (!clientInfo.getKeeper().isAi()) {
                    humanPlayers++;
                    if (humanPlayers > 1) {
                        fallback = false;
                        break;
                    }
                }
            }
        }

        IKwdFile kwdFile = mapSelector.getMap().map(); // This might get read twice on the hosting machine
        if (isOnline() && !fallback) {
            gameSessionService = stateManager.getState(ConnectionState.class).getGameSessionServerService();
            gameClientService = stateManager.getState(ConnectionState.class).getGameClientService();
        } else {
            LocalGameSession gameSession = new LocalGameSession();
            gameSessionService = gameSession;
            gameClientService = gameSession;

            // If fall back, close the connections
            if (fallback) {
                stateManager.getState(ConnectionState.class).disconnect();
            }
        }

        // The client
        GameClientState gameClientState = new GameClientState(kwdFile, players.stream().filter(c -> c.getId() == lobbyClientService.getPlayerId()).findFirst().get().getKeeper().getId(), players, gameClientService, app);
        stateManager.attach(gameClientState);

        // The game server
        if (isHosting()) {
            GameServerState gameServerState = new GameServerState(kwdFile, players.stream().map(ClientInfo::getKeeper).collect(toList()), false, gameSessionService);
            stateManager.attach(gameServerState);
        }

        // Disable the main menu and get rid of us
        if (isRenderThread()) {
            stateManager.getState(MainMenuState.class).setEnabled(false);
        } else {
            app.enqueue(() -> {
                stateManager.getState(MainMenuState.class).setEnabled(false);
            });
        }
        stateManager.detach(this);
    }

    /**
         * Small class to filter all the notifications to the render thread
         */
        private record SafeLobbySessionListener(LobbyState lobbyState,
                                                LobbySessionListener listener) implements LobbySessionListener {

        @Override
            public void onPlayerListChanged(List<ClientInfo> players) {
                runOnRenderThread(() -> {
                    listener.onPlayerListChanged(players);
                });

                // Start game if we are all ready
                if (lobbyState.isHosting()) {
                    for (ClientInfo clientInfo : players) {
                        if (!clientInfo.isReady()) {
                            return;
                        }
                    }
                    lobbyState.getLobbyService().startGame();
                }
            }

            @Override
            public void onMapChanged(String mapName) {
                runOnRenderThread(() -> {
                    listener.onMapChanged(mapName);
                });
            }

            @Override
            public void onGameStarted(String mapName, List<ClientInfo> players) {
                runOnRenderThread(() -> {
                    listener.onGameStarted(mapName, players);
                });
                lobbyState.startGame(players);
            }

            private void runOnRenderThread(Runnable runnable) {
                if (lobbyState.isRenderThread()) {
                    runnable.run();
                } else {
                    lobbyState.app.enqueue(runnable);
                }
            }
        }

}
