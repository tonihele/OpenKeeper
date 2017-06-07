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
import com.jme3.math.Vector2f;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.action.ActionPointState;
import toniarts.openkeeper.game.controller.MapClientService;
import toniarts.openkeeper.game.controller.MapController;
import toniarts.openkeeper.game.data.GameResult;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.GameLogicThread;
import toniarts.openkeeper.game.map.MapData;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.game.state.loading.IPlayerLoadingProgress;
import toniarts.openkeeper.game.state.loading.MultiplayerLoadingState;
import toniarts.openkeeper.game.state.loading.SingleBarLoadingState;
import toniarts.openkeeper.game.state.lobby.ClientInfo;
import toniarts.openkeeper.game.state.session.GameSessionClientService;
import toniarts.openkeeper.game.state.session.GameSessionListener;
import toniarts.openkeeper.game.task.TaskManager;
import toniarts.openkeeper.game.trigger.TriggerControl;
import toniarts.openkeeper.game.trigger.creature.CreatureTriggerState;
import toniarts.openkeeper.game.trigger.door.DoorTriggerState;
import toniarts.openkeeper.game.trigger.object.ObjectTriggerState;
import toniarts.openkeeper.game.trigger.party.PartyTriggerState;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.PauseableScheduledThreadPoolExecutor;
import toniarts.openkeeper.view.PlayerMapViewState;

/**
 * The game client state
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class GameClientState extends AbstractPauseAwareState {

    public static final int LEVEL_TIMER_MAX_COUNT = 16;
    private static final int LEVEL_FLAG_MAX_COUNT = 128;
    private static final float MOVEMENT_UPDATE_TPF = 0.02f;

    private Main app;

//    private ExecutorService loader = Executors.newSingleThreadExecutor();
    private AppStateManager stateManager;

    private KwdFile kwdFile;

    private GameLogicThread gameLogicThread;
    private TriggerControl triggerControl = null;
    private CreatureTriggerState creatureTriggerState;
    private ObjectTriggerState objectTriggerState;
    private DoorTriggerState doorTriggerState;
    private PartyTriggerState partyTriggerState;
    private ActionPointState actionPointState;
    // TODO What timer class we should take ?
    private int levelScore = 0;
    private boolean campaign;

    private GameResult gameResult = null;
    private float timeTaken = 0;
    private Float timeLimit = null;
    private TaskManager taskManager;
    private final Map<Short, Keeper> players = new TreeMap<>();
    private final Object loadingObject = new Object();
    private volatile boolean gameStarted = false;
    private PauseableScheduledThreadPoolExecutor exec;

    //private final GameSessionService gameService;
    private final Short playerId;
    private IPlayerLoadingProgress loadingState;
    private final GameSessionClientService gameClientService;
    private final GameSessionListenerImpl gameSessionListener = new GameSessionListenerImpl();
    private MapClientService mapClientService;

    private PlayerMapViewState playerMapViewState;

    private static final Logger logger = Logger.getLogger(GameClientState.class.getName());

    /**
     * Single use game states
     *
     * @param level the level to load
     * @param players players participating in this game
     */
    public GameClientState(KwdFile level, Short playerId, List<ClientInfo> players, GameSessionClientService gameClientService) {
        this.kwdFile = level;
        // this.gameService = gameService;
        this.gameClientService = gameClientService;
        this.playerId = playerId;
        for (ClientInfo ci : players) {
            this.players.put(ci.getKeeper().getId(), ci.getKeeper());
        }

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

        // FIXME: Super temp hax
        kwdFile.load();
        for (Keeper keeper : players.values()) {
            keeper.setPlayer(kwdFile.getPlayer(keeper.getId()));
            keeper.initialize(stateManager, app);
        }

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
        // Prewarm the whole scene
//                GameClientState.this.app.getRenderManager().preloadScene(rootNode);
//
//                // Enable player state
//                GameClientState.this.stateManager.getState(PlayerState.class).setEnabled(true);
//                GameClientState.this.stateManager.getState(SoundState.class).setEnabled(true);
//
//                // Set initialized
        GameClientState.this.initialized = true;
//
//                // Set the processors
//                GameClientState.this.app.setViewProcessors();
//
//                // FIXME: this is not correct
//                // Enqueue the thread starting to next frame so that the states are initialized
//                app.enqueue(() -> {
//
//                    // Enable game logic thread
//                    exec.resume();
//
//                    return null;
//                });
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
            logger.log(Level.SEVERE, "Failed to load the game!", e);
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
        public void onGameDataLoaded(MapData mapData) {

            // Now we have the game data, start loading the map
            kwdFile.load();
            final MapClientService localMap = new MapController(mapData, kwdFile);
            mapClientService = new MapClientService() {

                @Override
                public MapData getMapData() {
                    return localMap.getMapData();
                }

                @Override
                public void setTiles(List<MapTile> tiles) {
                    localMap.setTiles(tiles);
                }

                @Override
                public boolean isBuildable(int x, int y, Player player, Room room) {
                    return localMap.isBuildable(x, y, player, room);
                }

                @Override
                public boolean isClaimable(int x, int y, short playerId) {
                    return localMap.isClaimable(x, y, playerId);
                }

                @Override
                public boolean isSelected(int x, int y, short playerId) {
                    return localMap.isSelected(x, y, playerId);
                }

                @Override
                public boolean isTaggable(int x, int y) {
                    return localMap.isTaggable(x, y);
                }

                @Override
                public void selectTiles(Vector2f start, Vector2f end, boolean select, short playerId) {

                    // Relay this to the client service
                    gameClientService.selectTiles(start, end, select);
                }
            };
            playerMapViewState = new PlayerMapViewState(kwdFile, app.getAssetManager(), mapClientService, playerId) {

                @Override
                protected void updateProgress(float progress) {
                    gameClientService.loadStatus(progress);
                }
            };
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
            PlayerState playerState = stateManager.getState(PlayerState.class);
            playerState.setPlayerId(playerId);
            playerState.setEnabled(true);
            stateManager.attach(playerMapViewState);

            // Release the lock and enter to the game phase
            synchronized (loadingObject) {
                gameStarted = true;
                loadingObject.notifyAll();
            }
        }

        @Override
        public void onTilesChange(List<MapTile> updatedTiles) {
            mapClientService.setTiles(updatedTiles);
            playerMapViewState.onTilesChange(updatedTiles);
        }

    }

    public MapClientService getMapClientService() {
        return mapClientService;
    }

}
