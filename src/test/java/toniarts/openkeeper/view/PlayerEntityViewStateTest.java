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
package toniarts.openkeeper.view;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerEntityViewStateTest {

    @Test
    void coalescesFogChangesReceivedFromConcurrentThreads() throws InterruptedException {
        PlayerEntityViewState.PendingDirtyTileUpdates updates = new PlayerEntityViewState.PendingDirtyTileUpdates();
        CountDownLatch start = new CountDownLatch(1);
        Thread first = new Thread(() -> enqueueTiles(updates, start, 0), "fog-update-1");
        Thread second = new Thread(() -> enqueueTiles(updates, start, 1), "fog-update-2");

        first.start();
        second.start();
        start.countDown();
        first.join();
        second.join();

        Set<Point> expected = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            expected.add(new Point(i, 0));
            expected.add(new Point(i, 1));
        }
        assertEquals(expected, updates.drain());
        assertTrue(updates.drain().isEmpty());
    }

    @Test
    void queuedFogChangesAreNotAffectedByCallerArrayReuse() {
        PlayerEntityViewState.PendingDirtyTileUpdates updates = new PlayerEntityViewState.PendingDirtyTileUpdates();
        Point[] batch = {new Point(3, 7)};
        updates.add(batch);
        batch[0] = new Point(9, 9);

        assertEquals(Set.of(new Point(3, 7)), updates.drain());
    }

    private static void enqueueTiles(PlayerEntityViewState.PendingDirtyTileUpdates updates,
            CountDownLatch start, int y) {
        try {
            start.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return;
        }
        for (int x = 0; x < 100; x++) {
            updates.add(new Point[]{new Point(x, y), new Point(x, y)});
        }
    }
}
