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
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.HauledBy;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractTileTask;
import toniarts.openkeeper.game.task.ITaskManager;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * A task for workers to pick up a dead body and take it to the graveyard. As
 * in DK II, any body will do, whoever it belonged to, as long as it lies on
 * the player's own land and the player has a graveyard with room for it.
 */
public final class FetchCorpseTask extends AbstractTileTask {

    private final EntityData entityData;
    private final ITaskManager taskManager;
    private final EntityId corpse;

    public FetchCorpseTask(final INavigationService navigationService, final IMapController mapController,
            EntityData entityData, ITaskManager taskManager, EntityId corpse, short playerId) {
        super(navigationService, mapController, WorldUtils.vectorToPoint(entityData.getComponent(corpse, Position.class).position), playerId);

        this.entityData = entityData;
        this.taskManager = taskManager;
        this.corpse = corpse;
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(getTaskLocation());
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        return isCorpseLyingFree() && isOnOwnLand() && !isGraveyardCapacityFull();
    }

    private boolean isCorpseLyingFree() {
        return entityData.getComponent(corpse, Death.class) != null
                && entityData.getComponent(corpse, Stored.class) == null
                && entityData.getComponent(corpse, HauledBy.class) == null;
    }

    private boolean isOnOwnLand() {
        IMapTileInformation tile = mapController.getMapData().getTile(getTaskLocation());
        return tile != null && tile.getOwnerId() == playerId;
    }

    private boolean isGraveyardCapacityFull() {
        for (IRoomController room : mapController.getRoomsByFunction(ObjectType.CORPSE, playerId)) {
            if (!room.isFullCapacity()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isRemovable() {
        return entityData.getComponent(corpse, Death.class) == null;
    }

    @Override
    public String toString() {
        return "Collecting a corpse at " + getTaskLocation();
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        if (taskManager.assignClosestRoomTask(creature, ObjectType.CORPSE, corpse)) {
            entityData.setComponent(corpse, new HauledBy(creature.getEntityId()));
        }
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.FETCH_CORPSE;
    }

    @Override
    public EntityId getTaskTarget() {
        return corpse;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.corpse);
        hash = 59 * hash + this.playerId;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FetchCorpseTask other = (FetchCorpseTask) obj;
        return this.playerId == other.playerId && Objects.equals(this.corpse, other.corpse);
    }

}
