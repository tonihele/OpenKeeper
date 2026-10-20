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
package toniarts.openkeeper.game.fogofwar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LineOfSightTableTest {

    @Test
    void offsetsAreSortedByAscendingDistance() {
        LineOfSightTable table = LineOfSightTable.generate(5);
        long previousDistSq = -1;
        for (LineOfSightTable.Offset o : table.getOffsets()) {
            long distSq = (long) o.dx * o.dx + (long) o.dy * o.dy;
            assertTrue(distSq >= previousDistSq, "offsets must be sorted by ascending distance");
            previousDistSq = distSq;
        }
    }

    @Test
    void angleBoundsAreWithinRange() {
        LineOfSightTable table = LineOfSightTable.generate(5);
        for (LineOfSightTable.Offset o : table.getOffsets()) {
            assertTrue(o.angleStart >= 0 && o.angleStart <= 255);
            assertTrue(o.angleEnd >= 0 && o.angleEnd <= 255);
        }
    }

    @Test
    void coversAtLeastTheRequestedDisc() {
        int radius = 5;
        LineOfSightTable table = LineOfSightTable.generate(radius);
        boolean[][] covered = new boolean[2 * radius + 1][2 * radius + 1];
        for (LineOfSightTable.Offset o : table.getOffsets()) {
            long distSq = (long) o.dx * o.dx + (long) o.dy * o.dy;
            if (distSq <= (long) radius * radius) {
                covered[o.dx + radius][o.dy + radius] = true;
            }
        }
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                if (dx * dx + dy * dy <= radius * radius) {
                    assertTrue(covered[dx + radius][dy + radius],
                            "cell (" + dx + "," + dy + ") within radius should be present in the table");
                }
            }
        }
    }

    @Test
    void generationIsDeterministic() {
        LineOfSightTable a = LineOfSightTable.generate(5);
        LineOfSightTable b = LineOfSightTable.generate(5);
        assertEquals(a.getOffsets().size(), b.getOffsets().size());
        for (int i = 0; i < a.getOffsets().size(); i++) {
            assertEquals(a.getOffsets().get(i).dx, b.getOffsets().get(i).dx);
            assertEquals(a.getOffsets().get(i).dy, b.getOffsets().get(i).dy);
            assertEquals(a.getOffsets().get(i).angleStart, b.getOffsets().get(i).angleStart);
            assertEquals(a.getOffsets().get(i).angleEnd, b.getOffsets().get(i).angleEnd);
        }
    }
}
