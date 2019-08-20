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
import com.jme3.util.SafeArrayList;
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
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.game.data.GameResult;
import toniarts.openkeeper.game.data.GameTimer;
import toniarts.openkeeper.game.data.GeneralLevel;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.logic.ChickenAiSystem;
import toniarts.openkeeper.game.logic.ChickenSpawnSystem;
import toniarts.openkeeper.game.logic.CreatureAiSystem;
import toniarts.openkeeper.game.logic.CreatureExperienceSystem;
import toniarts.openkeeper.game.logic.CreatureFallSystem;
import toniarts.openkeeper.game.logic.CreatureImprisonSystem;
import toniarts.openkeeper.game.logic.CreatureRecuperatingSystem;
import toniarts.openkeeper.game.logic.CreatureSpawnSystem;
import toniarts.openkeeper.game.logic.CreatureTorturingSystem;
import toniarts.openkeeper.game.logic.CreatureViewSystem;
import toniarts.openkeeper.game.logic.DeathSystem;
import toniarts.openkeeper.game.logic.DecaySystem;
import toniarts.openkeeper.game.logic.DoorViewSystem;
import toniarts.openkeeper.game.logic.DungeonHeartConstruction;
import toniarts.openkeeper.game.logic.GameLogicManager;
import toniarts.openkeeper.game.logic.HaulingSystem;
import toniarts.openkeeper.game.logic.HealthSystem;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.logic.IGameLogicUpdatable;
import toniarts.openkeeper.game.logic.LooseGoldSystem;
import toniarts.openkeeper.game.logic.ManaCalculatorLogic;
import toniarts.openkeeper.game.logic.MovementSystem;
import toniarts.openkeeper.game.logic.PlayerCreatureSystem;
import toniarts.openkeeper.game.logic.PlayerSpellSystem;
import toniarts.openkeeper.game.logic.PositionSystem;
import toniarts.openkeeper.game.logic.SlapSystem;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.navigation.NavigationService;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.game.task.ITaskManager;
import toniarts.openkeeper.game.task.TaskManager;
import toniarts.openkeeper.game.trigger.TriggerControl;
import toniarts.openkeeper.game.trigger.actionpoint.ActionPointTriggerLogicController;
import toniarts.openkeeper.game.trigger.creature.CreatureTriggerLogicController;
import toniarts.openkeeper.game.trigger.door.DoorTriggerLogicController;
import toniarts.openkeeper.game.trigger.object.ObjectTriggerLogicController;
import toniarts.openkeeper.game.trigger.party.PartyTriggerLogicController;
import toniarts.openkeeper.game.trigger.player.PlayerTriggerLogicController;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.GameLoop;
import toniarts.openkeeper.utils.PathUtils;

