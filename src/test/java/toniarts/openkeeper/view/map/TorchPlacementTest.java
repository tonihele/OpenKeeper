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
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.map.WallSection.WallDirection;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TorchPlacementTest {

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
    void torchLightHugsTheWallFaceInsideTheFloorTile() {
        // In the floor tile's own space (a tile (x, y) is centred on world (x, y))
        Point tile = new Point(10, 20);
        assertEquals(new Vector3f(10f, 1.5f, 19.625f), TorchPlacement.getTorchLightPosition(tile, WallDirection.NORTH, 1.5f));
        assertEquals(new Vector3f(10.375f, 1.5f, 20f), TorchPlacement.getTorchLightPosition(tile, WallDirection.EAST, 1.5f));
        assertEquals(new Vector3f(10f, 1.5f, 20.375f), TorchPlacement.getTorchLightPosition(tile, WallDirection.SOUTH, 1.5f));
        assertEquals(new Vector3f(9.625f, 1.5f, 20f), TorchPlacement.getTorchLightPosition(tile, WallDirection.WEST, 1.5f));
    }
}
