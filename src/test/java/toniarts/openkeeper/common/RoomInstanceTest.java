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
package toniarts.openkeeper.common;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The reference tile DK II keeps for each room.
 */
class RoomInstanceTest {

    private static final short TREASURY = 1;
    private static final short PORTAL = 3;
    private static final short DUNGEON_HEART = 5;
    private static final short HERO_GATE = 18;

    @Test
    void aDungeonHeartHasTheTileOnePastItsMiddle() {
        assertEquals(new Point(13, 13), createRoom(DUNGEON_HEART, 10, 10, 14, 14).getReferenceTile());
    }

    @Test
    void aPortalHasItsMiddleAndAHeroGateItsFirstTile() {
        assertEquals(new Point(11, 6), createRoom(PORTAL, 10, 5, 12, 7).getReferenceTile());
        assertEquals(new Point(10, 5), createRoom(HERO_GATE, 10, 5, 12, 7).getReferenceTile());
    }

    @Test
    void anyOtherRoomHasTheAverageOfItsTilesRoundedDown() {
        assertEquals(new Point(11, 5), createRoom(TREASURY, 10, 5, 13, 6).getReferenceTile());
    }

    @Test
    void aRoomThatDoesNotHaveItsAverageTileTakesTheFirstOfItsTilesOnASpiralRoundIt() {

        // A U open to the north: the average (11, 6) is in the gap, the spiral's first step east finds a tile
        RoomInstance room = createRoom(TREASURY, 10, 7, 12, 7);
        room.addCoordinate(new Point(10, 5));
        room.addCoordinate(new Point(10, 6));
        room.addCoordinate(new Point(12, 5));
        room.addCoordinate(new Point(12, 6));

        assertEquals(new Point(12, 6), room.getReferenceTile());

        // Two corners round (11, 6): the spiral passes east, south east and south before the south west one
        room = createRoom(TREASURY, 10, 7, 10, 7);
        room.addCoordinate(new Point(12, 5));

        assertEquals(new Point(10, 7), room.getReferenceTile());
    }

    private static RoomInstance createRoom(short roomId, int x1, int y1, int x2, int y2) {
        RoomInstance room = new RoomInstance(RoomFixtures.room(roomId, 0));
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                room.addCoordinate(new Point(x, y));
            }
        }

        return room;
    }
}
