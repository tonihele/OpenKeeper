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
import com.simsilica.es.EntityId;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.controller.player.PlayerResearchControl;
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.game.data.GameResult;
import toniarts.openkeeper.game.data.GameTimer;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.logic.*;
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
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Variable;

/**
 * The game controller, runs the game simulation itself
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class GameController implements IGameLogicUpdatable, IGameController {

    private static final Logger logger = System.getLogger(GameController.class.getName());

    private final LevelInfo levelInfo;
    private final toniarts.openkeeper.game.data.Level levelObject;

    private final Map<Short, IPlayerController> playerControllers = HashMap.newHashMap(6);
    private final Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;
    private final EntityData entityData;
    private final PlayerService playerService;

    private TriggerControl triggerControl = null;
    private final List<IGameLogicUpdatable> controllers = new ArrayList<>();

    private GameWorldController gameWorldController;
    private INavigationService navigationService;
    private PositionSystem positionSystem;

    private GameResult gameResult = null;
    private TaskManager taskManager;

    /**
     * Single use game states
     *
     * @param level the level to load
     * @param players player participating in this game, can be {@code null}
     * @param entityData
     * @param gameSettings
     * @param playerService
     */
    public GameController(IKwdFile level, List<Keeper> players, EntityData entityData,
            Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings,
            PlayerService playerService) {

        levelInfo = new LevelInfo(level);
        levelObject = null;
        this.entityData = entityData;
        this.gameSettings = gameSettings;
        this.playerService = playerService;
        if (players != null) {
            for (Keeper keeper : players) {
                levelInfo.players.put(keeper.getId(), keeper);
            }
        }
        createNewGame();
    }

    public void createNewGame() {
        levelInfo.load();
        // The players
        setupPlayers();

        final GameTimeController gameTimer = new GameTimeController();
        // The world
        gameWorldController = new GameWorldController(this, levelInfo, entityData, gameSettings, playerControllers, gameTimer);

        positionSystem = new PositionSystem(gameWorldController.getMapController(), entityData,
                gameWorldController.getCreaturesController(), gameWorldController.getDoorsController(),
                gameWorldController.getObjectsController());
        gameWorldController.setEntityPositionLookup(positionSystem);

        // Navigation
        navigationService = new NavigationService(gameWorldController.getMapController(), positionSystem);

        // Initialize tasks
        taskManager = new TaskManager(entityData, gameWorldController, gameWorldController.getMapController(),
                gameWorldController.getObjectsController(), gameWorldController.getCreaturesController(),
                navigationService, playerControllers.values(), levelInfo, positionSystem, gameSettings);

        // The triggers
        controllers.add(new PartyTriggerLogicController(this, levelInfo, gameTimer,
                gameWorldController.getMapController(), gameWorldController.getCreaturesController()));
        controllers.add(new CreatureTriggerLogicController(this, levelInfo, gameTimer,
                gameWorldController.getMapController(), gameWorldController.getCreaturesController(),
                playerService, entityData));
        controllers.add(new ObjectTriggerLogicController(this, levelInfo, gameTimer,
                gameWorldController.getMapController(), gameWorldController.getCreaturesController(),
                playerService, entityData, gameWorldController.getObjectsController()));
        controllers.add(new DoorTriggerLogicController(this, levelInfo, gameTimer,
                gameWorldController.getMapController(), gameWorldController.getCreaturesController(),
                playerService, entityData, gameWorldController.getDoorsController()));
        controllers.add(new ActionPointTriggerLogicController(this, levelInfo, gameTimer,
                gameWorldController.getMapController(), gameWorldController.getCreaturesController(),
                positionSystem));
        controllers.add(new PlayerTriggerLogicController(this, levelInfo, gameTimer,
                gameWorldController.getMapController(), gameWorldController.getCreaturesController(),
                playerService));
        controllers.add(gameTimer);
        controllers.add(positionSystem);

        int triggerId = levelInfo.kwdFile.getGameLevel().getTriggerId();
        if (triggerId != 0) {
            triggerControl = new TriggerControl(this, levelInfo, gameTimer, gameWorldController.getMapController(), gameWorldController.getCreaturesController(), triggerId);
        }

    }

    private void setupPlayers() {
        // Setup players
        boolean addMissingPlayers = levelInfo.players.isEmpty(); // Add all if none is given (campaign...)
        for (Map.Entry<Short, Player> entry : levelInfo.kwdFile.getPlayers().entrySet()) {
            Keeper keeper = null;
            if (levelInfo.players.containsKey(entry.getKey())) {
                keeper = levelInfo.players.get(entry.getKey());
                keeper.setPlayer(entry.getValue());
            } else if (addMissingPlayers || entry.getKey() < Player.KEEPER1_ID) {
                keeper = new Keeper(entry.getValue());
                levelInfo.players.put(entry.getKey(), keeper);
            }

            // Init
            if (keeper != null) {
                PlayerController playerController = new PlayerController(levelInfo.kwdFile, keeper, levelInfo.kwdFile.getImp(), entityData, gameSettings);
                playerControllers.put(entry.getKey(), playerController);

                // Spells are all available for research unless otherwise stated
                for (KeeperSpell spell : levelInfo.kwdFile.getKeeperSpells()) {
                    if (spell.getBonusRTime() != 0) {
                        playerController.getSpellControl().setTypeAvailable(spell, true, false);
                    }
                }
            }
        }

        // Set the alliances
        for (Variable.PlayerAlliance playerAlliance : levelInfo.kwdFile.getPlayerAlliances()) {
            short player1 = (short) playerAlliance.getPlayerIdOne();
            short player2 = (short) playerAlliance.getPlayerIdTwo();
            createAlliance(player1, player2);
        }

        // Set player availabilities
        // TODO: the player customized game settings
        for (Variable.Availability availability : levelInfo.kwdFile.getAvailabilities()) {
            if (availability.getPlayerId() == 0) {

                // All players
                for (Keeper player : levelInfo.players.values()) {
                    setAvailability(player, availability);
                }
            } else {
                Keeper player = levelInfo.players.get((short) availability.getPlayerId());

                // Not all the players are participating...
                if (player != null) {
                    setAvailability(player, availability);
                }
            }
        }

        // Initialize the player research
        for (IPlayerController playerController : playerControllers.values()) {
            PlayerResearchControl researchControl = playerController.getResearchControl();
            if (researchControl != null) {
                researchControl.initialize();
            }
        }
    }

    private void setAvailability(Keeper player, Variable.Availability availability) {
        IPlayerController playerController = playerControllers.get(player.getId());
        boolean available = availability.getValue() == Variable.Availability.AvailabilityValue.AVAILABLE || availability.getValue() == Variable.Availability.AvailabilityValue.RESEARCHABLE;
        boolean discovered = availability.getValue() == Variable.Availability.AvailabilityValue.AVAILABLE;
        switch (availability.getType()) {
            case CREATURE: {
                playerController.getCreatureControl().setTypeAvailable(levelInfo.kwdFile.getCreature((short) availability.getTypeId()), available);
                break;
            }
            case ROOM: {
                playerController.getRoomControl().setTypeAvailable(levelInfo.kwdFile.getRoomById((short) availability.getTypeId()), available, discovered);
                break;
            }
            case SPELL: {
                playerController.getSpellControl().setTypeAvailable(levelInfo.kwdFile.getKeeperSpellById(availability.getTypeId()), available, discovered);
                break;
            }
            case DOOR: {
                playerController.getDoorControl().setTypeAvailable(levelInfo.kwdFile.getDoorById((short) availability.getTypeId()), available, discovered);
                break;
            }
            case TRAP: {
                playerController.getTrapControl().setTypeAvailable(levelInfo.kwdFile.getTrapById((short) availability.getTypeId()), available, discovered);
                break;
            }
        }
    }

    @Override
    public IPlayerController getPlayerController(short playerId) {
        return playerControllers.get(playerId);
    }

    @Override
    public Map<Short, IPlayerController> getPlayerControllers() {
        return playerControllers;
    }

    @Override
    public void processTick(float tpf) {

        // Update time for AI
        GdxAI.getTimepiece().update(tpf);

        // Time limit is a special timer, it just ticks towards 0 if it is set
        if (levelInfo.timeLimit != null) {
            levelInfo.timeLimit -= tpf;

            // We rest on zero
            if (levelInfo.timeLimit < 0) {
                levelInfo.timeLimit = 0f;
            }
        }

        // Advance game timers
        for (GameTimer timer : levelInfo.timers.getArray()) {
            timer.update(tpf);
        }

        if (triggerControl != null) {
            triggerControl.update(tpf);
        }

        controllers.stream().forEach(controller -> controller.processTick(tpf));

        if (levelInfo.timeLimit != null && levelInfo.timeLimit <= 0) {
            //TODO:
            throw new RuntimeException("Level time limit exceeded!");
        }
    }

    @Override
    public void endGame(short playerId, boolean win) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setEnd(boolean win) {

        // TODO: this is client stuff, we should only determine here what happens to the game & players
        gameResult = new GameResult();
        gameResult.setData(GameResult.ResultType.LEVEL_WON, win);
        gameResult.setData(GameResult.ResultType.TIME_TAKEN, getContoller(IGameTimer.class).getGameTime());

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
                logger.log(Level.ERROR, "Failed to save the level progress!", ex);
            }
        }
    }

    @Override
    public ITaskManager getTaskManager() {
        return taskManager;
    }

    /**
     * Get level variable value
     *
     * @param variable the variable type
     * @return variable value
     */
    @Override
    public float getLevelVariable(Variable.MiscVariable.MiscType variable) {
        // TODO: player is able to change these, so need a wrapper and store these to GameState
        return levelInfo.kwdFile.getVariables().get(variable).getValue();
    }

    public <T> T getContoller(Class<T> clazz) {
        return (T) controllers.stream()
                .filter(controller -> controller.getClass().isInstance(clazz))
                .findFirst().orElseThrow(() -> new IndexOutOfBoundsException(
                "Game contoller have not child of class " + clazz));
    }

    /**
     * Creates alliance between two players
     *
     * @param playerOneId player ID 1
     * @param playerTwoId player ID 2
     */
    @Override
    public void createAlliance(short playerOneId, short playerTwoId) {
        if (levelInfo.players.containsKey(playerOneId)) {
            levelInfo.players.get(playerOneId).createAlliance(playerTwoId);
        }
        if (levelInfo.players.containsKey(playerTwoId)) {
            levelInfo.players.get(playerTwoId).createAlliance(playerOneId);
        }
    }

    /**
     * Breaks alliance between two players
     *
     * @param playerOneId player ID 1
     * @param playerTwoId player ID 2
     */
    @Override
    public void breakAlliance(short playerOneId, short playerTwoId) {
        if (levelInfo.players.containsKey(playerOneId)) {
            levelInfo.players.get(playerOneId).breakAlliance(playerTwoId);
        }
        if (levelInfo.players.containsKey(playerTwoId)) {
            levelInfo.players.get(playerTwoId).breakAlliance(playerOneId);
        }
    }

    @Override
    public GameResult getGameResult() {
        return gameResult;
    }

    @Override
    public void setPossession(EntityId target, short playerId) {
        levelInfo.players.get(playerId).setPossession(target != null);
        playerService.setPossession(target, playerId);
    }

    @Override
    public IGameWorldController getGameWorldController() {
        return gameWorldController;
    }

    @Override
    public void start() {
        controllers.stream().forEach(IGameLogicUpdatable::start);
    }

    @Override
    public void stop() {
        controllers.stream().forEach(IGameLogicUpdatable::stop);
        controllers.clear();
    }

    @Override
    public INavigationService getNavigationService() {
        return navigationService;
    }

    @Override
    public IEntityPositionLookup getEntityLookupService() {
        return positionSystem;
    }

    @Override
    public ILevelInfo getLevelInfo() {
        return levelInfo;
    }

    private static class LevelInfo implements ILevelInfo {

        private static final int LEVEL_TIMER_MAX_COUNT = 16;
        private static final int LEVEL_FLAG_MAX_COUNT = 128;

        private final IKwdFile kwdFile;
        private int levelScore = 0;
        private Float timeLimit = null;

        private final SortedMap<Short, Keeper> players = new TreeMap<>();
        private final List<Integer> flags = new ArrayList<>(LEVEL_FLAG_MAX_COUNT);
        private final SafeArrayList<GameTimer> timers = new SafeArrayList<>(GameTimer.class, LEVEL_TIMER_MAX_COUNT);
        private final Map<Integer, ActionPoint> actionPointsById = new HashMap<>();
        private final List<ActionPoint> actionPoints = new ArrayList<>();

        public LevelInfo(IKwdFile kwdFile) {
            this.kwdFile = kwdFile;
        }

        public void load() {
            // Action points
            loadActionPoints();
            // Triggers data
            for (short i = 0; i < LEVEL_FLAG_MAX_COUNT; i++) {
                flags.add(i, 0);
            }
            // Timers data
            for (byte i = 0; i < LEVEL_TIMER_MAX_COUNT; i++) {
                timers.add(i, new GameTimer());
            }
        }

        private void loadActionPoints() {
            for (Thing.ActionPoint thing : kwdFile.getThings(Thing.ActionPoint.class)) {
                ActionPoint ap = new ActionPoint(thing);
                actionPointsById.put(ap.getId(), ap);
                actionPoints.add(ap);
            }
        }

        @Override
        public IKwdFile getLevelData() {
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

        @Override
        public Float getTimeLimit() {
            return timeLimit;
        }

        @Override
        public void setTimeLimit(float time) {
            timeLimit = time;
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
        public void setLevelScore(int score) {
            levelScore = score;
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
        public Keeper getPlayer(short playerId) {
            return players.get(playerId);
        }

        @Override
        public Map<Short, Keeper> getPlayers() {
            return players;
        }
    }

}
