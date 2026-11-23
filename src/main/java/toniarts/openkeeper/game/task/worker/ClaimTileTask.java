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

import toniarts.openkeeper.game.event.GameEventType;
import com.jme3.math.Vector2f;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractTileTask;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Claim a tile task, for workers
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ClaimTileTask extends AbstractTileTask {

    private final boolean leftByDigging;

    public ClaimTileTask(final INavigationService navigationService, final IMapController mapController, Point p, short playerId) {
        this(navigationService, mapController, p, playerId, false);
    }

    /**
     * Claim a tile
     *
     * @param navigationService the navigation service
     * @param mapController the map controller
     * @param p the tile
     * @param playerId the claiming player
     * @param leftByDigging whether the tile is the floor that digging by the
     * player's workers has just left
     */
    public ClaimTileTask(final INavigationService navigationService, final IMapController mapController, Point p, short playerId, boolean leftByDigging) {
        super(navigationService, mapController, p, playerId);
        this.leftByDigging = leftByDigging;
    }

    /**
     * The floor that digging has left is claimed by a worker that walks over
     * it, but not while any of the eight tiles around it is still tagged for
     * digging. Tiles tagged for mining gold or gems don't hold it up (DKII.EXE
     * 0x469600, job 0xE)
     *
     * @return {@code true} if this is such floor, with no digging left around
     * it
     */
    public boolean isDugFloorToClaim() {
        if (!leftByDigging) {
            return false;
        }
        for (Point p : WorldUtils.getSurroundingTiles(mapController.getMapData(), getTaskLocation(), true)) {
            IMapTileInformation tile = mapController.getMapData().getTile(p);
            if (tile.isSelected(playerId) && tile.getGold() == 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(getTaskLocation()); // FIXME 0.5f not needed?
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        return mapController.isClaimableTile(getTaskLocation(), playerId);
    }

    /**
     * A worker nearer to the tile than the one assigned to it takes the claim
     * over, unless that one is claiming it already. So after a fight the
     * workers go straight back to the tile (0x46AB50)
     */
    @Override
    public boolean makeRoomFor(ICreatureController creature) {
        return takeOverFromFartherWorker(creature);
    }

    @Override
    public String toString() {
        return "Claim tile at " + getTaskLocation();
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {

        // TODO: is this a general case or even smart to do this like this...?
        if (executionDuration - getExecutionDuration(creature) >= 1.0f) {
            setExecutionDuration(creature, executionDuration - getExecutionDuration(creature));

            mapController.applyClaimTile(getTaskLocation(), playerId, creature);

            // The work is done, let it be heard
            if (!isValid(creature)) {
                creature.fireEvent(GameEventType.CREATURE_CLAIM_TILE, getTaskLocation());
            }
        }
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.CLAIM_TILE;
    }

}
