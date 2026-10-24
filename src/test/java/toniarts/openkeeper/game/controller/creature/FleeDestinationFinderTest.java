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
package toniarts.openkeeper.game.controller.creature;

import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import toniarts.openkeeper.utils.Point;

/**
 * Where fleeing creatures run to
 */
public class FleeDestinationFinderTest {

    private static final int SIZE = 40;

    private static FleeDestinationFinder finder(FleeDestinationFinder.TileCheck check, long seed) {
        return new FleeDestinationFinder(SIZE, SIZE, check, new Random(seed));
    }

    @Test
    void runsAwayFromTheThreatAlongTheLineThroughTheCreature() {
        for (long seed = 0; seed < 50; seed++) {
            Point dest = finder((x, y) -> true, seed).findAwayFrom(new Point(22, 20), new Point(20, 20), 6).orElseThrow();

            // Aimed at (26, 20), the area is 24..28 by 18..22
            assertTrue(dest.x >= 24 && dest.x <= 28, "x " + dest);
            assertTrue(dest.y >= 18 && dest.y <= 22, "y " + dest);
        }
    }

    @Test
    void runsAwayInTheOtherDirectionsToo() {
        Point west = finder((x, y) -> true, 1).findAwayFrom(new Point(18, 20), new Point(20, 20), 6).orElseThrow();
        assertTrue(west.x <= 16 && west.x >= 12, "west " + west);

        Point south = finder((x, y) -> true, 1).findAwayFrom(new Point(20, 22), new Point(20, 20), 6).orElseThrow();
        assertTrue(south.y >= 24 && south.y <= 28, "south " + south);

        Point diagonal = finder((x, y) -> true, 1).findAwayFrom(new Point(23, 23), new Point(20, 20), 8).orElseThrow();
        assertTrue(diagonal.x > 23 && diagonal.y > 23, "diagonal " + diagonal);
    }

    @Test
    void staysInsideTheMapAtTheEdge() {
        for (long seed = 0; seed < 50; seed++) {
            Point dest = finder((x, y) -> true, seed).findAwayFrom(new Point(37, 20), new Point(35, 20), 8).orElseThrow();
            assertTrue(dest.x <= SIZE - 1 && dest.x >= 0 && dest.y >= 0 && dest.y < SIZE, "edge " + dest);
        }
    }

    @Test
    void skipsTilesTheCreatureCantReach() {
        // Only one tile of the area, (26, 20), is reachable
        FleeDestinationFinder.TileCheck check = (x, y) -> x == 26 && y == 20;
        for (long seed = 0; seed < 20; seed++) {
            assertEquals(Optional.of(new Point(26, 20)), finder(check, seed).findAwayFrom(new Point(22, 20), new Point(20, 20), 6));
        }
    }

    @Test
    void withNothingReachableInTheAreaRunsAwayFromItsFarCornerInstead() {
        // The area is walled off, the creature can only walk on the west side
        FleeDestinationFinder.TileCheck check = (x, y) -> x <= 22;
        for (long seed = 0; seed < 50; seed++) {
            Point dest = finder(check, seed).findAwayFrom(new Point(22, 20), new Point(20, 20), 6).orElseThrow();
            assertTrue(dest.x <= 22, "reachable " + dest);
            assertTrue(dest.y <= 20, "away from the far corner " + dest);
        }
    }

    @Test
    void givesUpWhenNothingIsReachable() {
        FleeDestinationFinder finder = finder((x, y) -> false, 3);

        assertTrue(finder.findAwayFrom(new Point(22, 20), new Point(20, 20), 6).isEmpty());
        assertTrue(finder.findAround(new Point(22, 20)).isEmpty());
    }

    @Test
    void findsATileWithinTwoTilesOfTheCreature() {
        for (long seed = 0; seed < 50; seed++) {
            Point dest = finder((x, y) -> true, seed).findAround(new Point(10, 10)).orElseThrow();
            assertTrue(Math.abs(dest.x - 10) <= 2 && Math.abs(dest.y - 10) <= 2, "around " + dest);
        }
    }
}
