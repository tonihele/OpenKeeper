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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.storage.RoomPrisonerControl;
import toniarts.openkeeper.tools.convert.map.KwdFile;

/**
 * TODO: not completed
 *
 * @author ArchDemon
 */
public class PrisonController extends DoubleQuadController {

    private static final short OBJECT_DOOR_ID = 109;
    private static final short OBJECT_DOORBAR_ID = 116;

    private Point door;
    private Set<Point> insideCoordinates;

    public PrisonController(KwdFile kwdFile, RoomInstance roomInstance, IObjectsController objectsController) {
        super(kwdFile, roomInstance, objectsController);

        addObjectControl(new RoomPrisonerControl(this, objectsController) {
            @Override
            protected Collection<Point> getCoordinates() {

                // Only the innards of prison can hold objects
                return insideCoordinates;
            }

            @Override
            protected int getNumberOfAccessibleTiles() {
                return getCoordinates().size();
            }

        });
    }

    @Override
    protected void constructObjects() {
        super.constructObjects();

        door = null;
        for (Point p : roomInstance.getCoordinates()) {

            // Figure out which peace by seeing the neighbours
            boolean N = roomInstance.hasCoordinate(new Point(p.x, p.y - 1));
            boolean NE = roomInstance.hasCoordinate(new Point(p.x + 1, p.y - 1));
            boolean E = roomInstance.hasCoordinate(new Point(p.x + 1, p.y));
            boolean SE = roomInstance.hasCoordinate(new Point(p.x + 1, p.y + 1));
            boolean S = roomInstance.hasCoordinate(new Point(p.x, p.y + 1));
            boolean SW = roomInstance.hasCoordinate(new Point(p.x - 1, p.y + 1));
            boolean W = roomInstance.hasCoordinate(new Point(p.x - 1, p.y));
            boolean NW = roomInstance.hasCoordinate(new Point(p.x - 1, p.y - 1));

            if (door == null && !N && !NE && E && SE && S && SW && W && !NW) {
                objectsController.loadObject(OBJECT_DOOR_ID, (short) 0, p.x, p.y);
                objectsController.loadObject(OBJECT_DOORBAR_ID, (short) 0, p.x, p.y);

                door = p;
                break;
            }
        }
    }

    @Override
    public boolean isTileAccessible(Integer fromX, Integer fromY, int toX, int toY) {

        // Prison is all accessible, but you do have to use the door!
        if (door != null && fromX != null && fromY != null) {

            // Path finding
            Point from = new Point(fromX, fromY);
            Point to = new Point(toX, toY);
            if (insideCoordinates.contains(to) && insideCoordinates.contains(from)) {
                return true; // Inside the prison == free movement
            } else if (insideCoordinates.contains(from) && to.equals(door)) {
                return true; // From inside also through the door
            } else if (insideCoordinates.contains(to) && from.equals(door)) {
                return true; // Path finding, from outside
            } else if (!insideCoordinates.contains(to) && !insideCoordinates.contains(from)) {
                return true; // Outside the prison == free movement
            }
            return false;
        }
        if (fromX == null && fromY == null && door != null) {
            return true; // We have a door, the tile is accessible
        }
        return super.isTileAccessible(fromX, fromY, toX, toY);
    }

    @Override
    protected void setupCoordinates() {
        super.setupCoordinates();

        insideCoordinates = getInsideCoordinates();
    }


    private Set<Point> getInsideCoordinates() {
        Set<Point> coordinates = new HashSet<>();
        for (int x = 1; x < map.length - 1; x++) {
            for (int y = 1; y < map[x].length - 1; y++) {
                boolean N = hasSameTile(map, x, y + 1);
                boolean NE = hasSameTile(map, x - 1, y + 1);
                boolean E = hasSameTile(map, x - 1, y);
                boolean SE = hasSameTile(map, x - 1, y - 1);
                boolean S = hasSameTile(map, x, y - 1);
                boolean SW = hasSameTile(map, x + 1, y - 1);
                boolean W = hasSameTile(map, x + 1, y);
                boolean NW = hasSameTile(map, x + 1, y + 1);
                if (N && NE && E && SE && S && SW && W && NW) {
                    coordinates.add(new Point(roomInstance.getMatrixStartPoint().x + x, roomInstance.getMatrixStartPoint().y + y));
                }
            }
        }
        return coordinates;
    }

}
