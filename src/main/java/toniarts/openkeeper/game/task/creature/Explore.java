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
package toniarts.openkeeper.game.task.creature;

import com.jme3.math.Vector2f;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractTileTask;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Explores the map. Like in DK II (0x45A290, state 0xE1), the creature walks
 * off to a tile nobody has claimed, and once there it looks for a job again
 */
public final class Explore extends AbstractTileTask {

    public Explore(final INavigationService navigationService, final IMapController mapController, Point p, short playerId) {
        super(navigationService, mapController, p, playerId);
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        return true;
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(getTaskLocation());
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        creature.unassingCurrentTask();
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.EXPLORE;
    }

}
