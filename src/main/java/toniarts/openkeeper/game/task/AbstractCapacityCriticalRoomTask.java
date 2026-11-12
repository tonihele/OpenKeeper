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
package toniarts.openkeeper.game.task;

import com.jme3.math.Vector2f;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * A base class for tasks that we should keep track off
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class AbstractCapacityCriticalRoomTask extends AbstractRoomTask {

    protected final TaskManager taskManager;
    private Vector2f placeNextToFurniture;

    public AbstractCapacityCriticalRoomTask(final INavigationService navigationService, final IMapController mapController, Point p, short playerId, IRoomController room, TaskManager taskManager) {
        super(navigationService, mapController, p, playerId, room);

        this.taskManager = taskManager;
    }

    @Override
    public void unassign(ICreatureController creature) {
        super.unassign(creature);

        // Remove us
        taskManager.removeRoomTask(this);
    }

    /**
     * The middle of a free tile next to the furniture the task is at, the
     * furniture itself stands in the middle of its tile. Looked up once, the
     * target is asked every tick and the closest free tile changes as the
     * creature walks
     *
     * @param creature the creature doing the task
     * @return the tile next to the furniture, {@code null} if there is none
     */
    protected Vector2f getTargetNextToFurniture(ICreatureController creature) {
        if (placeNextToFurniture == null) {
            placeNextToFurniture = getAccessibleTargetNextToLocation(creature);
        }
        return placeNextToFurniture;
    }

    /**
     * Where the creature stands to work at the furniture: halfway from the
     * middle of the tile next to it to the furniture. That is right in front
     * of the furniture, at the edge of its tile
     *
     * @param creature the creature doing the task
     * @return the position, {@code null} if there is no free tile next to the
     * furniture
     */
    protected Vector2f getWorkPositionAtFurniture(ICreatureController creature) {
        Vector2f nextToFurniture = getTargetNextToFurniture(creature);
        if (nextToFurniture == null) {
            return null;
        }
        return WorldUtils.pointToVector2f(getTaskLocation()).addLocal(nextToFurniture).multLocal(0.5f);
    }

}
