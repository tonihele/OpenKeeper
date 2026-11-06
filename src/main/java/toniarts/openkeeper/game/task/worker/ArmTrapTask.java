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
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.ITrapsController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.Point;

/**
 * A worker fetches a trap crate, from the workshop or from the floor, and
 * carries it to its blueprint. As soon as it gets there, the trap is armed.
 */
public final class ArmTrapTask extends AbstractDeliverCrateTask {

    private final ITrapsController trapsController;

    public ArmTrapTask(final INavigationService navigationService, final IMapController mapController, EntityData entityData,
            ITrapsController trapsController, IObjectsController objectsController, EntityId blueprint, Point blueprintTile, EntityId crate,
            short playerId) {
        super(navigationService, mapController, entityData, objectsController, blueprint, blueprintTile, crate, playerId);

        this.trapsController = trapsController;
    }

    @Override
    protected boolean isBlueprint() {
        TrapViewState viewState = entityData.getComponent(blueprint, TrapViewState.class);
        return viewState != null && viewState.blueprint;
    }

    @Override
    protected void install() {
        trapsController.armTrap(blueprint);
    }

    @Override
    protected TaskType getFetchTaskType() {
        return TaskType.FETCH_TRAP_CRATE;
    }

    @Override
    protected TaskType getDeliverTaskType() {
        return TaskType.DELIVER_TRAP_CRATE;
    }

    @Override
    protected String getDescription() {
        return "Arming trap";
    }

}
