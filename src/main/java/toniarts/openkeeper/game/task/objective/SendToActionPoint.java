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
package toniarts.openkeeper.game.task.objective;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Send to action point task
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class SendToActionPoint extends GoToTask {

    protected final ActionPoint actionPoint;
    private boolean executed = false;

    public SendToActionPoint(final INavigationService navigationService, final IMapController mapController, ActionPoint actionPoint, ICreatureController creature) {
        super(navigationService, mapController, getDestination(navigationService, mapController, actionPoint, creature), creature.getOwnerId());

        this.actionPoint = actionPoint;
    }

    /**
     * Like the original, the creature heads for a random tile of the action
     * point that it can get to. The middle of the area can well be a wall
     */
    private static Point getDestination(INavigationService navigationService, IMapController mapController, ActionPoint actionPoint,
            ICreatureController creature) {
        Point start = creature.getCreatureCoordinates();
        List<Point> tiles = new ArrayList<>(actionPoint.getPoints());
        Collections.shuffle(tiles, Utils.getRandom());
        for (Point tile : tiles) {

            // No path leads into a solid tile, and a path search that fails goes
            // through every tile the creature can get to
            if (!tile.equals(start) && isSolid(mapController, tile)) {
                continue;
            }
            if (navigationService.findPath(start, tile, creature) != null) {
                return tile;
            }
        }
        return WorldUtils.vectorToPoint(actionPoint.getCenter());
    }

    private static boolean isSolid(IMapController mapController, Point p) {
        IMapTileInformation tile = mapController.getMapData().getTile(p);
        return tile != null && mapController.getTerrain(tile).getFlags().contains(Terrain.TerrainFlag.SOLID);
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        if (!executed && creature != null) {

            // Check that the objectives are still the same
            return Thing.HeroParty.Objective.SEND_TO_ACTION_POINT.equals(creature.getObjective()) && actionPoint.getId() == creature.getObjectiveTargetActionPointId();
        }
        return !executed;
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {

        // The creature waits at the point for its wait delay (seconds) before moving on
        if (executionDuration < actionPoint.getWaitDelay()) {
            return;
        }
        executed = true;

        // Assign new objective. Past the last waypoint (0) there is no action point left to go to
        creature.setObjectiveTargetActionPointId(actionPoint.getNextWaypointId());
    }

}
