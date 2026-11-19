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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.logic.IGameLogicUpdatable;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The overrun log of the game loop
 */
class GameLoopTest {

    private static final long UPDATE_RATE = TimeUnit.MILLISECONDS.toNanos(10);
    private static final long SLOW_TICK_MILLIS = 80;

    @Test void stopBeforeStartReleasesConstructorOwnedSystemsAndRejectsLaterStart() {
        var starts = new AtomicInteger(); var stops = new AtomicInteger();
        var loop = new GameLoop(new IGameLogicUpdatable() {
            @Override public void start() { starts.incrementAndGet(); }
            @Override public void processTick(float tpf) { throw new AssertionError("Stopped loop ticked"); }
            @Override public void stop() { stops.incrementAndGet(); }
        }, UPDATE_RATE);
        loop.stop(); loop.stop();
        assertEquals(0, starts.get()); assertEquals(1, stops.get());
        assertThrows(IllegalThreadStateException.class, loop::start);
    }

    @Test void callbackCanRequestItsOwnStopWithoutJoiningItself() throws Exception {
        var stopped = new CountDownLatch(1); var ticks = new AtomicInteger();
        var owner = new GameLoop[1];
        owner[0] = new GameLoop(new IGameLogicUpdatable() {
            @Override public void start() { }
            @Override public void processTick(float tpf) { ticks.incrementAndGet(); owner[0].stop(); }
            @Override public void stop() { stopped.countDown(); }
        }, UPDATE_RATE);
        owner[0].start();
        try { assertTrue(stopped.await(2, TimeUnit.SECONDS)); }
        finally { owner[0].stop(); }
        assertEquals(1, ticks.get());
    }

    @Test void concurrentStopCallersWaitForTheSameCleanupAndObserveItsFailure() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1); var stops = new AtomicInteger();
        var loop = new GameLoop(new IGameLogicUpdatable() {
            @Override public void start() { }
            @Override public void processTick(float tpf) { }
            @Override public void stop() {
                stops.incrementAndGet(); entered.countDown();
                try { release.await(); } catch (InterruptedException error) { throw new AssertionError(error); }
                throw new IllegalStateException("cleanup failure");
            }
        }, UPDATE_RATE);
        var first = new CompletableFuture<Throwable>(); var second = new CompletableFuture<Throwable>();
        Thread.ofVirtual().start(() -> { try { loop.stop(); first.complete(null); } catch (Throwable error) { first.complete(error); } });
        assertTrue(entered.await(2, TimeUnit.SECONDS));
        Thread.ofVirtual().start(() -> { try { loop.stop(); second.complete(null); } catch (Throwable error) { second.complete(error); } });
        try { assertFalse(first.isDone()); assertFalse(second.isDone()); }
        finally { release.countDown(); }
        assertTrue(first.get(2, TimeUnit.SECONDS) instanceof CompletionException);
        assertTrue(second.get(2, TimeUnit.SECONDS) instanceof CompletionException);
        assertEquals(1, stops.get());
    }

    @Test void startupFailureStillRetiresSystemsExactlyOnce() throws Exception {
        var stops = new AtomicInteger(); var error = new CompletableFuture<Throwable>();
        var loop = new GameLoop(new IGameLogicUpdatable() {
            @Override public void start() { throw new IllegalStateException("startup failed"); }
            @Override public void processTick(float tpf) { throw new AssertionError("Failed startup ticked"); }
            @Override public void stop() { stops.incrementAndGet(); }
        }, UPDATE_RATE);
        var field = GameLoop.class.getDeclaredField("loop"); field.setAccessible(true);
        ((Thread) field.get(loop)).setUncaughtExceptionHandler((thread, failure) -> error.complete(failure));
        loop.start();
        assertTrue(error.get(2, TimeUnit.SECONDS) instanceof IllegalStateException);
        loop.stop(); loop.stop(); assertEquals(1, stops.get());
    }

    @Test void interruptedCallerStillWaitsForActualLoopRetirement() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1); var stops = new AtomicInteger();
        var loop = new GameLoop(new IGameLogicUpdatable() {
            @Override public void start() { }
            @Override public void processTick(float tpf) {
                entered.countDown();
                try { release.await(); } catch (InterruptedException error) { throw new AssertionError(error); }
            }
            @Override public void stop() { stops.incrementAndGet(); }
        }, UPDATE_RATE);
        loop.start(); assertTrue(entered.await(2, TimeUnit.SECONDS));
        var result = new CompletableFuture<Boolean>();
        Thread.ofVirtual().start(() -> {
            try { Thread.currentThread().interrupt(); loop.stop(); result.complete(Thread.currentThread().isInterrupted()); }
            catch (Throwable error) { result.completeExceptionally(error); }
        });
        try { assertFalse(result.isDone()); }
        finally { release.countDown(); }
        assertTrue(result.get(2, TimeUnit.SECONDS)); assertEquals(1, stops.get());
    }

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

}
