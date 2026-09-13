/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.game.fogofwar;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FogOfWarRulesTest {

    @Test
    void clampVisionRadiusRoundsAndClamps() {
        assertEquals(2, FogOfWarRules.clampVisionRadius(0f));
        assertEquals(2, FogOfWarRules.clampVisionRadius(1.4f));
        assertEquals(2, FogOfWarRules.clampVisionRadius(1.6f));
        assertEquals(4, FogOfWarRules.clampVisionRadius(4f));
        assertEquals(7, FogOfWarRules.clampVisionRadius(8f));
        assertEquals(7, FogOfWarRules.clampVisionRadius(100f));
    }

    @Test
    void exploreRectIsInclusiveOfBothCorners() {
        FogState state = new FogState((short) 1, 10, 10);
        FogOfWarRules.exploreRect(state, new Point(2, 2), new Point(4, 3));
        for (int x = 2; x <= 4; x++) {
            for (int y = 2; y <= 3; y++) {
                assertTrue(state.isExplored(x, y), "(" + x + "," + y + ") should be explored");
            }
        }
        assertFalse(state.isExplored(1, 2));
        assertFalse(state.isExplored(5, 2));
        assertFalse(state.isExplored(2, 4));
    }

    @Test
    void unexploreRectClearsThePreviouslyExploredRect() {
        FogState state = new FogState((short) 1, 10, 10);
        state.revealAll();
        FogOfWarRules.unexploreRect(state, new Point(0, 0), new Point(2, 2));
        for (int x = 0; x <= 2; x++) {
            for (int y = 0; y <= 2; y++) {
                assertFalse(state.isExplored(x, y));
            }
        }
        assertTrue(state.isExplored(3, 3));
    }

    @Test
    void perceiveDiscBoundaryIsStrictlyLessThan() {
        FogState state = new FogState((short) 1, 11, 11);
        FogOfWarRules.perceiveDisc(state, 5, 5, 3f);
        assertFalse(state.isPerceived(8, 5), "tile at exactly the range should not be perceived (strictly within)");
        assertTrue(state.isPerceived(7, 5), "tile within the range should be perceived");
    }

    @Test
    void exploreVisionRingOpenGridCoversTheDisc() {
        FogState state = new FogState((short) 1, 21, 21);
        LineOfSightTable table = LineOfSightTable.generate(7);
        FogOfWarRules.exploreVisionRing(state, (x, y) -> false, table, 10, 10, 5);

        assertTrue(state.isExplored(10, 10));
        assertTrue(state.isExplored(15, 10)); // directly east, 5 tiles
        assertFalse(state.isExplored(17, 10)); // beyond radius
    }

    @Test
    void exploreVisionRingWallBlocksTilesBehindIt() {
        FogState state = new FogState((short) 1, 21, 21);
        LineOfSightTable table = LineOfSightTable.generate(7);

        // A wall two tiles east of the centre; nothing further east (in the
        // narrow angular slice directly behind it) should be explored
        FogOfWarRules.exploreVisionRing(state, (x, y) -> x == 12 && y == 10, table, 10, 10, 5);

        assertTrue(state.isExplored(12, 10), "the wall tile itself is seen");
        assertFalse(state.isExplored(15, 10), "directly behind the wall should stay hidden");
        // A tile off to the side, not shadowed by the wall, is still explored
        assertTrue(state.isExplored(10, 15));
    }

    @Test
    void exploreVisionRingFullyEnclosedRoomStaysContained() {
        FogState state = new FogState((short) 1, 21, 21);
        LineOfSightTable table = LineOfSightTable.generate(7);

        int cx = 10;
        int cy = 10;
        FogOfWarRules.exploreVisionRing(state, (x, y) -> Math.abs(x - cx) > 2 || Math.abs(y - cy) > 2,
                table, cx, cy, 7);

        // Inside the room is explored
        assertTrue(state.isExplored(cx + 2, cy));
        // Outside the enclosing walls is not
        assertFalse(state.isExplored(cx + 4, cy));
    }

    @Test
    void isVisibleFoldsExploredAndPerceivedWithTerrainFlag() {
        FogState state = new FogState((short) 1, 5, 5);
        assertFalse(FogOfWarRules.isVisible(state, 0, 0, false));
        assertFalse(FogOfWarRules.isVisible(state, 0, 0, true));

        FogOfWarRules.perceive(state, 0, 0);
        assertFalse(FogOfWarRules.isVisible(state, 0, 0, false));
        assertTrue(FogOfWarRules.isVisible(state, 0, 0, true));

        FogOfWarRules.explore(state, 0, 0);
        assertTrue(FogOfWarRules.isVisible(state, 0, 0, false));
        assertTrue(FogOfWarRules.isVisible(state, 0, 0, true));
    }
}
