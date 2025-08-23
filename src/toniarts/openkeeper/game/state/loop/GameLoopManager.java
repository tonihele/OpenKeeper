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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.game.controller.GameController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.*;
import toniarts.openkeeper.game.state.session.GameSessionServerService;
import toniarts.openkeeper.game.task.ITaskManager;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.GameLoop;

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
    public GameLoopManager(KwdFile level, GameSessionServerService gameService, List<Keeper> players) {
        this.gameService = gameService;
        final EntityData entityData = gameService.getEntityData();
        final Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings = level.getVariables();

        gameController = new GameController(level, players, gameService.getEntityData(), gameSettings, gameService);
        final ILevelInfo levelInfo = gameController.getLevelInfo();
        final IGameWorldController gameWorldController = gameController.getGameWorldController();
        final IEntityPositionLookup positionSystem = gameController.getEntityLookupService();
        final Map<Short, IPlayerController> playerControllers = gameController.getPlayerControllers();
        final ITaskManager taskManager = gameController.getTaskManager();

        // Game logic
        final GameLogicManager gameLogicThread = new GameLogicManager(
                gameWorldController.getMapController(),
                new DecaySystem(entityData),
                new CreatureExperienceSystem(entityData, levelInfo.getLevelData(), gameSettings,
                        gameWorldController.getCreaturesController()),
                new SlapSystem(entityData, levelInfo.getLevelData(), playerControllers.values(), gameSettings),
                new HealthSystem(entityData, positionSystem, gameSettings, gameWorldController.getCreaturesController(),
                        levelInfo, playerControllers.values(), gameWorldController.getMapController()),
                new CreatureTorturingSystem(entityData, gameWorldController.getCreaturesController(),
                        gameWorldController.getMapController()),
                new DeathSystem(entityData, gameSettings, positionSystem),
                new PlayerCreatureSystem(entityData, levelInfo.getLevelData(), playerControllers.values()),
                new PlayerSpellbookSystem(entityData, levelInfo.getLevelData(), playerControllers.values()),
                (IGameLogicUpdatable) gameController,
                new CreatureSpawnSystem(gameWorldController.getCreaturesController(), playerControllers.values(),
                        gameSettings, levelInfo, gameWorldController.getMapController()),
                new ChickenSpawnSystem(entityData, gameWorldController.getObjectsController(),
                        playerControllers.values(), gameSettings, levelInfo, gameWorldController.getMapController()),
                new ManaCalculatorLogic(playerControllers.values(), entityData),
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
                new CreatureFallSystem(entityData));
        loops.add(new GameLoop(gameAnimationThread, GameLoop.INTERVAL_FPS_60, "Animation"));

        // Steering
        loops.add(new GameLoop(new GameLogicManager(new MovementSystem(entityData)), GameLoop.INTERVAL_FPS_60, "Steering"));
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
