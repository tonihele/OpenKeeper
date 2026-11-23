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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.logic.IGameLogicUpdatable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The overrun log of the game loop
 */
class GameLoopTest {

    private static final long UPDATE_RATE = TimeUnit.MILLISECONDS.toNanos(10);
    private static final long SLOW_TICK_MILLIS = 80;

    @Test
    void overrunLogShowsHowLongTheTickTook() throws InterruptedException {

        // Fast ticks, then one slow one. The slow one starts on time, so the time since the previous tick is short
        CountDownLatch slowTickDone = new CountDownLatch(1);
        AtomicInteger ticks = new AtomicInteger();
        IGameLogicUpdatable updatable = new IGameLogicUpdatable() {

            @Override
            public void start() {
            }

            @Override
            public void stop() {
            }

            @Override
            public void processTick(float tpf) {
                if (ticks.incrementAndGet() == 5) {
                    try {
                        Thread.sleep(SLOW_TICK_MILLIS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    slowTickDone.countDown();
                }
            }
        };

        List<LogRecord> overruns = new CopyOnWriteArrayList<>();
        Logger logger = Logger.getLogger(GameLoop.class.getName());
        Handler handler = new Handler() {

            @Override
            public void publish(LogRecord logRecord) {
                if (logRecord.getLevel().intValue() >= Level.SEVERE.intValue()) {
                    overruns.add(logRecord);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        logger.addHandler(handler);
        GameLoop gameLoop = new GameLoop(updatable, UPDATE_RATE, "Test");
        try {
            gameLoop.start();
            assertTrue(slowTickDone.await(5, TimeUnit.SECONDS));
            Thread.sleep(50);
        } finally {
            gameLoop.stop();
            logger.removeHandler(handler);
        }

        assertFalse(overruns.isEmpty(), "The slow tick is logged");
        long loggedMillis = ((Number) overruns.get(0).getParameters()[1]).longValue();
        assertTrue(loggedMillis >= SLOW_TICK_MILLIS, "Logged " + loggedMillis + " ms");
    }

    @Test
    void pausedLoopDefersSystemStartupUntilResumed() throws InterruptedException {
        CountDownLatch systemStarted = new CountDownLatch(1);
        IGameLogicUpdatable updatable = new IGameLogicUpdatable() {

            @Override
            public void start() {
                systemStarted.countDown();
            }

            @Override
            public void stop() {
            }

            @Override
            public void processTick(float tpf) {
            }
        };

        GameLoop gameLoop = new GameLoop(updatable, UPDATE_RATE, "PausedStart");
        try {
            gameLoop.pause();
            gameLoop.start();

            assertTrue(awaitRunnerWaiting("Game PausedStart", 5, TimeUnit.SECONDS),
                    "The runner should be waiting on the pause before system startup");
            assertEquals(1L, systemStarted.getCount(), "Systems must not start while the loop is paused");

            gameLoop.resume();
            assertTrue(systemStarted.await(5, TimeUnit.SECONDS),
                    "System startup should proceed after resume");
        } finally {
            gameLoop.stop();
        }
    }

    private static boolean awaitRunnerWaiting(String threadName, long timeout, TimeUnit unit)
            throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (System.nanoTime() < deadline) {
            Thread runner = Thread.getAllStackTraces().keySet().stream()
                    .filter(thread -> threadName.equals(thread.getName()))
                    .findFirst()
                    .orElse(null);
            if (runner != null && runner.getState() == Thread.State.WAITING) {
                return true;
            }
            Thread.sleep(1);
        }
        return false;
    }

}
