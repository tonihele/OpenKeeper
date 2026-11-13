/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.game.task.worker;

import com.jme3.math.Vector2f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.Objects;
import toniarts.openkeeper.game.component.Crate;
import toniarts.openkeeper.game.component.HauledBy;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.ITrapsController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractTask;
import toniarts.openkeeper.game.task.ITaskManager;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * A worker fetches a trap crate for a blueprint, from the workshop or from
 * the floor. Once it has the crate, it gets a {@link DeliverTrapCrateTask} to
 * carry it to the blueprint. This task stays until the trap is armed, so a
 * crate dropped on the way is fetched again.
 */
public final class ArmTrapTask extends AbstractTask {

    private final ITaskManager taskManager;
    private final EntityData entityData;
    private final ITrapsController trapsController;
    private final IObjectsController objectsController;
    private final EntityId blueprint;
    private final EntityId crate;
    private final Point blueprintTile;
    private final short playerId;

    public ArmTrapTask(final ITaskManager taskManager, final INavigationService navigationService, final IMapController mapController,
            EntityData entityData, ITrapsController trapsController, IObjectsController objectsController, EntityId blueprint,
            Point blueprintTile, EntityId crate, short playerId) {
        super(navigationService, mapController);

        this.taskManager = taskManager;
        this.entityData = entityData;
        this.trapsController = trapsController;
        this.objectsController = objectsController;
        this.blueprint = blueprint;
        this.blueprintTile = blueprintTile;
        this.crate = crate;
        this.playerId = playerId;
    }

    public EntityId getBlueprint() {
        return blueprint;
    }

    public EntityId getCrate() {
        return crate;
    }

    @Override
    public Point getTaskLocation() {
        Position position = entityData.getComponent(crate, Position.class);
        if (position != null) {
            return WorldUtils.vectorToPoint(position.position);
        }
        return blueprintTile;
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(getTaskLocation());
    }

    @Override
    public EntityId getTaskTarget() {
        return crate;
    }

    /**
     * Done, or it can't be done anymore: the trap is armed, the blueprint is
     * sold or the crate is gone. Unlike {@link #isValid}, which also says no
     * while somebody carries the crate.
     */
    @Override
    public boolean isRemovable() {
        if (entityData.getComponent(crate, Crate.class) == null) {
            return true;
        }
        TrapViewState viewState = entityData.getComponent(blueprint, TrapViewState.class);
        return viewState == null || !viewState.blueprint;
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        return !isRemovable() && entityData.getComponent(crate, HauledBy.class) == null;
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {

        // Out of the workshop storage, the crates left in the stack settle
        Stored stored = entityData.getComponent(crate, Stored.class);
        if (stored != null) {
            IRoomController room = mapController.getRoomControllerByCoordinates(getTaskLocation());
            if (room != null && room.getObjectControl(stored.objectType) != null) {
                room.getObjectControl(stored.objectType).removeItem(crate);
            } else {
                entityData.removeComponent(crate, Stored.class);
            }
        }
        objectsController.createController(crate).setHaulable(creature);

        // Assign carry to blueprint
        DeliverTrapCrateTask task = new DeliverTrapCrateTask(navigationService, mapController, entityData, trapsController,
                objectsController, blueprint, blueprintTile, crate);
        taskManager.addTask(playerId, task);
        task.assign(creature, true);
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.FETCH_TRAP_CRATE;
    }

    @Override
    public String toString() {
        return "Fetching crate " + crate + " for the trap at " + blueprintTile;
    }

    @Override
    public int hashCode() {
        return Objects.hash(blueprint, playerId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ArmTrapTask other = (ArmTrapTask) obj;
        return playerId == other.playerId && Objects.equals(blueprint, other.blueprint);
    }

}
