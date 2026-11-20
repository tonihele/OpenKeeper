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
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.HauledBy;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.IDoorsController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractTask;
import toniarts.openkeeper.game.task.ITaskManager;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * A worker fetches a door crate for a blueprint, from the workshop or from
 * the floor. Once it has the crate, it gets a {@link DeliverDoorCrateTask} to
 * carry it to the blueprint. This task stays until the door is built, so a
 * crate dropped on the way is fetched again.
 */
public final class BuildDoorTask extends AbstractTask {

    private final ITaskManager taskManager;
    private final EntityData entityData;
    private final IDoorsController doorsController;
    private final IObjectsController objectsController;
    private final EntityId blueprint;
    private final EntityId crate;
    private final Point blueprintTile;
    private final short playerId;

    public BuildDoorTask(final ITaskManager taskManager, final INavigationService navigationService, final IMapController mapController,
            EntityData entityData, IDoorsController doorsController, IObjectsController objectsController, EntityId blueprint,
            Point blueprintTile, EntityId crate, short playerId) {
        super(navigationService, mapController);

        this.taskManager = taskManager;
        this.entityData = entityData;
        this.doorsController = doorsController;
        this.objectsController = objectsController;
        this.blueprint = blueprint;
        this.blueprintTile = blueprintTile;
        this.crate = crate;
        this.playerId = playerId;
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
     * Done, or it can't be done anymore: the door is built, the blueprint is
     * sold or the crate is gone. Unlike {@link #isValid}, which also says no
     * while somebody carries the crate.
     */
    @Override
    public boolean isRemovable() {
        if (entityData.getComponent(crate, Crate.class) == null) {
            return true;
        }
        DoorComponent doorComponent = entityData.getComponent(blueprint, DoorComponent.class);
        return doorComponent == null || !doorComponent.blueprint;
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
        DeliverDoorCrateTask task = new DeliverDoorCrateTask(navigationService, mapController, entityData, doorsController,
                objectsController, blueprint, blueprintTile, crate);
        taskManager.addTask(playerId, task);
        task.assign(creature, true);
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.FETCH_DOOR_CRATE;
    }

    @Override
    public String toString() {
        return "Fetching crate " + crate + " for the door at " + blueprintTile;
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
        final BuildDoorTask other = (BuildDoorTask) obj;
        return playerId == other.playerId && Objects.equals(blueprint, other.blueprint);
    }

}
