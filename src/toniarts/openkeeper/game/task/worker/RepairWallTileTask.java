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

import java.awt.Point;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.TaskType;

/**
 * Repair a claimed wall
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class RepairWallTileTask extends DigTileTask {

    public RepairWallTileTask(final INavigationService navigationService, final IMapController mapController, Point p, short playerId) {
        super(navigationService, mapController, p, playerId);
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        IMapTileInformation tile = mapController.getMapData().getTile(getTaskLocation());
        return mapController.isRepairableWall(getTaskLocation(), playerId) && !tile.isSelected(playerId);
    }

    @Override
    public int getMaxAllowedNumberOfAsignees() {
        // TODO: I think it is 1 per accessible side
        return 1;
    }

    @Override
    public String toString() {
        return "Repair wall at " + getTaskLocation();
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        if (executionDuration - getExecutionDuration(creature) >= 1.0f) {
            setExecutionDuration(creature, executionDuration - getExecutionDuration(creature));

            mapController.applyClaimTile(getTaskLocation(), playerId);
        }
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.REPAIR_WALL;
    }

}
