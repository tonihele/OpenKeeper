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
package toniarts.openkeeper.utils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.logic.IGameLogicUpdatable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Holds a game loop between frames
 */
class GameLoopTest {

    @Test
    void testHoldWaitsForTheFrameAndHoldsTheNext() throws Exception {
        AtomicBoolean inFrame = new AtomicBoolean();
        AtomicInteger frames = new AtomicInteger();
        CountDownLatch frameStarted = new CountDownLatch(1);
        GameLoop loop = new GameLoop(new IGameLogicUpdatable() {

            @Override
            public void start() {
            }

            @Override
            public void stop() {
            }

            @Override
            public void processTick(float tpf) {
                inFrame.set(true);
                frameStarted.countDown();
                sleep(50);
                frames.incrementAndGet();
                inFrame.set(false);
            }
        }, GameLoop.INTERVAL_FPS_60, "Test");

        loop.start();
        try {
            assertTrue(frameStarted.await(5, TimeUnit.SECONDS));

            // A frame is most likely running now
            int framesWhileHeld = loop.hold(() -> {
                assertFalse(inFrame.get(), "A frame is running");
                int before = frames.get();
                sleep(200);
                return frames.get() - before;
            });

            assertEquals(0, framesWhileHeld);
            assertTrue(frames.get() > 0);
        } finally {
            loop.stop();
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

}
