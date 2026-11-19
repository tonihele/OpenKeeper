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
package toniarts.openkeeper.game.task;

import com.simsilica.es.EntityId;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/** Opt-in use lifecycle for tasks working at a particular floor furniture entity. */
public abstract class AbstractFurnitureTask extends AbstractCapacityCriticalRoomTask {
    private final IObjectsController objectsController;
    private IObjectController furniture;

    protected AbstractFurnitureTask(INavigationService navigationService, IMapController mapController,
            Point p, short playerId, IRoomController room, TaskManager taskManager,
            IObjectsController objectsController) {
        super(navigationService, mapController, p, playerId, room, taskManager);
        this.objectsController = objectsController;
    }

    /** A subclass must disambiguate object types when several objects share a tile. */
    protected abstract boolean acceptsFurniture(IObjectController object);

    @Override
    public boolean isValid(ICreatureController creature) {
        return super.isValid(creature) && (furniture == null || isFurniturePresent());
    }

    private boolean isFurniturePresent() {
        return !furniture.isRemoved() && room.getFloorFurniture().contains(furniture.getEntityId());
    }

    @Override
    public final void executeTask(ICreatureController creature, float executionDuration) {
        // Execution is called only after arrival and task validation, never on assignment.
        if (room != null && (room.isRemoved() || room.isDestroyed() || room.getOwnerId() != playerId
                || creature.getOwnerId() != playerId)) {
            if (furniture != null) {
                furniture.endUse(creature.getEntityId());
            }
            return;
        }
        if (furniture == null && room != null && objectsController != null) {
            IObjectController match = null;
            for (EntityId id : room.getFloorFurniture()) {
                IObjectController candidate = objectsController.createController(id);
                if (!candidate.isRemoved() && candidate.getPosition() != null
                        && getTaskLocation().equals(WorldUtils.vectorToPoint(candidate.getPosition()))
                        && acceptsFurniture(candidate)) {
                    // Retain this exact entity, so rebuilding on the same tile cannot
                    // redirect an old task's end-use to the replacement furniture.
                    if (match != null) {
                        // Ambiguous furniture needs an explicit object-type selector.
                        match = null;
                        break;
                    }
                    match = candidate;
                }
            }
            furniture = match;
        }
        if (furniture != null) {
            if (!isFurniturePresent()) {
                furniture.endUse(creature.getEntityId());
                return;
            }
            furniture.beginUse(creature.getEntityId());
        }
        executeFurnitureTask(creature, executionDuration);
    }

    protected abstract void executeFurnitureTask(ICreatureController creature, float executionDuration);

    @Override
    public void unassign(ICreatureController creature) {
        if (furniture != null) {
            furniture.endUse(creature.getEntityId());
        }
        super.unassign(creature);
    }
}
