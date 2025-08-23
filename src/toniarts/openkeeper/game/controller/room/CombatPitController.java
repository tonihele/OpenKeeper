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

}
