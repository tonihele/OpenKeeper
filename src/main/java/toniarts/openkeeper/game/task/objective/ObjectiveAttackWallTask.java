/*
 * Copyright (C) 2026 OpenKeeper
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
package toniarts.openkeeper.game.task.objective;

import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.worker.DigTileTask;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.Point;

/** Party workers break attackable walls through DKII's separate wall-work path. */
public final class ObjectiveAttackWallTask extends DigTileTask {
    private boolean completed;

    public ObjectiveAttackWallTask(INavigationService navigationService, IMapController mapController,
            Point point, short playerId) {
        super(navigationService, mapController, point, playerId);
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        IMapTileInformation tile = mapController.getMapData().getTile(getTaskLocation());
        Terrain terrain = mapController.getTerrain(tile);
        return !completed && tile.getOwnerId() != Player.NEUTRAL_PLAYER_ID
                && terrain.getGoldValue() == 0
                && terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)
                && terrain.getFlags().contains(Terrain.TerrainFlag.ATTACKABLE)
                && !terrain.getFlags().contains(Terrain.TerrainFlag.IMPENETRABLE);
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        if (creature.isWorker() && isValid(creature) && isWorkStepDue(creature, executionDuration)) {
            completed = mapController.attackWall(getTaskLocation(), playerId);
        }
    }
}
