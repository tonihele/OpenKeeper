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
package toniarts.openkeeper.game.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.*;

class RoomActionQueueTest {

    @Test
    void matchesNativeThreeByThreeTraceInEveryDragDirection() {
        assertTrace(10, 10, 12, 12, List.of(
                List.of(p(12,12)), List.of(p(12,11),p(11,12)),
                List.of(p(12,10),p(11,11),p(10,12)), List.of(p(11,10),p(10,11)), List.of(p(10,10))));
        assertTrace(12, 12, 10, 10, List.of(
                List.of(p(10,10)), List.of(p(10,11),p(11,10)),
                List.of(p(10,12),p(11,11),p(12,10)), List.of(p(11,12),p(12,11)), List.of(p(12,12))));
        assertTrace(10, 12, 12, 10, List.of(
                List.of(p(12,10)), List.of(p(12,11),p(11,10)),
                List.of(p(12,12),p(11,11),p(10,10)), List.of(p(11,12),p(10,11)), List.of(p(10,12))));
        assertTrace(12, 10, 10, 12, List.of(
                List.of(p(10,12)), List.of(p(10,11),p(11,12)),
                List.of(p(10,10),p(11,11),p(12,12)), List.of(p(11,10),p(12,11)), List.of(p(12,10))));
    }

    @Test
    void handlesSingleTilesLinesAndRectangles() {
        assertTrace(2, 3, 2, 3, List.of(List.of(p(2,3))));
        assertTrace(2, 1, 2, 3, List.of(List.of(p(2,3)),List.of(p(2,2)),List.of(p(2,1))));
        assertTrace(1, 2, 3, 2, List.of(List.of(p(3,2)),List.of(p(2,2)),List.of(p(1,2))));
        assertTrace(1, 1, 3, 2, List.of(List.of(p(3,2)),List.of(p(3,1),p(2,2)),
                List.of(p(2,1),p(1,2)),List.of(p(1,1))));
    }

    @Test
    void preparesOnlyOnceOnTheGameTick() {
        RoomActionQueue queue = new RoomActionQueue();
        AtomicInteger preparations = new AtomicInteger();
        List<List<Point>> trace = new ArrayList<>();
        Point start = p(1,1), end = p(2,2);
        queue.add(start, end, () -> { preparations.incrementAndGet(); return trace::add; });
        assertEquals(0, preparations.get());
        assertTrue(trace.isEmpty());
        for (int i = 0; i < 5; i++) queue.processTick();
        assertEquals(1, preparations.get());
        assertEquals(List.of(List.of(p(2,2)),List.of(p(2,1),p(1,2)),List.of(p(1,1))),trace);
    }

    @Test
    void laterRequestsStartTheirOwnSweepWithoutRestartingExistingSelections() {
        RoomActionQueue queue = new RoomActionQueue();
        List<List<Point>> first = new ArrayList<>(), second = new ArrayList<>();
        queue.add(p(0,0),p(2,0), () -> first::add);
        queue.processTick();
        queue.add(p(5,5),p(5,6), () -> second::add);
        queue.processTick();
        assertEquals(List.of(List.of(p(2,0)),List.of(p(1,0))),first);
        assertEquals(List.of(List.of(p(5,6))),second);
        queue.processTick();
        assertEquals(List.of(p(0,0)),first.getLast());
        assertEquals(List.of(p(5,5)),second.getLast());
    }

    @Test
    void rejectedRequestsAreRemovedAndClearDiscardsPendingAndActiveSelections() {
        RoomActionQueue queue = new RoomActionQueue();
        AtomicInteger rejected = new AtomicInteger();
        List<List<Point>> trace = new ArrayList<>();
        queue.add(p(0,0),p(1,1), () -> { rejected.incrementAndGet(); return null; });
        queue.add(p(0,0),p(2,2), () -> trace::add);
        queue.processTick();
        queue.add(p(5,5),p(5,5), () -> trace::add);
        queue.clear();
        queue.processTick();
        assertEquals(1,rejected.get());
        assertEquals(List.of(List.of(p(2,2))),trace);
    }

    @Test
    void skippedTilesDoNotPullLaterDiagonalsForward() {
        RoomActionQueue queue = new RoomActionQueue();
        List<List<Point>> trace = new ArrayList<>();
        queue.add(p(0, 0), p(2, 2), () -> diagonal -> trace.add(
                diagonal.stream().filter(tile -> tile.equals(p(0, 0))).toList()));
        for (int i = 0; i < 5; i++) {
            queue.processTick();
        }
        assertEquals(List.of(List.of(), List.of(), List.of(), List.of(), List.of(p(0, 0))), trace);
    }

    @Test
    void requestsSubmittedDuringAWaveWaitUntilTheNextTick() {
        RoomActionQueue queue = new RoomActionQueue();
        List<List<Point>> trace = new ArrayList<>();
        queue.add(p(0, 0), p(0, 0), () -> diagonal ->
                queue.add(p(1, 1), p(1, 1), () -> trace::add));
        queue.processTick();
        assertTrue(trace.isEmpty());
        queue.processTick();
        assertEquals(List.of(List.of(p(1, 1))), trace);
    }

    private static void assertTrace(int sx, int sy, int ex, int ey, List<List<Point>> expected) {
        RoomActionQueue queue = new RoomActionQueue();
        List<List<Point>> actual = new ArrayList<>();
        queue.add(p(sx,sy),p(ex,ey), () -> actual::add);
        for (int i = 0; i < expected.size() + 2; i++) queue.processTick();
        assertEquals(expected,actual);
    }

    private static Point p(int x, int y) { return new Point(x,y); }
}
