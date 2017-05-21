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
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.action.ActionPointState;
import toniarts.openkeeper.game.data.GameResult;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.GameLogicThread;
import toniarts.openkeeper.game.map.MapData;
import toniarts.openkeeper.game.state.loading.MultiplayerLoadingState;
import toniarts.openkeeper.game.state.session.GameSessionClientService;
import toniarts.openkeeper.game.state.session.GameSessionListener;
import toniarts.openkeeper.game.state.session.GameSessionService;
import toniarts.openkeeper.game.task.TaskManager;
import toniarts.openkeeper.game.trigger.TriggerControl;
import toniarts.openkeeper.game.trigger.creature.CreatureTriggerState;
import toniarts.openkeeper.game.trigger.door.DoorTriggerState;
import toniarts.openkeeper.game.trigger.object.ObjectTriggerState;
import toniarts.openkeeper.game.trigger.party.PartyTriggerState;
import toniarts.openkeeper.tools.convert.map.KwdFile;
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

    private final GameSessionService gameService;
    private final GameSessionClientService gameClientService;
    private final GameSessionListenerImpl gameSessionListener = new GameSessionListenerImpl();

    private static final Logger logger = Logger.getLogger(GameClientState.class.getName());

    /**
     * Single use game states
     *
     * @param level the level to load
     * @param players players participating in this game
     */
    public GameClientState(KwdFile level, GameSessionService gameService, GameSessionClientService gameClientService) {
        this.kwdFile = level;
        this.gameService = gameService;
        this.gameClientService = gameClientService;

        gameClientService.addGameSessionListener(gameSessionListener);
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        this.app = (Main) app;
        this.stateManager = stateManager;

        // Set up the loading screen
        MultiplayerLoadingState loader = new MultiplayerLoadingState() {

            @Override
            public Void onLoad() {

                try {

                    // The game is actually loaded elsewhere but for the visuals we need this
                    synchronized (loadingObject) {
                        if (!gameStarted) {
                            loadingObject.wait();
                        }
                    }

                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to load the game!", e);
                }

                return null;
            }

            @Override
            public void onLoadComplete() {

                // Prewarm the whole scene
//                GameClientState.this.app.getRenderManager().preloadScene(rootNode);
//
//                // Enable player state
//                GameClientState.this.stateManager.getState(PlayerState.class).setEnabled(true);
//                GameClientState.this.stateManager.getState(SoundState.class).setEnabled(true);
//
//                // Set initialized
//                GameClientState.this.initialized = true;
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
        };
        stateManager.attach(loader);
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

    private class GameSessionListenerImpl implements GameSessionListener {

        @Override
        public void onGameDataLoaded(MapData mapData) {

            // Now we have the game data, start loading the map
            PlayerMapViewState playerMapViewState = new PlayerMapViewState(kwdFile, app.getAssetManager(), mapData) {

                @Override
                protected void updateProgress(float progress) {
//                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
            };
        }

        @Override
        public void onLoadComplete(short keeperId) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void onLoadStatusUpdate(float progress, short keeperId) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void onGameStarted() {

            // Release the lock and enter to the game phase
            synchronized (loadingObject) {
                gameStarted = true;
                loadingObject.notifyAll();
            }
        }

    }

}
