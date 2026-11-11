/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.map;

import com.jme3.math.FastMath;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.map.RoomPillars;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.utils.Point;

class PillarPlacementTest {

    private static final Set<Point> FOUR_CORNERS = Set.of(
            new Point(10, 20), new Point(14, 20), new Point(10, 24), new Point(14, 24));

    @Test
    void nativeRoomPillarObjectsHaveOneSharedDefinition() {
        short[] rooms = {1, 4, 10, 12, 13, 14, 15, 16, 26};
        short[] objects = {76, 78, 80, 82, 111, 84, 85, 79, 141};
        for (int i = 0; i < rooms.length; i++) {
            assertEquals(objects[i], RoomPillars.getObjectId(rooms[i]), "room id " + rooms[i]);
        }
        assertNull(RoomPillars.getObjectId((short) 2), "Lair has no active native pillar");
        assertNull(RoomPillars.getObjectId((short) 11), "Prison has no active native pillar");
    }

    @Test
    void interiorPillarRoomsUseTheSameCornerLayout() {
        for (short roomId : new short[]{1, 4, 12, 14, 15, 16, 26}) {
            Set<Point> actual = new HashSet<>();
            for (RoomPillars.Placement placement : RoomPillars.getPlacements(rectangle(roomId, 5, 5))) {
                actual.add(placement.tile());
            }
            assertEquals(FOUR_CORNERS, actual, "room id " + roomId);
        }
    }

    @Test
    void interiorPillarsRequireACompleteThreeByThreeBlock() {
        RoomInstance room = new RoomInstance(RoomFixtures.room((short) 4, 0));
        for (int y = 20; y < 23; y++) {
            for (int x = 10; x < 13; x++) {
                if (x != 11 || y != 21) {
                    room.addCoordinate(new Point(x, y));
                }
            }
        }

        assertTrue(RoomPillars.getPlacements(room).isEmpty(),
                "a hole in the footprint beside the candidate corner prevents placement");
        assertTrue(RoomPillars.getPlacements(rectangle((short) 4, 2, 2)).isEmpty(),
                "a two-by-two room has no complete pillar interior");
    }

    @Test
    void workshopUsesItsExposedCornerRuleOnSmallAndIrregularRooms() {
        List<RoomPillars.Placement> small = RoomPillars.getPlacements(rectangle((short) 10, 2, 2));
        assertEquals(4, small.size());
        assertEquals(Set.of(new Point(10, 20), new Point(11, 20), new Point(10, 21), new Point(11, 21)),
                small.stream().map(RoomPillars.Placement::tile).collect(java.util.stream.Collectors.toSet()));

        List<RoomPillars.Placement> isolated = RoomPillars.getPlacements(rectangle((short) 10, 1, 1));
        assertEquals(4, isolated.size(), "each exposed corner retains its own workshop prop");
        assertEquals(Set.of(new Point(10, 20)),
                isolated.stream().map(RoomPillars.Placement::tile).collect(java.util.stream.Collectors.toSet()));
        assertEquals(Set.of(-FastMath.HALF_PI, FastMath.PI, FastMath.HALF_PI, 0f),
                isolated.stream().map(RoomPillars.Placement::rotation).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void templeCandlesticksAndInactiveRoomsDoNotReservePillarCeilingTiles() {
        assertTrue(RoomPillars.getPlacements(rectangle((short) 13, 5, 5)).isEmpty());
        assertTrue(RoomPillars.getPillarTiles(rectangle((short) 2, 5, 5)).isEmpty());
        assertTrue(RoomPillars.getPillarTiles(rectangle((short) 11, 5, 5)).isEmpty());
    }

    @Test
    void eachInteriorCornerKeepsTheNativeFacingRotation() {
        List<RoomPillars.Placement> placements = RoomPillars.getPlacements(rectangle((short) 4, 5, 5));
        Map<Point, Float> expected = Map.of(
                new Point(10, 20), 0f,
                new Point(14, 20), -FastMath.HALF_PI,
                new Point(10, 24), FastMath.HALF_PI,
                new Point(14, 24), FastMath.PI);
        for (RoomPillars.Placement placement : placements) {
            assertEquals(expected.get(placement.tile()), placement.rotation(), placement.tile().toString());
        }
    }

    private static RoomInstance rectangle(short roomId, int width, int height) {
        RoomInstance room = new RoomInstance(RoomFixtures.room(roomId, 0));
        for (int y = 20; y < 20 + height; y++) {
            for (int x = 10; x < 10 + width; x++) {
                room.addCoordinate(new Point(x, y));
            }
        }
        return room;
    }
}
