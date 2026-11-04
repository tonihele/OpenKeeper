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
package toniarts.openkeeper.view.map;

import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.map.WallSection.WallDirection;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TorchPlacementTest {

    private static final short LIBRARY_ID = 6;
    private static final short TORTURE_CHAMBER_ID = 12;

    @Test
    void evenXAndOddYDoNotAllowATorch() {
        assertEquals(List.of(), TorchPlacement.getTorchDirections(2, 3));
    }

    @Test
    void oddXAndOddYAllowNorthAndSouthWalls() {
        assertEquals(List.of(WallDirection.NORTH, WallDirection.SOUTH),
                TorchPlacement.getTorchDirections(3, 3));
    }

    @Test
    void evenXAndEvenYAllowWestAndEastWalls() {
        assertEquals(List.of(WallDirection.WEST, WallDirection.EAST),
                TorchPlacement.getTorchDirections(2, 4));
    }

    @Test
    void oddXAndEvenYAllowAllWallsInPriorityOrder() {
        assertEquals(List.of(WallDirection.NORTH, WallDirection.WEST, WallDirection.SOUTH, WallDirection.EAST),
                TorchPlacement.getTorchDirections(3, 4));
    }

    @Test
    void tortureChamberTorchesGoBetweenTheWallFurniture() {

        // A 5x5 room from (1, 1): the furniture is on x 2 and 4 of the north and south walls,
        // and on y 2 and 4 of the west and east walls
        RoomInstance tortureChamber = roomInstance(TORTURE_CHAMBER_ID, 1, 1, 5, 5);

        // Not on the west wall beside the furniture at (1, 2), where other rooms have one
        assertEquals(List.of(WallDirection.NORTH, WallDirection.SOUTH), TorchPlacement.getTorchDirections(1, 2, tortureChamber));
        assertEquals(List.of(WallDirection.NORTH, WallDirection.WEST, WallDirection.SOUTH, WallDirection.EAST),
                TorchPlacement.getTorchDirections(1, 3, tortureChamber));

        // Not on the north wall beside the furniture at (2, 1), but on the west and east walls
        assertEquals(List.of(WallDirection.WEST, WallDirection.EAST), TorchPlacement.getTorchDirections(2, 1, tortureChamber));
    }

    @Test
    void tortureChamberHasNoTorchesOnWallsWithoutFurniture() {

        // One row, the west and east walls are a tile long and get no furniture
        RoomInstance tortureChamber = roomInstance(TORTURE_CHAMBER_ID, 1, 2, 5, 1);

        assertEquals(List.of(WallDirection.NORTH, WallDirection.SOUTH), TorchPlacement.getTorchDirections(1, 2, tortureChamber));
    }

    @Test
    void torchSoundsAreLookedForWhereTheTortureChamberHasItsTorches() {
        RoomInstance tortureChamber = roomInstance(TORTURE_CHAMBER_ID, 1, 1, 5, 5);

        // Not the west wall beside the furniture at (1, 2), only the north and south walls
        assertEquals(List.of(new Point(1, 1), new Point(1, 3)), TorchPlacement.getTorchWallCandidates(1, 2, tortureChamber));
        assertEquals(List.of(new Point(1, 1), new Point(0, 2), new Point(1, 3), new Point(2, 2)),
                TorchPlacement.getTorchWallCandidates(1, 2, null));
    }

    @Test
    void otherRoomsKeepTheirTorches() {
        RoomInstance library = roomInstance(LIBRARY_ID, 1, 1, 5, 5);

        assertEquals(TorchPlacement.getTorchDirections(1, 2), TorchPlacement.getTorchDirections(1, 2, library));
        assertEquals(TorchPlacement.getTorchDirections(2, 1), TorchPlacement.getTorchDirections(2, 1, library));
    }

    /**
     * A rectangular room with a wall object, special walls and walls all round
     */
    private static RoomInstance roomInstance(short roomId, int x, int y, int width, int height) {
        RoomInstance roomInstance = new RoomInstance(RoomFixtures.room(roomId,
                List.of((short) 0, (short) 0, (short) 0, (short) 8, (short) 0, (short) 0, (short) 0, (short) 0),
                Room.RoomFlag.HAS_WALLS, Room.RoomFlag.SPECIAL_WALLS));
        List<Point> northWall = new ArrayList<>();
        List<Point> southWall = new ArrayList<>();
        List<Point> westWall = new ArrayList<>();
        List<Point> eastWall = new ArrayList<>();
        for (int i = 0; i < width; i++) {
            northWall.add(new Point(x + i, y));
            southWall.add(new Point(x + i, y + height - 1));
            for (int j = 0; j < height; j++) {
                roomInstance.addCoordinate(new Point(x + i, y + j));
            }
        }
        for (int j = 0; j < height; j++) {
            westWall.add(new Point(x, y + j));
            eastWall.add(new Point(x + width - 1, y + j));
        }

        // A section is given by the way its wall faces
        roomInstance.setWallSections(List.of(new WallSection(WallDirection.SOUTH, northWall),
                new WallSection(WallDirection.NORTH, southWall), new WallSection(WallDirection.EAST, westWall),
                new WallSection(WallDirection.WEST, eastWall)));
        return roomInstance;
    }

    @Test
    void torchLightHugsTheWallFaceInsideTheFloorTile() {
        // In the floor tile's own space (a tile (x, y) is centred on world (x, y))
        Point tile = new Point(10, 20);
        assertEquals(new Vector3f(10f, 1.5f, 19.625f), TorchPlacement.getTorchLightPosition(tile, WallDirection.NORTH, 1.5f));
        assertEquals(new Vector3f(10.375f, 1.5f, 20f), TorchPlacement.getTorchLightPosition(tile, WallDirection.EAST, 1.5f));
        assertEquals(new Vector3f(10f, 1.5f, 20.375f), TorchPlacement.getTorchLightPosition(tile, WallDirection.SOUTH, 1.5f));
        assertEquals(new Vector3f(9.625f, 1.5f, 20f), TorchPlacement.getTorchLightPosition(tile, WallDirection.WEST, 1.5f));
    }
}
