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
package toniarts.openkeeper.view.map;

import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.map.WallSection.WallDirection;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Which tiles of a room get a pillar - a room prop that, like a solid tile,
 * has its own little rock ceiling on top and should push the cave ceiling up
 * and away from it rather than let the ceiling mesh cut through it.
 */
final class PillarPlacement {

    /**
     * Rooms whose corner prop is a real pillar (see
     * {@code AbstractRoomController.getPillarObject}), placed on a corner
     * whose full 3x3 interior block also belongs to the room. Work Shop (10)
     * and Temple (13) are deliberately excluded - their corner prop doesn't
     * carry the {@code PILLAR} object flag.
     */
    private static final Set<Short> PILLAR_ROOM_IDS = Set.of(
            (short) 1,  // Treasury
            (short) 2,  // Lair
            (short) 4,  // Hatchery
            (short) 12, // Torture
            (short) 14, // Graveyard
            (short) 15, // Casino
            (short) 16, // Pit
            (short) 26  // Crypt
    );

    private PillarPlacement() {
        // Nope
    }

    /**
     * @param roomInstance the room
     * @return the tiles of this room instance that carry a pillar, empty if
     * this room type has none
     */
    static Set<Point> getPillarTiles(RoomInstance roomInstance) {
        if (!PILLAR_ROOM_IDS.contains(roomInstance.getRoom().getRoomId())) {
            return Set.of();
        }

        boolean[][] map = roomInstance.getCoordinatesAsMatrix();
        Point start = roomInstance.getMatrixStartPoint();
        Set<Point> pillarTiles = new HashSet<>();

        for (Point p : roomInstance.getCoordinates()) {
            Set<WallDirection> freeDirections = EnumSet.noneOf(WallDirection.class);
            if (!hasSameTile(map, p.x - start.x, p.y - start.y - 1)) {
                freeDirections.add(WallDirection.NORTH);
            }
            if (!hasSameTile(map, p.x - start.x, p.y - start.y + 1)) {
                freeDirections.add(WallDirection.SOUTH);
            }
            if (!hasSameTile(map, p.x - start.x + 1, p.y - start.y)) {
                freeDirections.add(WallDirection.EAST);
            }
            if (!hasSameTile(map, p.x - start.x - 1, p.y - start.y)) {
                freeDirections.add(WallDirection.WEST);
            }

            // Only fires for an exact corner: 2 free, perpendicular directions
            boolean exactCorner = freeDirections.size() == 2
                    && !(freeDirections.contains(WallDirection.NORTH) && freeDirections.contains(WallDirection.SOUTH))
                    && !(freeDirections.contains(WallDirection.WEST) && freeDirections.contains(WallDirection.EAST));
            if (exactCorner && hasInteriorBlock(map, start, p, freeDirections)) {
                pillarTiles.add(p);
            }
        }

        return pillarTiles;
    }

    private static boolean hasInteriorBlock(boolean[][] map, Point start, Point p, Set<WallDirection> freeDirections) {
        boolean goWest = freeDirections.contains(WallDirection.EAST);
        boolean goNorth = freeDirections.contains(WallDirection.SOUTH);
        for (int y = 0; y < 3; y++) {
            int yPoint = goNorth ? -y : y;
            for (int x = 0; x < 3; x++) {
                int xPoint = goWest ? -x : x;
                if (!hasSameTile(map, p.x - start.x + xPoint, p.y - start.y + yPoint)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean hasSameTile(boolean[][] map, int x, int y) {
        if (x < 0 || x >= map.length || y < 0 || y >= map[x].length) {
            return false;
        }
        return map[x][y];
    }
}
