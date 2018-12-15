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

import java.awt.Point;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.tools.convert.map.KwdFile;

/**
 *
 * @author ArchDemon
 */
public class DoubleQuadController extends NormalRoomController {

    public DoubleQuadController(KwdFile kwdFile, RoomInstance roomInstance, IObjectsController objectsController) {
        super(kwdFile, roomInstance, objectsController);
    }

    @Override
    public boolean isTileAccessible(Integer fromX, Integer fromY, int toX, int toY) {

        // Only the surroundings are accessible
        Point roomPoint = roomInstance.worldCoordinateToLocalCoordinate(toX, toY);
        boolean N = hasSameTile(map, roomPoint.x, roomPoint.y + 1);
        boolean NE = hasSameTile(map, roomPoint.x - 1, roomPoint.y + 1);
        boolean E = hasSameTile(map, roomPoint.x - 1, roomPoint.y);
        boolean SE = hasSameTile(map, roomPoint.x - 1, roomPoint.y - 1);
        boolean S = hasSameTile(map, roomPoint.x, roomPoint.y - 1);
        boolean SW = hasSameTile(map, roomPoint.x + 1, roomPoint.y - 1);
        boolean W = hasSameTile(map, roomPoint.x + 1, roomPoint.y);
        boolean NW = hasSameTile(map, roomPoint.x + 1, roomPoint.y + 1);

        return !(N && NE && E && SE && S && SW && W && NW);
    }
}
