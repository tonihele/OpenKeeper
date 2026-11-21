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
import java.util.random.RandomGenerator;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractTask;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Exploring, the job of the Firefly and the Rogue. Like in DK II the creature
 * walks to a random tile anywhere on the map that nobody owns and that it can
 * walk to. It tries up to 20 random tiles and gives up for now when none fits
 * (DKII.EXE 0x45A290).
 */
public final class Explore extends AbstractTask {

    private static final int TRIES = 20;

    private Point destination;
    private boolean arrived = false;

    public Explore(final INavigationService navigationService, final IMapController mapController) {
        super(navigationService, mapController);
    }

    /**
     * Picks the tile to explore
     *
     * @param creature the explorer
     * @param random picks the tiles to try
     * @return true if a tile to walk to was found
     */
    public boolean chooseDestination(ICreatureController creature, RandomGenerator random) {
        IMapDataInformation<? extends IMapTileInformation> mapData = mapController.getMapData();
        for (int i = 0; i < TRIES; i++) {
            Point tile = new Point(random.nextInt(mapData.getWidth()), random.nextInt(mapData.getHeight()));
            IMapTileInformation mapTile = mapData.getTile(tile);
            if (mapTile != null && !mapController.isSolid(tile) && mapTile.getOwnerId() == Player.NEUTRAL_PLAYER_ID
                    && isReachable(creature, WorldUtils.pointToVector2f(tile))) {
                destination = tile;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        return destination != null && !arrived;
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(destination);
    }

    @Override
    public Point getTaskLocation() {
        return destination;
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        arrived = true;
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.EXPLORE;
    }

}