/**
 * The game controller, runs the game simulation itself
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class GameController implements IGameLogicUpdatable, AutoCloseable, IGameTimer, ILevelInfo, IGameController {

    public static final int LEVEL_TIMER_MAX_COUNT = 16;
    private static final int LEVEL_FLAG_MAX_COUNT = 128;

    private String level;
    private KwdFile kwdFile;
    private toniarts.openkeeper.game.data.Level levelObject;

    private final SortedMap<Short, Keeper> players = new TreeMap<>();
    private final Map<Short, IPlayerController> playerControllers = new HashMap<>();
    private final Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;
    private final EntityData entityData;
    private final PlayerService playerService;

    private GameLoop gameLogicLoop;
    private GameLoop steeringCalculatorLoop;
    private GameLoop gameAnimationLoop;
    private GameLogicManager gameAnimationThread;
    private GameLogicManager gameLogicThread;
    private TriggerControl triggerControl = null;
    private CreatureTriggerLogicController creatureTriggerState;
    private ObjectTriggerLogicController objectTriggerState;
    private DoorTriggerLogicController doorTriggerState;
    private PartyTriggerLogicController partyTriggerState;
    private ActionPointTriggerLogicController actionPointController;
    private PlayerTriggerLogicController playerTriggerLogicController;
    private final List<Integer> flags = new ArrayList<>(LEVEL_FLAG_MAX_COUNT);
    private final SafeArrayList<GameTimer> timers = new SafeArrayList<>(GameTimer.class, LEVEL_TIMER_MAX_COUNT);
    private final Map<Integer, ActionPoint> actionPointsById = new HashMap<>();
    private final List<ActionPoint> actionPoints = new ArrayList<>();
    private int levelScore = 0;
    private boolean campaign;
    private GameWorldController gameWorldController;
    private INavigationService navigationService;
    private PositionSystem positionSystem;

    private GameResult gameResult = null;
    private Float timeLimit = null;
    private TaskManager taskManager;

    private static final Logger LOGGER = Logger.getLogger(GameController.class.getName());

    /**
     * Single use game states
     *
     * @param level the level to load
     * @param entityData
     * @param gameSettings
     * @param playerService
     */
    public GameController(String level, EntityData entityData, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings, PlayerService playerService) {
        this.level = level;
        this.levelObject = null;
        this.entityData = entityData;
        this.gameSettings = gameSettings;
        this.playerService = playerService;
    }

    /**
     * Single use game states
     *
     * @param level the level to load
     * @param players player participating in this game, can be {@code null}
     * @param entityData
     * @param gameSettings
     * @param playerService
     */
    public GameController(KwdFile level, List<Keeper> players, EntityData entityData, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings, PlayerService playerService) {
        this.level = null;
        this.kwdFile = level;
        this.levelObject = null;
        this.entityData = entityData;
        this.gameSettings = gameSettings;
        this.playerService = playerService;
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
     * @param playerService
     */
    public GameController(GeneralLevel selectedLevel, EntityData entityData, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings, PlayerService playerService) {
        this.level = null;
        this.kwdFile = selectedLevel.getKwdFile();
        this.entityData = entityData;
        this.gameSettings = gameSettings;
        this.playerService = playerService;
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
            LOGGER.log(Level.SEVERE, "Failed to load the map file!", ex);
            throw new RuntimeException(level, ex);
        }

        // The players
        setupPlayers();

        // Action points
        loadActionPoints();

        // The world
        gameWorldController = new GameWorldController(kwdFile, entityData, gameSettings, players, playerControllers, this);
        gameWorldController.createNewGame(this, this);

        positionSystem = new PositionSystem(gameWorldController.getMapController(), entityData, gameWorldController.getCreaturesController(), gameWorldController.getDoorsController(), gameWorldController.getObjectsController());

        // Navigation
        navigationService = new NavigationService(gameWorldController.getMapController(), positionSystem);

        // Initialize tasks
        taskManager = new TaskManager(entityData, gameWorldController, gameWorldController.getMapController(), gameWorldController.getCreaturesController(), navigationService, playerControllers.values(), this, positionSystem);

        // The triggers
        partyTriggerState = new PartyTriggerLogicController(this, this, this, gameWorldController.getMapController(), gameWorldController.getCreaturesController());
        creatureTriggerState = new CreatureTriggerLogicController(this, this, this, gameWorldController.getMapController(), gameWorldController.getCreaturesController(), playerService, entityData);
        objectTriggerState = new ObjectTriggerLogicController(this, this, this, gameWorldController.getMapController(), gameWorldController.getCreaturesController(), playerService, entityData, gameWorldController.getObjectsController());
        doorTriggerState = new DoorTriggerLogicController(this, this, this, gameWorldController.getMapController(), gameWorldController.getCreaturesController(), playerService, entityData, gameWorldController.getDoorsController());
        actionPointController = new ActionPointTriggerLogicController(this, this, this, gameWorldController.getMapController(), gameWorldController.getCreaturesController(), positionSystem);
        playerTriggerLogicController = new PlayerTriggerLogicController(this, this, this, gameWorldController.getMapController(), gameWorldController.getCreaturesController(), playerService);

        // Trigger data
        for (short i = 0; i < LEVEL_FLAG_MAX_COUNT; i++) {
            flags.add(i, 0);
        }

        for (byte i = 0; i < LEVEL_TIMER_MAX_COUNT; i++) {
            timers.add(i, new GameTimer());
        }

        int triggerId = kwdFile.getGameLevel().getTriggerId();
        if (triggerId != 0) {
            triggerControl = new TriggerControl(this, this, this, gameWorldController.getMapController(), gameWorldController.getCreaturesController(), triggerId);
        }

        // Create the game loops ready to start
        // Game logic
        gameLogicThread = new GameLogicManager(positionSystem,
                gameWorldController.getMapController(),
                new DecaySystem(entityData),
                new CreatureExperienceSystem(entityData, kwdFile, gameSettings, gameWorldController.getCreaturesController()),
                new SlapSystem(entityData, kwdFile, playerControllers.values(), gameSettings),
                new HealthSystem(entityData, kwdFile, positionSystem, gameSettings, gameWorldController.getCreaturesController()),
                new CreatureRecuperatingSystem(entityData, gameSettings),
                new CreatureImprisonSystem(entityData, gameSettings),
                new CreatureTorturingSystem(entityData, this, gameSettings),
                new DeathSystem(entityData, gameSettings, positionSystem),
                new PlayerCreatureSystem(entityData, kwdFile, playerControllers.values()),
                new PlayerSpellSystem(entityData, kwdFile, playerControllers.values()),
                this,
                new CreatureSpawnSystem(gameWorldController.getCreaturesController(), playerControllers.values(), gameSettings, this, gameWorldController.getMapController()),
                new ChickenSpawnSystem(gameWorldController.getObjectsController(), playerControllers.values(), gameSettings, this, gameWorldController.getMapController()),
                new ManaCalculatorLogic(gameSettings, playerControllers.values(), gameWorldController.getMapController()),
                new CreatureAiSystem(entityData, gameWorldController.getCreaturesController()),
                new ChickenAiSystem(entityData, gameWorldController.getObjectsController()),
                new CreatureViewSystem(entityData),
                new DoorViewSystem(entityData, positionSystem),
                new LooseGoldSystem(entityData, gameWorldController.getMapController(), playerControllers, positionSystem),
                new HaulingSystem(entityData),
                taskManager);
        gameLogicLoop = new GameLoop(gameLogicThread, 1000000000 / kwdFile.getGameLevel().getTicksPerSec(), "GameLogic");

        // Animation systems
        gameAnimationThread = new GameLogicManager(new DungeonHeartConstruction(entityData, getLevelVariable(Variable.MiscVariable.MiscType.TIME_BEFORE_DUNGEON_HEART_CONSTRUCTION_BEGINS)), new CreatureFallSystem(entityData));
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
                PlayerController playerController = new PlayerController(kwdFile, keeper, kwdFile.getImp(), entityData, gameSettings);
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

    private void loadActionPoints() {
        for (Thing.ActionPoint thing : getLevelData().getThings(Thing.ActionPoint.class)) {
            ActionPoint ap = new ActionPoint(thing);
            actionPointsById.put(ap.getId(), ap);
            actionPoints.add(ap);
        }
    }

    @Override
    public IPlayerController getPlayerController(short playerId) {
        return playerControllers.get(playerId);
    }

    @Override
    public Collection<IPlayerController> getPlayerControllers() {
        return playerControllers.values();
    }

    @Override
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
        playerService.setGamePaused(true);
    }

    @Override
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
        playerService.setGamePaused(false);
    }

    @Override
    public void processTick(float tpf, double gameTime) {

        // Update time for AI
        GdxAI.getTimepiece().update(tpf);

        // Time limit is a special timer, it just ticks towards 0 if it is set
        if (timeLimit != null) {
            timeLimit -= tpf;

            // We rest on zero
            if (timeLimit < 0) {
                timeLimit = 0f;
            }
        }

        // Advance game timers
        for (GameTimer timer : timers.getArray()) {
            timer.update(tpf);
        }

        if (triggerControl != null) {
            triggerControl.update(tpf);
        }

        if (partyTriggerState != null) {
            partyTriggerState.processTick(tpf, gameTime);
        }

        if (creatureTriggerState != null) {
            creatureTriggerState.processTick(tpf, gameTime);
        }

        if (objectTriggerState != null) {
            objectTriggerState.processTick(tpf, gameTime);
        }

        if (doorTriggerState != null) {
            doorTriggerState.processTick(tpf, gameTime);
        }

        if (actionPointController != null) {
            actionPointController.processTick(tpf, gameTime);
        }

        if (playerTriggerLogicController != null) {
            playerTriggerLogicController.processTick(tpf, gameTime);
        }

        if (timeLimit != null && timeLimit <= 0) {
            //TODO:
            throw new RuntimeException("Level time limit exceeded!");
        }
    }

    /**
     * Get the level raw data file
     *
     * @return the KWD
     */
    @Override
    public KwdFile getLevelData() {
        return kwdFile;
    }

    @Override
    public int getFlag(int id) {
        return flags.get(id);
    }

    @Override
    public void setFlag(int id, int value) {
        flags.set(id, value);
    }

    @Override
    public GameTimer getTimer(int id) {
        return timers.get(id);
    }

    /**
     * @see GameLogicManager#getGameTime()
     * @return the game time
     */
    @Override
    public double getGameTime() {
        if (gameLogicThread != null) {
            return gameLogicThread.getGameTime();
        }
        return 0;
    }

    @Override
    public Float getTimeLimit() {
        return timeLimit;
    }

    @Override
    public void setTimeLimit(float timeLimit) {
        this.timeLimit = timeLimit;
    }

    @Override
    public void endGame(short playerId, boolean win) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void setEnd(boolean win) {

        // TODO: this is client stuff, we should only determine here what happens to the game & players
        gameResult = new GameResult();
        gameResult.setData(GameResult.ResultType.LEVEL_WON, win);
        gameResult.setData(GameResult.ResultType.TIME_TAKEN, gameLogicThread.getGameTime());

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
                LOGGER.log(Level.SEVERE, "Failed to save the level progress!", ex);
            }
        }
    }

    @Override
    public ITaskManager getTaskManager() {
        return taskManager;
    }

    @Override
    public Keeper getPlayer(short playerId) {
        return players.get(playerId);
    }

    @Override
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

    /**
     * Get level score, not really a player score... kinda
     *
     * @return the level score
     */
    @Override
    public int getLevelScore() {
        return levelScore;
    }

    @Override
    public void setLevelScore(int levelScore) {
        this.levelScore = levelScore;
    }

    public CreatureTriggerLogicController getCreatureTriggerState() {
        return creatureTriggerState;
    }

    public ObjectTriggerLogicController getObjectTriggerState() {
        return objectTriggerState;
    }

    public DoorTriggerLogicController getDoorTriggerState() {
        return doorTriggerState;
    }

    public PartyTriggerLogicController getPartyTriggerState() {
        return partyTriggerState;
    }

    /**
     * Creates alliance between two players
     *
     * @param playerOneId player ID 1
     * @param playerTwoId player ID 2
     */
    @Override
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
    @Override
    public void breakAlliance(short playerOneId, short playerTwoId) {
        getPlayer(playerOneId).breakAlliance(playerTwoId);
        getPlayer(playerTwoId).breakAlliance(playerOneId);
    }

    @Override
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

    @Override
    public IGameWorldController getGameWorldController() {
        return gameWorldController;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public ActionPoint getActionPoint(int id) {
        return actionPointsById.get(id);
    }

    @Override
    public List<ActionPoint> getActionPoints() {
        return actionPoints;
    }

    @Override
    public INavigationService getNavigationService() {
        return navigationService;
    }

    @Override
    public IEntityPositionLookup getEntityLookupService() {
        return positionSystem;
    }

}
