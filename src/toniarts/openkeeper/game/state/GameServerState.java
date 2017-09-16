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

import com.badlogic.gdx.ai.GdxAI;
import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.Vector2f;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.GameTimer;
import toniarts.openkeeper.game.action.ActionPointState;
import toniarts.openkeeper.game.controller.GameController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.data.GameResult;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.listener.MapListener;
import toniarts.openkeeper.game.listener.PlayerActionListener;
import toniarts.openkeeper.game.logic.GameLogicThread;
import toniarts.openkeeper.game.logic.IGameLogicUpdateable;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.game.state.session.GameSessionServerService;
import toniarts.openkeeper.game.state.session.GameSessionServiceListener;
import toniarts.openkeeper.game.task.TaskManager;
import toniarts.openkeeper.game.trigger.TriggerControl;
import toniarts.openkeeper.game.trigger.creature.CreatureTriggerState;
import toniarts.openkeeper.game.trigger.door.DoorTriggerState;
import toniarts.openkeeper.game.trigger.object.ObjectTriggerState;
import toniarts.openkeeper.game.trigger.party.PartyTriggerState;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.utils.PauseableScheduledThreadPoolExecutor;
import toniarts.openkeeper.world.WorldState;

/**
 * The game state that actually runs the game. Has no relation to visuals.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class GameServerState extends AbstractPauseAwareState implements IGameLogicUpdateable {

    public static final int LEVEL_TIMER_MAX_COUNT = 16;
    private static final int LEVEL_FLAG_MAX_COUNT = 128;
    private static final float MOVEMENT_UPDATE_TPF = 0.02f;

    private Main app;

    private Thread loader;
    private AppStateManager stateManager;

    private final String level;
    private KwdFile kwdFile;
    private final toniarts.openkeeper.game.data.Level levelObject;

    private GameLogicThread gameLogicThread;
    private TriggerControl triggerControl = null;
    private CreatureTriggerState creatureTriggerState;
    private ObjectTriggerState objectTriggerState;
    private DoorTriggerState doorTriggerState;
    private PartyTriggerState partyTriggerState;
    private ActionPointState actionPointState;
    private final List<Integer> flags = new ArrayList<>(LEVEL_FLAG_MAX_COUNT);
    // TODO What timer class we should take ?
    private final List<GameTimer> timers = new ArrayList<>(LEVEL_TIMER_MAX_COUNT);
    private int levelScore = 0;
    private boolean campaign;
    private final GameSessionServerService gameService;
    private IMapController mapController;
    private final MapListener mapListener = new MapListenerImpl();
    private final GameSessionServiceListener gameSessionListener = new GameSessionServiceListenerImpl();
    private final PlayerActionListener plaerActionListener = new PlayerActionListenerImpl();
    private GameController gameController;

    private GameResult gameResult = null;
    private float timeTaken = 0;
    private Float timeLimit = null;
    private TaskManager taskManager;
    private PauseableScheduledThreadPoolExecutor exec;

    private static final Logger logger = Logger.getLogger(GameServerState.class.getName());

    /**
     * Single use game states
     *
     * @param level the level to load
     * @param players players participating in this game
     */
    public GameServerState(KwdFile level, List<Keeper> players, boolean campaign, GameSessionServerService gameService) {
        this.level = null;
        this.kwdFile = level;
        this.levelObject = null;
        this.campaign = campaign;
        this.gameService = gameService;

        // Add the listener
        gameService.addGameSessionServiceListener(gameSessionListener);

        // Start loading game
        loadGame(players);
    }

    private void loadGame(List<Keeper> players) {
        loader = new GameLoader(players);
        loader.start();
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        this.app = (Main) app;
        this.stateManager = stateManager;

        // Set up the loading screen
//        MultiplayerLoadingState loader = new MultiplayerLoadingState() {
//
//            @Override
//            public Void onLoad() {
//
//                try {
//
//                    // Load the level data
//                    if (level != null) {
//                        kwdFile = new KwdFile(Main.getDkIIFolder(),
//                                new File(ConversionUtils.getRealFileName(Main.getDkIIFolder(), PathUtils.DKII_MAPS_FOLDER + level + ".kwd")));
//                    } else {
//                        kwdFile.load();
//                    }
//                    AssetUtils.prewarmAssets(kwdFile, assetManager, app);
//                    setProgress(0.1f, Player.KEEPER1_ID);
//
//                    // load sounds
////                    loadSounds();
//                    // The players
//                    setupPlayers();
//
//                    // Triggers
//                    partyTriggerState = new PartyTriggerState(true);
//                    partyTriggerState.initialize(stateManager, app);
//                    creatureTriggerState = new CreatureTriggerState(true);
//                    creatureTriggerState.initialize(stateManager, app);
//                    objectTriggerState = new ObjectTriggerState(true);
//                    objectTriggerState.initialize(stateManager, app);
//                    doorTriggerState = new DoorTriggerState(true);
//                    doorTriggerState.initialize(stateManager, app);
//                    actionPointState = new ActionPointState(true);
//                    actionPointState.initialize(stateManager, app);
//                    setProgress(0.20f, Player.KEEPER1_ID);
//                    setProgress(0.20f, Player.KEEPER2_ID);
//                    setProgress(0.20f, Player.KEEPER3_ID);
//                    setProgress(0.20f, Player.KEEPER4_ID);
//
//                    // Create the actual level
////                    WorldState worldState = new WorldState(kwdFile, assetManager, GameServerState.this) {
////                        @Override
////                        protected void updateProgress(float progress) {
////                            setProgress(0.2f + progress * 0.6f, Player.KEEPER1_ID);
////                        }
////                    };
//                    // Initialize tasks
////                    taskManager = new TaskManager(worldState, getPlayers());
////                    GameServerState.this.stateManager.attach(worldState);
//                    GameServerState.this.stateManager.attach(new SoundState(false));
//                    setProgress(0.60f, Player.KEEPER1_ID);
//
//                    // Trigger data
//                    for (short i = 0; i < LEVEL_FLAG_MAX_COUNT; i++) {
//                        flags.add(i, 0);
//                    }
//
//                    for (byte i = 0; i < LEVEL_TIMER_MAX_COUNT; i++) {
//                        timers.add(i, new GameTimer());
//                    }
//
//                    int triggerId = kwdFile.getGameLevel().getTriggerId();
//                    if (triggerId != 0) {
//                        triggerControl = new TriggerControl(stateManager, triggerId);
//                        setProgress(0.90f, Player.KEEPER1_ID);
//                    }
//
//                    // Game logic thread & movement
//                    exec = new PauseableScheduledThreadPoolExecutor(2, true);
//                    exec.setThreadFactory(new ThreadFactory() {
//
//                        @Override
//                        public Thread newThread(Runnable r) {
//                            return new Thread(r, "GameLogicAndMovementThread");
//                        }
//                    });
////                    gameLogicThread = new GameLogicThread(GameServerState.this.app,
////                            worldState, 1.0f / kwdFile.getGameLevel().getTicksPerSec(),
////                            GameServerState.this, new CreatureLogicState(worldState.getThingLoader()),
////                            new CreatureSpawnLogicState(worldState.getThingLoader(), getPlayers(), GameServerState.this),
////                            new RoomGoldFixer(worldState));
////                    exec.scheduleAtFixedRate(gameLogicThread,
////                            0, 1000 / kwdFile.getGameLevel().getTicksPerSec(), TimeUnit.MILLISECONDS);
////                    //                    exec.scheduleAtFixedRate(new MovementThread(GameServerState.this.app, MOVEMENT_UPDATE_TPF, worldState.getThingLoader()),
////                    0, (long) (MOVEMENT_UPDATE_TPF * 1000), TimeUnit.MILLISECONDS
////                    );
//
//                    setProgress(1.0f, Player.KEEPER1_ID);
//                } catch (Exception e) {
//                    logger.log(Level.SEVERE, "Failed to load the game!", e);
//                }
//
//                return null;
//            }
//
//            private void setupPlayers() {
//
//                // Setup players
//                boolean addMissingPlayers = players.isEmpty(); // Add all if none is given (campaign..)
//                for (Entry<Short, Player> entry : kwdFile.getPlayers().entrySet()) {
//                    Keeper keeper = null;
//                    if (players.containsKey(entry.getKey())) {
//                        keeper = players.get(entry.getKey());
//                        keeper.setPlayer(entry.getValue());
//                    } else if (addMissingPlayers || entry.getKey() < Player.KEEPER1_ID) {
//                        keeper = new Keeper(entry.getValue(), app);
//                        players.put(entry.getKey(), keeper);
//                    }
//
//                    // Init
//                    if (keeper != null) {
//                        keeper.initialize(stateManager, app);
//
//                        // Spells are all available for research unless otherwise stated
//                        for (KeeperSpell spell : kwdFile.getKeeperSpells()) {
//                            if (spell.getBonusRTime() != 0) {
//                                keeper.getSpellControl().setTypeAvailable(spell, true);
//                            }
//                        }
//                    }
//                }
//
//                // Set player availabilities
//                // TODO: the player customized game settings
////                for (Variable.Availability availability : kwdFile.getAvailabilities()) {
////                    if (availability.getPlayerId() == 0 && availability.getType() != Variable.Availability.AvailabilityType.SPELL) {
////
////                        // All players
////                        for (Keeper player : getPlayers()) {
////                            setAvailability(player, availability);
////                        }
////                    } else {
////                        Keeper player = getPlayer((short) availability.getPlayerId());
////
////                        // Not all the players are participating...
////                        if (player != null) {
////                            setAvailability(player, availability);
////                        }
////                    }
////                }
//            }
//
//            private void setAvailability(Keeper player, Variable.Availability availability) {
//                switch (availability.getType()) {
//                    case CREATURE: {
//                        player.getCreatureControl().setTypeAvailable(kwdFile.getCreature((short) availability.getTypeId()), availability.getValue() == Variable.Availability.AvailabilityValue.ENABLE);
//                        break;
//                    }
//                    case ROOM: {
//                        player.getRoomControl().setTypeAvailable(kwdFile.getRoomById((short) availability.getTypeId()), availability.getValue() == Variable.Availability.AvailabilityValue.ENABLE);
//                        break;
//                    }
//                    case SPELL: {
//                        if (availability.getValue() == Variable.Availability.AvailabilityValue.ENABLE) {
//
//                            // Enable the spell, no need to research it
//                            player.getSpellControl().setSpellDiscovered(kwdFile.getKeeperSpellById(availability.getTypeId()), true);
//                        } else {
//                            player.getSpellControl().setTypeAvailable(kwdFile.getKeeperSpellById(availability.getTypeId()), false);
//                        }
//                    }
//                }
//            }
//
//            @Override
//            public void onLoadComplete() {
//
//                // Prewarm the whole scene
//                GameServerState.this.app.getRenderManager().preloadScene(rootNode);
//
//                // Enable player state
//                GameServerState.this.stateManager.getState(PlayerState.class).setEnabled(true);
//                GameServerState.this.stateManager.getState(SoundState.class).setEnabled(true);
//
//                // Set initialized
//                GameServerState.this.initialized = true;
//
//                // Set the processors
//                GameServerState.this.app.setViewProcessors();
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
//            }
//        };
        // stateManager.attach(loader);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        // Game logic thread
        if (enabled) {
            exec.resume();
        } else {
            exec.pause();
        }
        stateManager.getState(WorldState.class).setEnabled(enabled);
    }

    private void detachRelatedAppStates() {
        stateManager.detach(stateManager.getState(WorldState.class));
        stateManager.detach(stateManager.getState(SoundState.class));
    }

    /**
     * If you are getting rid of the game state, use this so that all the
     * related states are detached on the same render loop. Otherwise the app
     * might crash.
     */
    public void detach() {
        if (loader != null && loader.isAlive()) {
            loader.interrupt();
        }
        if (exec != null) {
            exec.shutdownNow();
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
        if (actionPointState != null) {
            actionPointState.updateControls(tpf);
        }
        timeTaken += tpf;
    }

    @Override
    public void processTick(float tpf, Application app) {

        // Update time for AI
        GdxAI.getTimepiece().update(tpf);

        if (timeLimit != null && timeLimit > 0) {
            timeLimit -= tpf;
        }

        for (GameTimer timer : timers) {
            timer.update(tpf);
        }

        if (triggerControl != null) {
            triggerControl.update(tpf);
        }

        if (partyTriggerState != null) {
            partyTriggerState.update(tpf);
        }

        if (creatureTriggerState != null) {
            creatureTriggerState.update(tpf);
        }

        if (objectTriggerState != null) {
            objectTriggerState.update(tpf);
        }
        if (doorTriggerState != null) {
            doorTriggerState.update(tpf);
        }
        if (actionPointState != null) {
            actionPointState.update(tpf);
        }

//        for (Keeper player : players.values()) {
//            player.update(tpf);
//        }
    }

    /**
     * Get the level raw data file
     *
     * @return the KWD
     */
    public KwdFile getLevelData() {
        return kwdFile;
    }

    public int getFlag(int id) {
        return flags.get(id);
    }

    public void setFlag(int id, int value) {
        flags.set(id, value);
    }

    public GameTimer getTimer(int id) {
        return timers.get(id);
    }

    /**
     * @see GameLogicThread#getGameTime()
     * @return the game time
     */
    public double getGameTime() {
        if (gameLogicThread != null) {
            return gameLogicThread.getGameTime();
        }
        return 0;
    }

    public Float getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(float timeLimit) {
        this.timeLimit = timeLimit;
    }

    @Override
    public boolean isPauseable() {
        return true;
    }

    /**
     * Load the game
     */
    private class GameLoader extends Thread {

        private final List<Keeper> players;

        public GameLoader(List<Keeper> players) {
            super("GameLoader");

            this.players = players;
        }

        @Override
        public void run() {

            // Make sure the KWD file is fully loaded
            kwdFile.load();

            // Create the central game controller
            gameController = new GameController(kwdFile, gameService.getEntityData(), kwdFile.getVariables());
            gameController.createNewGame(players);
            mapController = gameController.getMapController();
            gameController.addListener(plaerActionListener);

            // Send the the initial game data
            gameService.sendGameData(gameController.getPlayers(), mapController.getMapData());

            // Set up a listener for the map
            mapController.addListener(mapListener);

            // Set up a listener for the player changes, they are per player
            for (IPlayerController playerController : gameController.getPlayerControllers()) {
                playerController.addListener(gameService);
            }

            // Nullify the thread object
            loader = null;
        }
    }

    /**
     * Listen for basically clients' requests
     */
    private class GameSessionServiceListenerImpl implements GameSessionServiceListener {

        public GameSessionServiceListenerImpl() {
        }

        @Override
        public void onSelectTiles(Vector2f start, Vector2f end, boolean select, short playerId) {
            mapController.selectTiles(start, end, select, playerId);
        }

        @Override
        public void onBuild(Vector2f start, Vector2f end, short roomId, short playerId) {
            gameController.build(start, end, playerId, roomId);
        }

        @Override
        public void onSell(Vector2f start, Vector2f end, short playerId) {
            gameController.sell(start, end, playerId);
        }
    }

    /**
     * Listen for the map changes
     */
    private class MapListenerImpl implements MapListener {

        @Override
        public void onTilesChange(List<MapTile> updatedTiles) {
            gameService.updateTiles(updatedTiles);
        }
    }

    /**
     * Listen for the map changes
     */
    private class PlayerActionListenerImpl implements PlayerActionListener {

        @Override
        public void onBuild(short keeperId, List<MapTile> tiles) {
            gameService.onBuild(keeperId, tiles);
        }

        @Override
        public void onSold(short keeperId, List<MapTile> tiles) {
            gameService.onSold(keeperId, tiles);
        }

    }

}
