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
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Rebels with no portal to leave through. Like in DK II, the creature stays
 * where it is for a moment and then turns on its keeper
 */
public final class Rebel extends AbstractTileTask {

    private static final int TICKS_BEFORE_TURNING = 30;

    private final int ticksPerSecond;

    public Rebel(final INavigationService navigationService, final IMapController mapController, ICreatureController creature,
            int ticksPerSecond) {
        super(navigationService, mapController, creature.getCreatureCoordinates(), creature.getOwnerId());

        this.ticksPerSecond = ticksPerSecond;
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
        if (executionDuration * ticksPerSecond > TICKS_BEFORE_TURNING) {
            creature.rebel();
        }
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.REBEL;
    }

}
