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
package toniarts.openkeeper.game.task.creature;

import com.jme3.math.Vector2f;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractTileTask;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Go to sleep!
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class GoToSleep extends AbstractTileTask {

    private boolean executed = false;
    private final ICreatureController creature;

    public GoToSleep(final INavigationService navigationService, final IMapController mapController, ICreatureController creature) {
        super(navigationService, mapController, creature.getLairLocation(), creature.getOwnerId());
        this.creature = creature;
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        return !executed && this.creature.hasLair();
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(getTaskLocation()); // FIXME 0.5f not needed?
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        creature.sleep();

        // This is a one timer
        executed = true;
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.GO_TO_SLEEP;
    }
}
