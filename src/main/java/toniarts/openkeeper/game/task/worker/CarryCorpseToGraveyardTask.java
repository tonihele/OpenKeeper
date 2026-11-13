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
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.HauledBy;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractCapacityCriticalRoomTask;
import toniarts.openkeeper.game.task.TaskManager;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * A task for workers to haul a dead body to a free graveyard tile and lay it
 * to rest there
 */
public final class CarryCorpseToGraveyardTask extends AbstractCapacityCriticalRoomTask {

    private final EntityData entityData;
    private final EntityId corpse;
    private boolean executed = false;

    public CarryCorpseToGraveyardTask(final INavigationService navigationService, final IMapController mapController, Point p, short playerId, IRoomController room,
            TaskManager taskManager, EntityData entityData, EntityId corpse) {
        super(navigationService, mapController, p, playerId, room, taskManager);

        this.entityData = entityData;
        this.corpse = corpse;
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(getTaskLocation());
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        return !executed && entityData.getComponent(corpse, Death.class) != null && super.isValid(creature);
    }

    @Override
    public String toString() {
        return "Carrying a corpse to the graveyard at " + getTaskLocation();
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        executed = true;
        entityData.removeComponent(corpse, HauledBy.class);
        if (entityData.getComponent(corpse, Death.class) != null) {
            getRoomObjectControl().addItem(corpse, getTaskLocation());
        }
    }

    @Override
    protected ObjectType getRoomObjectType() {
        return ObjectType.CORPSE;
    }

    @Override
    public void unassign(ICreatureController creature) {
        super.unassign(creature);

        // The body is dropped where the worker stands
        entityData.removeComponent(corpse, HauledBy.class);
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.CARRY_CORPSE_TO_GRAVEYARD;
    }

    @Override
    public EntityId getTaskTarget() {
        return corpse;
    }

}
