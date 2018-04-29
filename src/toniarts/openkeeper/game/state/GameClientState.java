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
import com.jme3.app.state.AppStateManager;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.controller.MapController;
import toniarts.openkeeper.game.controller.player.PlayerSpell;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.MapData;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.game.state.loading.IPlayerLoadingProgress;
import toniarts.openkeeper.game.state.loading.MultiplayerLoadingState;
import toniarts.openkeeper.game.state.loading.SingleBarLoadingState;
import toniarts.openkeeper.game.state.lobby.ClientInfo;
import toniarts.openkeeper.game.state.session.GameSessionClientService;
import toniarts.openkeeper.game.state.session.GameSessionListener;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.view.PlayerEntityViewState;
import toniarts.openkeeper.view.PlayerMapViewState;

/**
 * The game client state
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class GameClientState extends AbstractPauseAwareState {

    private Main app;

    private AppStateManager stateManager;

    private final KwdFile kwdFile;

    private final Map<Short, Keeper> players = new TreeMap<>();
    private final Object loadingObject = new Object();
    private volatile boolean gameStarted = false;

    //private final GameSessionService gameService;
    private final Short playerId;
    private IPlayerLoadingProgress loadingState;
    private final GameSessionClientService gameClientService;
    private final GameSessionListenerImpl gameSessionListener = new GameSessionListenerImpl();
    private IMapInformation mapClientService;
    private PlayerState playerState;

    private PlayerMapViewState playerMapViewState;
    private PlayerEntityViewState playerModelViewState;

    private static final Logger LOGGER = Logger.getLogger(GameClientState.class.getName());

    /**
     * Single use game states
     *
     * @param level the level to load
     * @param playerId our player ID
     * @param players players participating in this game
     * @param gameClientService client services
     */
    public GameClientState(KwdFile level, Short playerId, List<ClientInfo> players, GameSessionClientService gameClientService) {
        this.kwdFile = level;
        // this.gameService = gameService;
        this.gameClientService = gameClientService;
        this.playerId = playerId;

        // Create the loading state
        loadingState = createLoadingState(players);

        // Add the listener
        gameClientService.addGameSessionListener(gameSessionListener);

        // Tell that we are ready to start receiving game data
        gameClientService.markReady();
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        this.app = (Main) app;
        this.stateManager = stateManager;

        // Attach the loading state finally
        if (!gameStarted) {
            synchronized (loadingObject) {
                if (!gameStarted) {
                    stateManager.attach(loadingState);
                }
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

//        stateManager.getState(WorldState.class).setEnabled(enabled);
    }

    private void detachRelatedAppStates() {
//        stateManager.detach(stateManager.getState(WorldState.class));
//        stateManager.detach(stateManager.getState(SoundState.class));
    }

    /**
     * If you are getting rid of the game state, use this so that all the
     * related states are detached on the same render loop. Otherwise the app
     * might crash.
     */
    public void detach() {

        // If we are stuck loading
        synchronized (loadingObject) {
            loadingObject.notifyAll();
        }

        stateManager.detach(this);
        detachRelatedAppStates();
    }

    @Override
    public void cleanup() {

        // Detach
        detach();

        super.cleanup();
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);
//        if (actionPointState != null) {
//            actionPointState.updateControls(tpf);
//        }
//        timeTaken += tpf;
    }

    @Override
    public boolean isPauseable() {
        return true;
    }

    private IPlayerLoadingProgress createLoadingState(List<ClientInfo> players) {

        // See if multiplayer or not
        boolean multiplayer = false;
        int humanPlayers = 0;
        for (ClientInfo clientInfo : players) {
            if (!clientInfo.getKeeper().isAi()) {
                humanPlayers++;
                if (humanPlayers > 1) {
                    multiplayer = true;
                    break;
                }
            }
        }

        // Create the appropriate loaging screen
        IPlayerLoadingProgress loader;
        if (multiplayer) {
            loader = new MultiplayerLoadingState() {

                @Override
                public Void onLoad() {

                    return onGameLoad();
                }

                @Override
                public void onLoadComplete() {

                    onGameLoadComplete();
                }
            };
        } else {
            loader = new SingleBarLoadingState() {

                @Override
                public Void onLoad() {

                    return onGameLoad();
                }

                @Override
                public void onLoadComplete() {

                    onGameLoadComplete();
                }
            };
        }
        return loader;
    }

    private void onGameLoadComplete() {

        // Set initialized
        GameClientState.this.initialized = true;

        // Set the processors
        GameClientState.this.app.setViewProcessors();
    }

    private Void onGameLoad() {
        try {

            // The game is actually loaded elsewhere but for the visuals we need this
            if (!gameStarted) {
                synchronized (loadingObject) {
                    if (!gameStarted) {
                        loadingObject.wait();
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load the game!", e);
        }

        return null;
    }

    /**
     * Get the level raw data file
     *
     * @return the KWD
     */
    public KwdFile getLevelData() {
        return kwdFile;
    }

    public Keeper getPlayer(short playerId) {
        return players.get(playerId);
    }

    public Collection<Keeper> getPlayers() {
        return players.values();
    }

    /**
     * Get level variable value
     *
     * @param variable the variable type
     * @return variable value
     */
    public float getLevelVariable(Variable.MiscVariable.MiscType variable) {
        // TODO: player is able to change these, so need a wrapper and store these to GameState
        return kwdFile.getVariables().get(variable).getValue();
    }

    private class GameSessionListenerImpl implements GameSessionListener {

        @Override
        public void onGameDataLoaded(Collection<Keeper> players, MapData mapData) {

            // Now we have the game data, start loading the map
            kwdFile.load();
            AssetUtils.prewarmAssets(kwdFile, app.getAssetManager(), app);
            for (Keeper keeper : players) {
                GameClientState.this.players.put(keeper.getId(), keeper);
            }
            mapClientService = new MapController(mapData, kwdFile);
            playerModelViewState = new PlayerEntityViewState(kwdFile, app.getAssetManager(), gameClientService.getEntityData(), playerId);
            playerMapViewState = new PlayerMapViewState(kwdFile, app.getAssetManager(), mapClientService, playerId) {

                private float lastProgress = 0;

                @Override
                protected void updateProgress(float progress) {

                    // Update ourselves
                    onLoadStatusUpdate(progress, playerId);

                    if (progress - lastProgress >= 0.01f) {
                        gameClientService.loadStatus(progress);
                        lastProgress = progress;
                    }
                }
            };

            // Prewarm the whole scene
            app.getRenderManager().preloadScene(app.getRootNode());

            // Signal our readiness
            gameClientService.loadComplete();
        }

        @Override
        public void onLoadComplete(short keeperId) {
            loadingState.setProgress(1f, keeperId);
        }

        @Override
        public void onLoadStatusUpdate(float progress, short keeperId) {
            loadingState.setProgress(progress, keeperId);
        }

        @Override
        public void onGameStarted() {

            // Release loading state from memory
            loadingState = null;

            // Set the player stuff
            playerState = stateManager.getState(PlayerState.class);
            playerState.setKwdFile(kwdFile);
            playerState.setEntityData(gameClientService.getEntityData());
            playerState.setPlayerId(playerId);

            app.enqueue(() -> {
                playerState.setEnabled(true);
                stateManager.attach(playerMapViewState);
                stateManager.attach(playerModelViewState);

                // Release the lock and enter to the game phase
                synchronized (loadingObject) {
                    gameStarted = true;
                    loadingObject.notifyAll();
                }
            });
        }

        @Override
        public void onTilesChange(List<MapTile> updatedTiles) {
            mapClientService.setTiles(updatedTiles);
            playerMapViewState.onTilesChange(updatedTiles);
        }

        @Override
        public void onAdded(PlayerSpell spell) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void onRemoved(PlayerSpell spell) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void onResearchStatusChanged(PlayerSpell spell) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void onGoldChange(short keeperId, int gold) {
            players.get(keeperId).setGold(gold);

            // FIXME: See in what thread we are
            if (playerState != null && playerState.getPlayerId() == keeperId) {
                app.enqueue(() -> {
                    playerState.onGoldChange(keeperId, gold);
                });
            }
        }

        @Override
        public void onManaChange(short keeperId, int mana, int manaLoose, int manaGain) {
            Keeper keeper = players.get(keeperId);
            keeper.setMana(mana);
            keeper.setManaGain(manaGain);
            keeper.setManaLoose(manaLoose);

            // FIXME: See in what thread we are
            if (playerState != null && playerState.getPlayerId() == keeperId) {
                app.enqueue(() -> {
                    playerState.onManaChange(keeperId, mana, manaLoose, manaGain);
                });
            }
        }

        @Override
        public void onBuild(short keeperId, List<MapTile> tiles) {
            playerMapViewState.onBuild(keeperId, tiles);
        }

        @Override
        public void onSold(short keeperId, List<MapTile> tiles) {
            playerMapViewState.onSold(keeperId, tiles);
        }

    }

    public IMapInformation getMapClientService() {
        return mapClientService;
    }

    public GameSessionClientService getGameClientService() {
        return gameClientService;
    }

}
