/*
 * Copyright (C) 2014-2016 OpenKeeper
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
import java.util.Objects;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractTileTask;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * A task for creatures to get an object from the game world
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class FetchObjectTask extends AbstractTileTask {

    private final IObjectController gameObject;

    public FetchObjectTask(final INavigationService navigationService, final IMapController mapController, final IObjectController objectController, short playerId) {
        super(navigationService, mapController, WorldUtils.vectorToPoint(objectController.getPosition()), playerId);
        this.gameObject = objectController;
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(getTaskLocation()); // FIXME 0.5f not needed?
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        return gameObject.isPickableByPlayerCreature(playerId) && !isPlayerCapacityFull();
    }

    @Override
    public int getPriority() {
        return gameObject.getPickUpPriority();
    }

    @Override
    public String toString() {
        return "Collecting item at " + getTaskLocation();
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        gameObject.creaturePicksUp(creature);

        // TODO: perhaps chaining? everything except gold, maybe a new task class...? Fetch & deliver
    }

    private boolean isPlayerCapacityFull() {
        for (IRoomController room : mapController.getRoomsByFunction(gameObject.getType(), playerId)) {
            if (!room.isFullCapacity()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isRemovable() {
        return gameObject.isRemoved() || gameObject.isStoredInRoom();
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.FETCH_OBJECT;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.gameObject);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FetchObjectTask other = (FetchObjectTask) obj;
        if (!Objects.equals(this.gameObject, other.gameObject)) {
            return false;
        }
        return true;
    }

}
