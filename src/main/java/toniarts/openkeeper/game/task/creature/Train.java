/*
 * Copyright (C) 2014-2023 OpenKeeper
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
package toniarts.openkeeper.game.task.creature;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import java.util.Map;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractCapacityCriticalRoomTask;
import toniarts.openkeeper.game.task.TaskManager;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Trains creature
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class Train extends AbstractCapacityCriticalRoomTask {

    /**
     * How far from the dummy the trainee stands, in tiles
     */
    private static final float DISTANCE_FROM_DUMMY = 0x7FF / 4096f;

    private final IGameWorldController gameWorldController;
    private final IPlayerController playerController;
    private final int trainingRoomMaxExperienceLevel;
    private final int trainingCost;
    private final EntityId wallTarget;
    private final Point shootingTile;
    private Vector2f workPosition;

    /**
     * Trains at a Training Dummy or a wall target
     *
     * @param navigationService the navigation
     * @param mapController the map
     * @param p the tile of the Training Dummy or the wall target
     * @param playerId the player
     * @param room the Training Room
     * @param taskManager the task manager
     * @param gameWorldController the game world
     * @param gameSettings the level variables
     * @param playerController the player
     * @param wallTarget the wall target to shoot at, {@code null} at a
     * Training Dummy
     * @param shootingTile the tile to shoot at the wall target from
     */
    public Train(final INavigationService navigationService, final IMapController mapController,
            Point p, short playerId, IRoomController room, TaskManager taskManager,
            IGameWorldController gameWorldController, Map<MiscType, MiscVariable> gameSettings,
            IPlayerController playerController, EntityId wallTarget, Point shootingTile) {
        super(navigationService, mapController, p, playerId, room, taskManager);

        this.gameWorldController = gameWorldController;
        this.playerController = playerController;
        this.wallTarget = wallTarget;
        this.shootingTile = shootingTile;

        trainingRoomMaxExperienceLevel = (int) gameSettings.get(Variable.MiscVariable.MiscType.TRAINING_ROOM_MAX_EXPERIENCE_LEVEL).getValue();
        trainingCost = Math.abs((int) gameSettings.get(Variable.MiscVariable.MiscType.MODIFY_PLAYER_GOLD_WHILE_TRAINING_PER_SECOND).getValue());
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        return playerController.getKeeper().getGold() >= trainingCost && creature.getLevel() < trainingRoomMaxExperienceLevel && super.isValid(creature);
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        if (wallTarget == null) {
            return getTargetNextToFurniture(creature);
        }
        return shootingTile != null ? WorldUtils.pointToVector2f(shootingTile) : null;
    }

    @Override
    public EntityId getTaskTarget() {
        return wallTarget;
    }

    @Override
    public boolean isFaceTarget() {
        return true;
    }

    @Override
    public Vector2f getWorkPosition(ICreatureController creature) {

        // At a wall target the trainee stands on its shooting tile
        if (wallTarget != null) {
            return getTarget(creature);
        }

        // DK II puts the trainee just under half a tile from the dummy, in a random direction (0x485310). Picked
        // once, the position is asked every tick
        if (workPosition == null) {
            float angle = Utils.getRandom().nextFloat() * FastMath.TWO_PI;
            workPosition = WorldUtils.pointToVector2f(getTaskLocation()).addLocal(FastMath.cos(angle) * DISTANCE_FROM_DUMMY,
                    FastMath.sin(angle) * DISTANCE_FROM_DUMMY);
        }
        return workPosition;
    }

    @Override
    protected ObjectType getRoomObjectType() {
        return ObjectType.TRAINEE;
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {

        // Once a second
        if (!isWorkStepDue(creature, executionDuration)) {
            return;
        }
        gameWorldController.substractGold(trainingCost, playerId);
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.TRAIN;
    }

    @Override
    public boolean isRemovable() {
        return true;
    }
}
