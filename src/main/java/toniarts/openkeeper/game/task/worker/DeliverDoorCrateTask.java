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
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.HauledBy;
import toniarts.openkeeper.game.controller.IDoorsController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractTask;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * A worker carries the door crate it fetched ({@link BuildDoorTask}) to the
 * blueprint. As soon as it gets there, the door is built, as in the original
 * there is no building time.
 */
public final class DeliverDoorCrateTask extends AbstractTask {

    private final EntityData entityData;
    private final IDoorsController doorsController;
    private final IObjectsController objectsController;
    private final EntityId blueprint;
    private final EntityId crate;
    private final Point blueprintTile;
    private boolean executed = false;

    public DeliverDoorCrateTask(final INavigationService navigationService, final IMapController mapController, EntityData entityData,
            IDoorsController doorsController, IObjectsController objectsController, EntityId blueprint, Point blueprintTile,
            EntityId crate) {
        super(navigationService, mapController);

        this.entityData = entityData;
        this.doorsController = doorsController;
        this.objectsController = objectsController;
        this.blueprint = blueprint;
        this.blueprintTile = blueprintTile;
        this.crate = crate;
    }

    @Override
    public Point getTaskLocation() {
        return blueprintTile;
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(getTaskLocation());
    }

    @Override
    public EntityId getTaskTarget() {
        return blueprint;
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        if (executed) {
            return false;
        }

        // The blueprint may be sold, and the worker must still carry the crate
        DoorComponent doorComponent = entityData.getComponent(blueprint, DoorComponent.class);
        HauledBy hauledBy = entityData.getComponent(crate, HauledBy.class);
        return doorComponent != null && doorComponent.blueprint
                && hauledBy != null && (creature == null || hauledBy.entityId.equals(creature.getEntityId()));
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        executed = true;

        // Let go of the crate first, so it isn't moved along anymore once it is gone
        IObjectController crateController = objectsController.createController(crate);
        crateController.setHaulable(null);
        doorsController.buildDoor(blueprint);
        crateController.remove();
    }

    @Override
    public void unassign(ICreatureController creature) {
        super.unassign(creature);

        // The crate is dropped where the worker is, somebody can fetch it from there
        if (!executed) {
            executed = true;
            HauledBy hauledBy = entityData.getComponent(crate, HauledBy.class);
            if (hauledBy != null && hauledBy.entityId.equals(creature.getEntityId())) {
                objectsController.createController(crate).setHaulable(null);
            }
        }
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.DELIVER_DOOR_CRATE;
    }

    @Override
    public String toString() {
        return "Delivering crate " + crate + " to the door at " + blueprintTile;
    }

}
