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

import com.jme3.math.Vector2f;
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

    /**
     * The four side neighbours a creature milling about at the point steps to
     */
    private static final int[][] SIDE_NEIGHBOURS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    protected final ActionPoint actionPoint;
    private final int ticksPerSecond;
    private boolean executed = false;
    private Point spot;
    private Vector2f spotPosition;

    /**
     * Sends the creature to the action point
     *
     * @param navigationService the navigation
     * @param mapController the map
     * @param actionPoint the action point
     * @param creature the creature sent
     * @param ticksPerSecond the level's ticks per second, the point's wait
     * delay is in game ticks
     */
    public SendToActionPoint(final INavigationService navigationService, final IMapController mapController, ActionPoint actionPoint, ICreatureController creature,
            int ticksPerSecond) {
        super(navigationService, mapController, getDestination(navigationService, mapController, actionPoint, creature), creature.getOwnerId());

        this.actionPoint = actionPoint;
        this.ticksPerSecond = ticksPerSecond;
        spot = getTaskLocation();
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
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(spot);
    }

    @Override
    public Vector2f getWorkPosition(ICreatureController creature) {
        return spotPosition != null ? spotPosition : getTarget(creature);
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {

        // The creature mills about at the point for its wait delay before moving on. The delay is in game ticks,
        // the original adds it to the game tick (0x472150)
        if (executionDuration < (float) actionPoint.getWaitDelay() / ticksPerSecond) {
            millAbout(creature);
            return;
        }
        executed = true;

        // Assign new objective. Past the last waypoint (0) there is no action point left to go to
        creature.setObjectiveTargetActionPointId(actionPoint.getNextWaypointId());
    }

    /**
     * Steps to one of the four side neighbours, trying them in turn from a
     * random one, and takes the first the creature can get to. With none it
     * stays (0x4721C0). It stops at one of nine places in the tile, a third of
     * a tile apart (DKII.EXE table 0x698EC8)
     */
    private void millAbout(ICreatureController creature) {
        int start = Utils.getRandom().nextInt(SIDE_NEIGHBOURS.length);
        for (int i = 0; i < SIDE_NEIGHBOURS.length; i++) {
            int[] offset = SIDE_NEIGHBOURS[(start + i) % SIDE_NEIGHBOURS.length];
            Point neighbour = new Point(spot.x + offset[0], spot.y + offset[1]);
            if (mapController.getMapData().getTile(neighbour) != null && !isSolid(mapController, neighbour)
                    && navigationService.findPath(spot, neighbour, creature) != null) {
                spot = neighbour;
                spotPosition = WorldUtils.pointToVector2f(neighbour).addLocal((Utils.getRandom().nextInt(3) - 1) * WorldUtils.TILE_WIDTH / 3,
                        (Utils.getRandom().nextInt(3) - 1) * WorldUtils.TILE_WIDTH / 3);
                return;
            }
        }
    }

}
