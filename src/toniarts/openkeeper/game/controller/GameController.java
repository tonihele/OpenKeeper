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
package toniarts.openkeeper.game.controller;

import com.badlogic.gdx.ai.GdxAI;
import com.simsilica.es.EntityData;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.action.ActionPointState;
import toniarts.openkeeper.game.data.GameResult;
import toniarts.openkeeper.game.data.GameTimer;
import toniarts.openkeeper.game.data.GeneralLevel;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.logic.CreatureAiSystem;
import toniarts.openkeeper.game.logic.CreatureFallSystem;
import toniarts.openkeeper.game.logic.CreatureViewSystem;
import toniarts.openkeeper.game.logic.GameLogicManager;
import toniarts.openkeeper.game.logic.IGameLogicUpdatable;
import toniarts.openkeeper.game.logic.ManaCalculatorLogic;
import toniarts.openkeeper.game.logic.MovementSystem;
import toniarts.openkeeper.game.logic.PositionSystem;
import toniarts.openkeeper.game.task.ITaskManager;
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
import toniarts.openkeeper.utils.GameLoop;
import toniarts.openkeeper.utils.PathUtils;

/**
 * The game controller, runs the game simulation itself
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class GameController implements IGameLogicUpdatable, AutoCloseable, IGameTimer {

    public static final int LEVEL_TIMER_MAX_COUNT = 16;
    private static final int LEVEL_FLAG_MAX_COUNT = 128;
    private static final float MOVEMENT_UPDATE_TPF = 0.02f;

    private String level;
    private KwdFile kwdFile;
    private toniarts.openkeeper.game.data.Level levelObject;

    private final SortedMap<Short, Keeper> players = new TreeMap<>();
    private final Map<Short, IPlayerController> playerControllers = new HashMap<>();
    private final Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;
    private final EntityData entityData;

    private GameLoop gameLogicLoop;
    private GameLoop steeringCalculatorLoop;
    private GameLoop gameAnimationLoop;
    private GameLogicManager gameAnimationThread;
    private GameLogicManager gameLogicThread;
    private TriggerControl triggerControl = null;
    private CreatureTriggerState creatureTriggerState;
    private ObjectTriggerState objectTriggerState;
    private DoorTriggerState doorTriggerState;
    private PartyTriggerState partyTriggerState;
    private ActionPointState actionPointState;
    private final List<Integer> flags = new ArrayList<>(LEVEL_FLAG_MAX_COUNT);
    private final List<GameTimer> timers = new ArrayList<>(LEVEL_TIMER_MAX_COUNT);
    private int levelScore = 0;
    private boolean campaign;
    private IMapController mapController;
    private GameWorldController gameWorldController;

    private GameResult gameResult = null;
    private float timeTaken = 0;
    private Float timeLimit = null;
    private ITaskManager taskManager;

    private static final Logger logger = Logger.getLogger(GameController.class.getName());

    /**
     * Single use game states
     *
     * @param level the level to load
     */
    public GameController(String level, EntityData entityData, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings) {
        this.level = level;
        this.levelObject = null;
        this.entityData = entityData;
        this.gameSettings = gameSettings;
    }

    /**
     * Single use game states
     *
     * @param level the level to load
     * @param players player participating in this game, can be {@code null}
     * @param entityData
     * @param gameSettings
     */
    public GameController(KwdFile level, List<Keeper> players, EntityData entityData, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings) {
        this.level = null;
        this.kwdFile = level;
        this.levelObject = null;
        this.entityData = entityData;
        this.gameSettings = gameSettings;
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
     * @param entityData
     * @param gameSettings
     */
    public GameController(GeneralLevel selectedLevel, EntityData entityData, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings) {
        this.level = null;
        this.kwdFile = selectedLevel.getKwdFile();
        this.entityData = entityData;
        this.gameSettings = gameSettings;
        if (selectedLevel instanceof toniarts.openkeeper.game.data.Level) {
            this.levelObject = (toniarts.openkeeper.game.data.Level) selectedLevel;
        } else {
            this.levelObject = null;
        }
    }

    public void createNewGame() {

        // Load the level data
        try {
            if (level != null) {

                kwdFile = new KwdFile(Main.getDkIIFolder(),
                        new File(ConversionUtils.getRealFileName(Main.getDkIIFolder(), PathUtils.DKII_MAPS_FOLDER + level + ".kwd")));

            } else {
                kwdFile.load();
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Failed to load the map file!", ex);
            throw new RuntimeException(level, ex);
        }

        // The players
        setupPlayers();

        // The world
        gameWorldController = new GameWorldController(kwdFile, entityData, gameSettings, playerControllers.values(), this);
        gameWorldController.createNewGame();

        // The triggers
        partyTriggerState = new PartyTriggerState(true);
        //partyTriggerState.initialize(stateManager, app);
        creatureTriggerState = new CreatureTriggerState(true);
        //creatureTriggerState.initialize(stateManager, app);
        objectTriggerState = new ObjectTriggerState(true);
        //objectTriggerState.initialize(stateManager, app);
        doorTriggerState = new DoorTriggerState(true);
        //doorTriggerState.initialize(stateManager, app);
        actionPointState = new ActionPointState(true);
        //actionPointState.initialize(stateManager, app);

        // Initialize tasks
        taskManager = new TaskManager(gameWorldController, gameWorldController.getMapController(), playerControllers.values());

        // Trigger data
        for (short i = 0; i < LEVEL_FLAG_MAX_COUNT; i++) {
            flags.add(i, 0);
        }

        for (byte i = 0; i < LEVEL_TIMER_MAX_COUNT; i++) {
            timers.add(i, new GameTimer());
        }

        int triggerId = kwdFile.getGameLevel().getTriggerId();
        if (triggerId != 0) {
            //triggerControl = new TriggerControl(stateManager, triggerId);
        }

        // Create the game loops ready to start
        // Game logic
        gameLogicThread = new GameLogicManager(new PositionSystem(gameWorldController.getMapController(), entityData),
                new ManaCalculatorLogic(gameSettings, playerControllers.values(), gameWorldController.getMapController()),
                new CreatureAiSystem(entityData, gameWorldController, taskManager, kwdFile),
                new CreatureViewSystem(entityData));
        gameLogicLoop = new GameLoop(gameLogicThread, 1000000000 / kwdFile.getGameLevel().getTicksPerSec(), "GameLogic");

        // Animation systems
        gameAnimationThread = new GameLogicManager(new CreatureFallSystem(entityData));
        gameAnimationLoop = new GameLoop(gameAnimationThread, GameLoop.INTERVAL_FPS_60, "GameAnimation");

        // Steering
        steeringCalculatorLoop = new GameLoop(new GameLogicManager(new MovementSystem(entityData)), GameLoop.INTERVAL_FPS_60, "SteeringCalculator");
    }

    public void startGame() {

        // Game logic thread & movement
        gameLogicLoop.start();
        gameAnimationLoop.start();
        steeringCalculatorLoop.start();
    }

    private void setupPlayers() {

        // Setup players
        boolean addMissingPlayers = players.isEmpty(); // Add all if none is given (campaign...)
        for (Map.Entry<Short, Player> entry : kwdFile.getPlayers().entrySet()) {
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
                PlayerController playerController = new PlayerController(keeper, entityData, gameSettings);
                playerControllers.put(entry.getKey(), playerController);

                // Spells are all available for research unless otherwise stated
                for (KeeperSpell spell : kwdFile.getKeeperSpells()) {
                    if (spell.getBonusRTime() != 0) {
                        playerController.getSpellControl().setTypeAvailable(spell, true);
                    }
                }
            }
        }

        // Set player availabilities
        // TODO: the player customized game settings
        for (Variable.Availability availability : kwdFile.getAvailabilities()) {
            if (availability.getPlayerId() == 0 && availability.getType() != Variable.Availability.AvailabilityType.SPELL) {

                // All players
                for (Keeper player : players.values()) {
                    setAvailability(player, availability);
                }
            } else {
                Keeper player = players.get((short) availability.getPlayerId());

                // Not all the players are participating...
                if (player != null) {
                    setAvailability(player, availability);
                }
            }
        }
    }

    private void setAvailability(Keeper player, Variable.Availability availability) {
        IPlayerController playerController = playerControllers.get(player.getId());
        switch (availability.getType()) {
            case CREATURE: {
                playerController.getCreatureControl().setTypeAvailable(kwdFile.getCreature((short) availability.getTypeId()), availability.getValue() == Variable.Availability.AvailabilityValue.ENABLE);
                break;
            }
            case ROOM: {
                playerController.getRoomControl().setTypeAvailable(kwdFile.getRoomById((short) availability.getTypeId()), availability.getValue() == Variable.Availability.AvailabilityValue.ENABLE);
                break;
            }
            case SPELL: {
                if (availability.getValue() == Variable.Availability.AvailabilityValue.ENABLE) {

                    // Enable the spell, no need to research it
                    playerController.getSpellControl().setSpellDiscovered(kwdFile.getKeeperSpellById(availability.getTypeId()), true);
                } else {
                    playerController.getSpellControl().setTypeAvailable(kwdFile.getKeeperSpellById(availability.getTypeId()), false);
                }
            }
        }
    }

    public Collection<IPlayerController> getPlayerControllers() {
        return playerControllers.values();
    }

    public void pauseGame() {
        if (steeringCalculatorLoop != null) {
            steeringCalculatorLoop.pause();
        }
        if (gameAnimationLoop != null) {
            gameAnimationLoop.pause();
        }
        if (gameLogicLoop != null) {
            gameLogicLoop.pause();
        }
    }

    public void resumeGame() {
        if (gameLogicLoop != null) {
            gameLogicLoop.resume();
        }
        if (gameAnimationLoop != null) {
            gameAnimationLoop.resume();
        }
        if (steeringCalculatorLoop != null) {
            steeringCalculatorLoop.resume();
        }
    }

    @Override
    public void processTick(float tpf, double gameTime) {

        // Update time for AI
        GdxAI.getTimepiece().update(tpf);

        if (actionPointState != null) {
            actionPointState.updateControls(tpf);
        }
        timeTaken += tpf;

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
//        stateManager.getState(PlayerState.class).endGame(win);
        // Mark the achievement if campaign level
        if (levelObject != null) {
            Main.getUserSettings().increaseLevelAttempts(levelObject);
            if (win) {
                Main.getUserSettings().setLevelStatus(levelObject, Settings.LevelStatus.COMPLETED);
            }
            try {
                Main.getUserSettings().save();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Failed to save the level progress!", ex);
            }
        }
    }

    public ITaskManager getTaskManager() {
        return taskManager;
    }

    public Keeper getPlayer(short playerId) {
        return players.get(playerId);
    }

    public Collection<Keeper> getPlayers() {
        return players.values();
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

    @Nullable
    public GameResult getGameResult() {
        return gameResult;
    }

    @Override
    public void close() throws Exception {
        if (steeringCalculatorLoop != null) {
            steeringCalculatorLoop.stop();
            steeringCalculatorLoop = null;
        }
        if (gameAnimationLoop != null) {
            gameAnimationLoop.stop();
            gameAnimationLoop = null;
        }
        if (gameLogicLoop != null) {
            gameLogicLoop.stop();
            gameLogicLoop = null;
        }
    }

    public GameWorldController getGameWorldController() {
        return gameWorldController;
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
