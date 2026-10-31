/*
 * Copyright (C) 2014-2025 OpenKeeper
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
package toniarts.openkeeper.game.state.loop;

import com.simsilica.es.EntityData;
import toniarts.openkeeper.game.controller.GameController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.logic.ChickenAiSystem;
import toniarts.openkeeper.game.logic.ChickenSpawnSystem;
import toniarts.openkeeper.game.logic.CreatureAiSystem;
import toniarts.openkeeper.game.logic.CreatureExperienceSystem;
import toniarts.openkeeper.game.logic.CreatureFallSystem;
import toniarts.openkeeper.game.logic.CreatureMoodSystem;
import toniarts.openkeeper.game.logic.CreatureSleepSystem;
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
import toniarts.openkeeper.game.logic.LooseObjectSystem;
import toniarts.openkeeper.game.logic.ManaCalculatorLogic;
import toniarts.openkeeper.game.logic.MovementSystem;
import toniarts.openkeeper.game.logic.PlayerCreatureSystem;
import toniarts.openkeeper.game.logic.PlayerSpellbookSystem;
import toniarts.openkeeper.game.logic.PossessedMovementSystem;
import toniarts.openkeeper.game.logic.PossessedSystem;
import toniarts.openkeeper.game.logic.SlapSystem;
import toniarts.openkeeper.game.logic.WoodenBridgeDecaySystem;
import toniarts.openkeeper.game.state.session.GameSessionServerService;
import toniarts.openkeeper.game.task.ITaskManager;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.GameLoop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ArchDemon
 */
public final class GameLoopManager {

    private final List<GameLoop> loops = new ArrayList<>();

    private final GameSessionServerService gameService;

    private final IGameController gameController;

    /**
     * Single use game states
     *
     * @param level the level to load
     * @param gameService
     * @param players player participating in this game, can be {@code null}
     */
    public GameLoopManager(IKwdFile level, GameSessionServerService gameService, List<Keeper> players) {
        this.gameService = gameService;
        final EntityData entityData = gameService.getServerEntityData();
        final Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings = level.getVariables();

        gameController = new GameController(level, players, gameService.getServerEntityData(), gameSettings, gameService);
        final ILevelInfo levelInfo = gameController.getLevelInfo();
        final IGameWorldController gameWorldController = gameController.getGameWorldController();
        final IEntityPositionLookup positionSystem = gameController.getEntityLookupService();
        final Map<Short, IPlayerController> playerControllers = gameController.getPlayerControllers();
        final ITaskManager taskManager = gameController.getTaskManager();
        final GameEventService gameEventService = gameController.getGameEventService();

        // Game logic
        final GameLogicManager gameLogicThread = new GameLogicManager(
                gameWorldController.getMapController(),
                new DecaySystem(entityData),
                new WoodenBridgeDecaySystem(entityData, gameController.getGameTimer(), gameWorldController,
                        level, gameWorldController.getMapController(),
                        gameController.getLevelVariable(Variable.MiscVariable.MiscType.WOOD_BRIDGE_LIFE_ON_LAVA_SECONDS)),
                new CreatureExperienceSystem(entityData, levelInfo.getLevelData(), gameSettings,
                        gameWorldController.getCreaturesController()),
                new CreatureSleepSystem(entityData, levelInfo.getLevelData(),
                        gameController.getGameTimer()),
                new CreatureMoodSystem(entityData, levelInfo.getLevelData(), playerControllers.values(),
                        gameController.getGameTimer(), gameSettings),
                new SlapSystem(entityData, levelInfo.getLevelData(), playerControllers.values(), gameSettings),
                new HealthSystem(entityData, positionSystem, gameSettings, gameWorldController.getCreaturesController(),
                        levelInfo, playerControllers.values(), gameWorldController.getMapController(),
                        gameController.getGameTimer(), gameEventService),
                new CreatureTorturingSystem(entityData, gameWorldController.getCreaturesController(),
                        gameWorldController.getMapController()),
                new DeathSystem(entityData, gameSettings, positionSystem),
                new PlayerCreatureSystem(entityData, levelInfo.getLevelData(), playerControllers.values()),
                new PlayerSpellbookSystem(entityData, levelInfo.getLevelData(), playerControllers.values()),
                (IGameLogicUpdatable) gameController,
                new CreatureSpawnSystem(entityData, gameWorldController.getCreaturesController(), playerControllers.values(),
                        gameSettings, levelInfo, gameWorldController.getMapController(), gameEventService),
                new ChickenSpawnSystem(entityData, gameWorldController.getObjectsController(),
                        playerControllers.values(), gameSettings, levelInfo, gameWorldController.getMapController()),
                new ManaCalculatorLogic(playerControllers.values(), entityData),
                new PossessedSystem(playerControllers.values(), entityData, gameController,
                        gameWorldController.getCreaturesController()),
                new CreatureAiSystem(entityData, gameWorldController.getCreaturesController(), taskManager),
                new ChickenAiSystem(entityData, gameWorldController.getObjectsController()),
                new CreatureViewSystem(entityData),
                new DoorViewSystem(entityData, positionSystem),
                new LooseObjectSystem(entityData, gameWorldController.getMapController(), playerControllers, positionSystem),
                new HaulingSystem(entityData),
                (IGameLogicUpdatable) taskManager);

        loops.add(new GameLoop(gameLogicThread, 1_000_000_000 / levelInfo.getLevelData().getGameLevel().getTicksPerSec(), "Logic"));

        // Animation systems
        final GameLogicManager gameAnimationThread = new GameLogicManager(
                new DungeonHeartConstruction(
                        entityData,
                        gameController.getLevelVariable(Variable.MiscVariable.MiscType.TIME_BEFORE_DUNGEON_HEART_CONSTRUCTION_BEGINS)),
                new CreatureFallSystem(entityData, gameController.getGameTimer(), gameEventService));
        loops.add(new GameLoop(gameAnimationThread, GameLoop.INTERVAL_FPS_60, "Animation"));

        // Steering
        loops.add(new GameLoop(new GameLogicManager(new MovementSystem(entityData),
                new PossessedMovementSystem(entityData, gameWorldController.getMapController(),
                        positionSystem, gameWorldController.getCreaturesController())), GameLoop.INTERVAL_FPS_60, "Steering"));
    }

    public void pause() {
        loops.stream().forEach(GameLoop::pause);
        gameService.setGamePaused(true);
    }

    public void resume() {
        loops.stream().forEach(GameLoop::resume);
        gameService.setGamePaused(false);
    }

    public void start() {
        loops.stream().forEach(GameLoop::start);
    }

    public void stop() {
        loops.forEach(GameLoop::stop);
        loops.clear();
    }

    public IGameController getGameController() {
        return gameController;
    }

}
