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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.controller.IDoorsController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.Point;

/**
 * A worker fetches a door crate, from the workshop or from the floor, and
 * carries it to its blueprint. As soon as it gets there, the door is built,
 * as in the original there is no building time.
 */
public final class BuildDoorTask extends AbstractDeliverCrateTask {

    private final IDoorsController doorsController;

    public BuildDoorTask(final INavigationService navigationService, final IMapController mapController, EntityData entityData,
            IDoorsController doorsController, IObjectsController objectsController, EntityId blueprint, Point blueprintTile, EntityId crate,
            short playerId) {
        super(navigationService, mapController, entityData, objectsController, blueprint, blueprintTile, crate, playerId);

        this.doorsController = doorsController;
    }

    @Override
    protected boolean isBlueprint() {
        DoorComponent doorComponent = entityData.getComponent(blueprint, DoorComponent.class);
        return doorComponent != null && doorComponent.blueprint;
    }

    @Override
    protected void install() {
        doorsController.buildDoor(blueprint);
    }

    @Override
    protected TaskType getFetchTaskType() {
        return TaskType.FETCH_DOOR_CRATE;
    }

    @Override
    protected TaskType getDeliverTaskType() {
        return TaskType.DELIVER_DOOR_CRATE;
    }

    @Override
    protected String getDescription() {
        return "Building door";
    }

}
