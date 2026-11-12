/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.game.controller.room;

import com.jme3.math.FastMath;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.tools.convert.map.IKwdFile;

/**
 * Manages combat pit
 *
 * @author ArchDemon
 */
public final class CombatPitController extends DoubleQuadController {

    private Point door;

    public CombatPitController(EntityId entityId, EntityData entityData, IKwdFile kwdFile,
            RoomInstance roomInstance, IObjectsController objectsController) {
        super(entityId, entityData, kwdFile, roomInstance, objectsController);
    }

    @Override
    public void construct() {
        super.construct();

        // Store the door position for spawning the creatures in
        door = null;
        for (Point p : roomInstance.getCoordinates()) {
            if (door == null && insideCoordinates.contains(new Point(p.x, p.y + 1))) {
                door = p;
                break;
            }
        }
    }

    @Override
    public boolean isTileAccessible(Integer fromX, Integer fromY, int toX, int toY) {

        // The pit's door is the way in and out, for the imps that carry the knocked out fighters off too
        if (door != null && fromX != null && fromY != null) {
            Point from = new Point(fromX, fromY);
            Point to = new Point(toX, toY);
            if ((insideCoordinates.contains(from) && to.equals(door)) || (insideCoordinates.contains(to) && from.equals(door))) {
                return true;
            }
        }
        return super.isTileAccessible(fromX, fromY, toX, toY);
    }

    /**
     * Is the tile in the pit itself, where the creatures fight, rather than on
     * its edge
     *
     * @param tile the tile
     * @return true if the creatures fight on the tile
     */
    public boolean isFightingTile(Point tile) {
        return insideCoordinates.contains(tile);
    }

    /**
     * Picks a tile in the pit itself, where a fighter without an opponent
     * walks to wait for one
     *
     * @return a random tile the creatures fight on
     */
    public Point getRandomFightingTile() {
        return insideCoordinates.stream().skip(FastMath.nextRandomInt(0, insideCoordinates.size() - 1)).findFirst().orElseThrow();
    }

}
