/*
 * Copyright (C) 2014-2015 OpenKeeper
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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.action.ActionPointState;
import toniarts.openkeeper.game.data.GameResult;
import toniarts.openkeeper.game.data.GameTimer;
import toniarts.openkeeper.game.data.GeneralLevel;
import toniarts.openkeeper.game.data.ISoundable;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.logic.GameLogicManager;
import toniarts.openkeeper.game.logic.IGameLogicUpdatable;
import toniarts.openkeeper.game.logic.MovementThread;
import toniarts.openkeeper.game.state.loading.MultiplayerLoadingState;
import toniarts.openkeeper.game.task.TaskManager;
import toniarts.openkeeper.game.trigger.TriggerControl;
import toniarts.openkeeper.game.trigger.creature.CreatureTriggerState;
import toniarts.openkeeper.game.trigger.door.DoorTriggerState;
import toniarts.openkeeper.game.trigger.object.ObjectTriggerState;
import toniarts.openkeeper.game.trigger.party.PartyTriggerState;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.tools.modelviewer.SoundsLoader;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.PathUtils;
import toniarts.openkeeper.utils.PauseableScheduledThreadPoolExecutor;
import toniarts.openkeeper.world.WorldState;

/**
 * The GAME state!
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class GameState extends AbstractPauseAwareState implements IGameLogicUpdatable {

    public static final int LEVEL_TIMER_MAX_COUNT = 16;
    private static final int LEVEL_FLAG_MAX_COUNT = 128;
    private static final float MOVEMENT_UPDATE_TPF = 0.02f;

    private Main app;

    private AppStateManager stateManager;

    private final String level;
    private KwdFile kwdFile;
    private final toniarts.openkeeper.game.data.Level levelObject;

    private GameLogicManager gameLogicThread;
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

    private GameResult gameResult = null;
    private float timeTaken = 0;
    private Float timeLimit = null;
    private TaskManager taskManager;
    private final Map<Short, Keeper> players = new TreeMap<>();
    private PauseableScheduledThreadPoolExecutor exec;

    private static final Logger logger = Logger.getLogger(GameState.class.getName());

    /**
     * Single use game states
     *
     * @param level the level to load
     */
    public GameState(String level) {
        this.level = level;
        this.levelObject = null;
    }

    /**
     * Single use game states
     *
     * @param level the level to load
     * @param players player participating in this game, can be {@code null}
     */
    public GameState(KwdFile level, List<Keeper> players) {
        this.level = null;
        this.kwdFile = level;
        this.levelObject = null;
        if (players != null) {
            for (Keeper keeper : players) {
                this.players.put(keeper.getId(), keeper);
            }
        }
    }

    /**
     * Single use game states
     *
     * @param selectedLevel the level to load
     */
    public GameState(GeneralLevel selectedLevel) {
        this.level = null;
        this.kwdFile = selectedLevel.getKwdFile();
        if (selectedLevel instanceof toniarts.openkeeper.game.data.Level) {
            this.levelObject = (toniarts.openkeeper.game.data.Level) selectedLevel;
        } else {
            this.levelObject = null;
        }
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

                    // Load the level data
                    if (level != null) {
                        kwdFile = new KwdFile(Main.getDkIIFolder(),
                                new File(ConversionUtils.getRealFileName(Main.getDkIIFolder(), PathUtils.DKII_MAPS_FOLDER + level + ".kwd")));
                    } else {
                        kwdFile.load();
                    }
                    AssetUtils.prewarmAssets(kwdFile, assetManager, app);
                    setProgress(0.1f, Player.KEEPER1_ID);

                    // load sounds
                    loadSounds();

                    // The players
                    setupPlayers();

                    // Triggers
                    partyTriggerState = new PartyTriggerState(true);
                    partyTriggerState.initialize(stateManager, app);
                    creatureTriggerState = new CreatureTriggerState(true);
                    creatureTriggerState.initialize(stateManager, app);
                    objectTriggerState = new ObjectTriggerState(true);
                    objectTriggerState.initialize(stateManager, app);
                    doorTriggerState = new DoorTriggerState(true);
                    doorTriggerState.initialize(stateManager, app);
                    actionPointState = new ActionPointState(true);
                    actionPointState.initialize(stateManager, app);
                    setProgress(0.20f, Player.KEEPER1_ID);
                    setProgress(0.20f, Player.KEEPER2_ID);
                    setProgress(0.20f, Player.KEEPER3_ID);
                    setProgress(0.20f, Player.KEEPER4_ID);

                    // Create the actual level
                    WorldState worldState = new WorldState(kwdFile, assetManager, GameState.this) {
                        @Override
                        protected void updateProgress(float progress) {
                            setProgress(0.2f + progress * 0.6f, Player.KEEPER1_ID);
                        }
                    };

                    // Initialize tasks
                    taskManager = new TaskManager(worldState, getPlayers());

                    GameState.this.stateManager.attach(worldState);

                    GameState.this.stateManager.attach(new SoundState(false));
                    setProgress(0.60f, Player.KEEPER1_ID);

                    // Trigger data
                    for (short i = 0; i < LEVEL_FLAG_MAX_COUNT; i++) {
                        flags.add(i, 0);
                    }

                    for (byte i = 0; i < LEVEL_TIMER_MAX_COUNT; i++) {
                        timers.add(i, new GameTimer());
                    }

                    int triggerId = kwdFile.getGameLevel().getTriggerId();
                    if (triggerId != 0) {
                        triggerControl = new TriggerControl(stateManager, triggerId);
                        setProgress(0.90f, Player.KEEPER1_ID);
                    }

                    // Game logic thread & movement
                    exec = new PauseableScheduledThreadPoolExecutor(2, true);
                    exec.setThreadFactory(new ThreadFactory() {

                        @Override
                        public Thread newThread(Runnable r) {
                            return new Thread(r, "GameLogicAndMovementThread");
                        }
                    });
//                    gameLogicThread = new GameLogicThread(GameState.this.app,
//                            worldState, 1.0f / kwdFile.getGameLevel().getTicksPerSec(),
//                            GameState.this, new CreatureLogicState(worldState.getThingLoader()),
//                            new CreatureSpawnLogicState(worldState.getThingLoader(), getPlayers(), GameState.this),
//                            new RoomGoldFixer(worldState));
//                    exec.scheduleAtFixedRate(gameLogicThread,
//                            0, 1000 / kwdFile.getGameLevel().getTicksPerSec(), TimeUnit.MILLISECONDS);
                    exec.scheduleAtFixedRate(new MovementThread(GameState.this.app, MOVEMENT_UPDATE_TPF, worldState.getThingLoader()),
                            0, (long) (MOVEMENT_UPDATE_TPF * 1000), TimeUnit.MILLISECONDS);

                    setProgress(1.0f, Player.KEEPER1_ID);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to load the game!", e);
                }

                return null;
            }

            private void setupPlayers() {

                // Setup players
                boolean addMissingPlayers = players.isEmpty(); // Add all if none is given (campaign..)
                for (Entry<Short, Player> entry : kwdFile.getPlayers().entrySet()) {
                    Keeper keeper = null;
                    if (players.containsKey(entry.getKey())) {
                        keeper = players.get(entry.getKey());
                        keeper.setPlayer(entry.getValue());
                    } else if (addMissingPlayers || entry.getKey() < Player.KEEPER1_ID) {
                        keeper = new Keeper(entry.getValue());
                        players.put(entry.getKey(), keeper);
                    }

                    // Init
                    if (keeper != null) {
                        keeper.initialize(stateManager, app);

                        // Spells are all available for research unless otherwise stated
                        for (KeeperSpell spell : kwdFile.getKeeperSpells()) {
                            if (spell.getBonusRTime() != 0) {
//                                keeper.getSpellControl().setTypeAvailable(spell, true);
                            }
                        }
                    }
                }

                // Set player availabilities
                // TODO: the player customized game settings
                for (Variable.Availability availability : kwdFile.getAvailabilities()) {
                    if (availability.getPlayerId() == 0 && availability.getType() != Variable.Availability.AvailabilityType.SPELL) {

                        // All players
                        for (Keeper player : getPlayers()) {
                            setAvailability(player, availability);
                        }
                    } else {
                        Keeper player = getPlayer((short) availability.getPlayerId());

                        // Not all the players are participating...
                        if (player != null) {
                            setAvailability(player, availability);
                        }
                    }
                }
            }

            private void setAvailability(Keeper player, Variable.Availability availability) {
                switch (availability.getType()) {
                    case CREATURE: {
//                        player.getCreatureControl().setTypeAvailable(kwdFile.getCreature((short) availability.getTypeId()), availability.getValue() == Variable.Availability.AvailabilityValue.ENABLE);
                        break;
                    }
                    case ROOM: {
//                        player.getRoomControl().setTypeAvailable(kwdFile.getRoomById((short) availability.getTypeId()), availability.getValue() == Variable.Availability.AvailabilityValue.ENABLE);
                        break;
                    }
                    case SPELL: {
                        if (availability.getValue() == Variable.Availability.AvailabilityValue.ENABLE) {

                            // Enable the spell, no need to research it
//                            player.getSpellControl().setSpellDiscovered(kwdFile.getKeeperSpellById(availability.getTypeId()), true);
                        } else {
//                            player.getSpellControl().setTypeAvailable(kwdFile.getKeeperSpellById(availability.getTypeId()), false);
                        }
                    }
                }
            }

            @Override
            public void onLoadComplete() {

                // Prewarm the whole scene
                GameState.this.app.getRenderManager().preloadScene(rootNode);

                // Enable player state
                GameState.this.stateManager.getState(PlayerState.class).setEnabled(true);
                GameState.this.stateManager.getState(SoundState.class).setEnabled(true);

                // Set initialized
                GameState.this.initialized = true;

                // Set the processors
                GameState.this.app.setViewProcessors();

                // FIXME: this is not correct
                // Enqueue the thread starting to next frame so that the states are initialized
                app.enqueue(() -> {

                    // Enable game logic thread
                    exec.resume();

                    return null;
                });
            }
        };
        stateManager.attach(loader);
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
    public void processTick(float tpf, double gameTime) {

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

        for (Keeper player : players.values()) {
//            player.update(tpf);
        }
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
     * @see GameLogicManager#getGameTime()
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

    public void setEnd(boolean win) {

        gameResult = new GameResult();
        gameResult.setData(GameResult.ResultType.LEVEL_WON, win);
        gameResult.setData(GameResult.ResultType.TIME_TAKEN, timeTaken);

        // Enable the end game state
        stateManager.getState(PlayerState.class).endGame(win);

        // Mark the achievement if campaign level
        if (levelObject != null) {
            Main.getUserSettings().increaseLevelAttempts(levelObject);
            if (win) {
                Main.getUserSettings().setLevelStatus(levelObject, Settings.LevelStatus.COMPLETED);
            }
            try {
                Main.getUserSettings().save();
            } catch (IOException ex) {
                Logger.getLogger(GameState.class.getName()).log(Level.SEVERE, "Failed to save the level progress!", ex);
            }
        }
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public Keeper getPlayer(short playerId) {
        return players.get(playerId);
    }

    public Collection<Keeper> getPlayers() {
        return players.values();
    }

    @Override
    public boolean isPauseable() {
        return true;
    }

    public ActionPointState getActionPointState() {
        return actionPointState;
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

    public Application getApplication() {
        return app;
    }

    /**
     * Get level score, not really a player score... kinda
     *
     * @return the level score
     */
    public int getLevelScore() {
        return levelScore;
    }

    public void setLevelScore(int levelScore) {
        this.levelScore = levelScore;
    }

    public CreatureTriggerState getCreatureTriggerState() {
        return creatureTriggerState;
    }

    public ObjectTriggerState getObjectTriggerState() {
        return objectTriggerState;
    }

    public DoorTriggerState getDoorTriggerState() {
        return doorTriggerState;
    }

    public PartyTriggerState getPartyTriggerState() {
        return partyTriggerState;
    }

    /**
     * Creates alliance between two players
     *
     * @param playerOneId player ID 1
     * @param playerTwoId player ID 2
     */
    public void createAlliance(short playerOneId, short playerTwoId) {
        getPlayer(playerOneId).createAlliance(playerTwoId);
        getPlayer(playerTwoId).createAlliance(playerOneId);
    }

    /**
     * Breaks alliance between two players
     *
     * @param playerOneId player ID 1
     * @param playerTwoId player ID 2
     */
    public void breakAlliance(short playerOneId, short playerTwoId) {
        getPlayer(playerOneId).breakAlliance(playerTwoId);
        getPlayer(playerTwoId).breakAlliance(playerOneId);
    }

    private void loadSounds() {
        SoundsLoader.load(kwdFile.getGameLevel().getSoundCategory(), false);

        List<ISoundable> items = new ArrayList<>();
        items.addAll(kwdFile.getCreatureList());
        items.addAll(kwdFile.getDoors());
        items.addAll(kwdFile.getObjectList());
        items.addAll(kwdFile.getKeeperSpells());
        items.addAll(kwdFile.getRooms());
        items.addAll(kwdFile.getShots());
        items.addAll(kwdFile.getTerrainList());
        items.addAll(kwdFile.getTraps());

        for (ISoundable item : items) {
            // all in global space
            SoundsLoader.load(item.getSoundCategory());
        }
    }

    @Nullable
    public GameResult getGameResult() {
        return gameResult;
    }

    @Override
    public void start() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void stop() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
